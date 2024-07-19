/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.web.api

import com.auth0.jwk.JwkProviderBuilder
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.utils.json
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
import dev.nikomaru.raceassist.utils.Utils.toLivingHorse
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import dev.nikomaru.raceassist.utils.Utils.toUUID
import dev.nikomaru.raceassist.web.api.BetRouter.betRouter
import dev.nikomaru.raceassist.web.api.BetRouter.jwtBetRouter
import dev.nikomaru.raceassist.web.api.LoginRouter.jwtLoginRouter
import dev.nikomaru.raceassist.web.api.LoginRouter.loginRouter
import dev.nikomaru.raceassist.web.api.PlaceRouter.jwtPlaceRouter
import dev.nikomaru.raceassist.web.api.PlaceRouter.placeRouter
import dev.nikomaru.raceassist.web.api.RaceRouter.jwtRaceRouter
import dev.nikomaru.raceassist.web.api.RaceRouter.raceRouter
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.java.KoinJavaComponent.inject
import java.security.KeyStore
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WebAPI : KoinComponent {
    val plugin: RaceAssist by inject()

    private val jwtConfig = Config.config.webAPI?.jwtConfig!!
    val myRealm = jwtConfig.realm
    val issuer = jwtConfig.issuer
    val audience = jwtConfig.audience
    val privateKey = jwtConfig.privateKey
    val keyId = jwtConfig.keyId
    private lateinit var originalServer: NettyApplicationEngine

    fun settingServer() {
        val keyStoreFile = plugin.dataFolder.resolve("keystore.jks")


        val environment = applicationEngineEnvironment {
            connector {
                port = Config.config.webAPI!!.port
            }
            val sslSetting = Config.config.webAPI!!.sslSetting
            if (sslSetting != null) {
                val keystore =
                    KeyStore.getInstance(keyStoreFile, sslSetting.keyStorePassword.toCharArray())
                sslConnector(keyStore = keystore,
                    keyAlias = sslSetting.keyAlias,
                    keyStorePassword = { sslSetting.keyStorePassword.toCharArray() },
                    privateKeyPassword = { sslSetting.privateKeyPassword.toCharArray() }) {
                    port = sslSetting.sslPort
                    keyStorePath = keyStoreFile
                }
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
    //inject the plugin instance
    val plugin: RaceAssist by inject(RaceAssist::class.java)

    install(ContentNegotiation) {
        json()
    }
    val jwkProvider =
        JwkProviderBuilder(WebAPI.issuer).cached(10, 24, TimeUnit.HOURS).rateLimited(10, 1, TimeUnit.MINUTES).build()
    install(Authentication) {
        jwt("auth-jwt") {
            realm = WebAPI.myRealm
            verifier(jwkProvider, WebAPI.issuer) {
                acceptLeeway(3)
            }
            validate { credential ->
                if (credential.payload.getClaim("username").asString() == credential.payload.getClaim("uuid").asString()
                        .toUUID()
                        .toOfflinePlayer().name
                ) {
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
        route("/") {
            get {
                call.respondText("Hello World!")
            }
        }
        loginRouter(jwkProvider)
        horseRouter()
        betRouter()
        raceRouter()
        placeRouter()
        betRouter()
        authenticate("auth-jwt") {
            get("/hello") {
                val principal = call.principal<JWTPrincipal>()
                val uuid = principal!!.payload.getClaim("uuid").asString()
                val expiresAt = principal.expiresAt?.time?.minus(System.currentTimeMillis())
                call.respondText("Hello, ${uuid.toUUID().toOfflinePlayer().name}! Token is expired at $expiresAt ms.")
            }
            jwtBetRouter()
            jwtRaceRouter()
            jwtPlaceRouter()
            jwtLoginRouter(jwkProvider)
        }
        staticFiles(".well-known", plugin.dataFolder, "jwks.json")
    }
}


private fun Routing.horseRouter() {
    route("/horse") {
        get("/{uuid}") {
            val uuid = call.parameters["uuid"]?.toUUID() ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val horse = uuid.toLivingHorse() ?: return@get call.respondText(
                "Horse not found",
                status = HttpStatusCode.NotFound
            )

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
            call.respondText(
                horseDataJson,
                status = HttpStatusCode.OK,
                contentType = ContentType.Application.Json
            )
        }
    }
}






