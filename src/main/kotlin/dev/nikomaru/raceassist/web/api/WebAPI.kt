/*
 *     Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.web.api

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.data.database.UserAuthData
import dev.nikomaru.raceassist.data.files.*
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.horse.data.HorseData
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getBirthDate
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getBreaderUniqueId
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcJump
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcMaxHealth
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcSpeed
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getDeathDate
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getFatherUniqueId
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getHistories
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getMotherUniqueId
import dev.nikomaru.raceassist.utils.Utils.passwordHash
import dev.nikomaru.raceassist.utils.Utils.toLivingHorse
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import dev.nikomaru.raceassist.utils.Utils.toUUID
import dev.nikomaru.raceassist.web.data.WebRaceJockeyData
import dev.nikomaru.raceassist.web.data.WebRaceJockeyDatas
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.KeyFactory
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.*

object WebAPI {

    private val jwtConfig = Config.config.webAPI?.jwtConfig!!
    val myRealm = jwtConfig.realm
    val issuer = jwtConfig.issuer
    val audience = jwtConfig.audience
    val privateKey = jwtConfig.privateKey
    val keyId = jwtConfig.keyId
    private lateinit var originalServer: NettyApplicationEngine

    fun settingServer() {
        val keyStoreFile = RaceAssist.plugin.dataFolder.resolve("keystore.jks")
        val keystore = KeyStore.getInstance(keyStoreFile, Config.config.webAPI!!.sslSetting.keyStorePassword.toCharArray())

        val environment = applicationEngineEnvironment {
            connector {
                port = Config.config.webAPI!!.port
            }
            sslConnector(keyStore = keystore,
                keyAlias = Config.config.webAPI!!.sslSetting.keyAlias,
                keyStorePassword = { Config.config.webAPI!!.sslSetting.keyStorePassword.toCharArray() },
                privateKeyPassword = { Config.config.webAPI!!.sslSetting.privateKeyPassword.toCharArray() }) {
                port = Config.config.webAPI!!.sslPort
                keyStorePath = keyStoreFile
            }
            module(Application::module)
        }

        originalServer = embeddedServer(Netty, environment)
    }

    fun startServer() {
        originalServer.start(wait = false)
    }

    fun stopServer() {
        originalServer.stop(0, 0, TimeUnit.SECONDS)
    }

}

private fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    val jwkProvider = JwkProviderBuilder(WebAPI.issuer).cached(10, 24, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = WebAPI.myRealm
            verifier(jwkProvider, WebAPI.issuer) {
                acceptLeeway(3)
            }
            validate { credential ->
                if (credential.payload.getClaim("username").asString() == credential.payload.getClaim("uuid").asString().toUUID()
                        .toOfflinePlayer().name) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }

        }

    }

    routing {
        route(("/api/v1")) {
            get("/") {
                call.respondText("Hello World!")
            }

        }
        route("/login") {
            post {
                val userData = call.receive<UserData>()
                val publicKey = jwkProvider.get(WebAPI.keyId).publicKey
                val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(WebAPI.privateKey))
                val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

                val username = userData.username
                val password = userData.password

                val offlinePlayer = Bukkit.getOfflinePlayer(username)
                if (!offlinePlayer.hasPlayedBefore()) {
                    //401
                    call.respond(HttpStatusCode.Unauthorized, "This player has never played before")
                }
                val uuid = offlinePlayer.uniqueId

                //パスワードの検証

                val exist = newSuspendedTransaction(Dispatchers.IO) {
                    UserAuthData.select(UserAuthData.uuid eq uuid.toString()).count() > 0
                }
                if (!exist) {
                    //401
                    call.respond(HttpStatusCode.Unauthorized, "This player is not registered")
                }
                if (offlinePlayer.isBanned) {
                    //403
                    call.respond(HttpStatusCode.Forbidden, "This player is banned")
                }
                val registeredPassword = newSuspendedTransaction {
                    val rr = UserAuthData.select { UserAuthData.uuid eq uuid.toString() }.first()
                    rr[UserAuthData.hashedPassword]
                }
                if (passwordHash(password) != registeredPassword) {
                    //401
                    call.respond(HttpStatusCode.Unauthorized, "Password is incorrect")
                }

                val token = JWT.create().withAudience(*(WebAPI.audience.toTypedArray())).withIssuer(WebAPI.issuer).withClaim("uuid", uuid.toString())
                    .withClaim("username", username).withExpiresAt(Date(System.currentTimeMillis() + 30_000))
                    .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))
                call.respond(hashMapOf("token" to token))
            }
        }
        route("/horse") {
            get("{uuid?}") {
                val uuid = call.parameters["uuid"]?.toUUID() ?: return@get call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                val horse = uuid.toLivingHorse() ?: return@get call.respondText("Horse not found", status = HttpStatusCode.NotFound)

                val horseData = HorseData(
                    horse.uniqueId,
                    horse.getBreaderUniqueId(),
                    horse.ownerUniqueId!!,
                    horse.getMotherUniqueId(),
                    horse.getFatherUniqueId(),
                    horse.getHistories(),
                    horse.color.name,
                    horse.style.name,
                    horse.getCalcSpeed(),
                    horse.getCalcJump(),
                    horse.getCalcMaxHealth(),
                    horse.customName()?.toPlainText(),
                    horse.getBirthDate(),
                    ZonedDateTime.now(),
                    horse.getDeathDate(),
                )

                val horseDataJson = json.encodeToString(horseData)
                call.respondText(horseDataJson, status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
            }
        }
        route("/jockeys") {
            get("{raceId?}") {
                val raceId = call.parameters["raceId"] ?: return@get call.respondText("Missing id", status = HttpStatusCode.BadRequest)
                if (!RaceSettingData.existsRace(raceId)) {
                    call.respondText("Race not found", status = HttpStatusCode.NotFound)
                }
                val jockeys = RaceSettingData.getJockeys(raceId)
                val jockeyDatas = arrayListOf<WebRaceJockeyData>()

                jockeys.forEach {
                    val webRaceJockeyData =
                        WebRaceJockeyData(it.uniqueId, RaceSettingData.getHorse(raceId)[it.uniqueId], BetUtils.getOdds(raceId, it))
                    jockeyDatas.add(webRaceJockeyData)
                }

                val datas = WebRaceJockeyDatas(raceId, BetSettingData.getBetUnit(raceId), jockeyDatas)
                val datasJson = json.encodeToString(datas)

                call.respondText(datasJson, status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
            }
        }
        authenticate("auth-jwt") {
            get("/hello") {
                val principal = call.principal<JWTPrincipal>()
                val uuid = principal!!.payload.getClaim("uuid").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, ${uuid.toUUID().toOfflinePlayer().name}! Token is expired at $expiresAt ms.")
            }
        }
        static(".well-known") {
            staticRootFolder = RaceAssist.plugin.dataFolder
            file("jwks.json")
        }
    }
}

@Serializable
data class UserData(val username: String, val password: String)