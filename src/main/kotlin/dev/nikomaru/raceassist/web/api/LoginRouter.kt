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

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.nikomaru.raceassist.data.database.UserAuthData
import dev.nikomaru.raceassist.event.LogDataType
import dev.nikomaru.raceassist.event.web.WebAccountCreateEvent
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toUUID
import dev.nikomaru.raceassist.web.api.WebAPI.privateKey
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

object LoginRouter {
    fun Routing.loginRouter(jwkProvider: JwkProvider) {
        route("/login") {
            post {
                val userData = call.receive<UserData>()

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
                if (Utils.passwordHash(password) != registeredPassword) {
                    //401
                    call.respond(HttpStatusCode.Unauthorized, "Password is incorrect")
                }

                val publicKey = jwkProvider.get(WebAPI.keyId).publicKey
                val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey))
                val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

                val token = JWT.create().withIssuer(WebAPI.issuer)
                    .withClaim("uuid", uuid.toString())
                    .withClaim("username", username)
                    .withExpiresAt(Date(System.currentTimeMillis() + (3 * 24 * 60 * 60)))
                    .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))

                val refreshToken = JWT.create().withIssuer(WebAPI.issuer)
                    .withClaim("refreshToken", true)
                    .withClaim("uuid", uuid.toString())
                    .withClaim("username", username)
                    .withExpiresAt(Date(System.currentTimeMillis() + (30 * 24 * 60 * 60)))
                    .sign(Algorithm.RSA256(publicKey, privateKey))

                WebAccountCreateEvent(LogDataType.WEB, offlinePlayer).callEvent()
                call.respond(hashMapOf("token" to token, "refreshToken" to refreshToken))
            }
        }
    }

    fun Route.jwtLoginRouter(jwkProvider: JwkProvider) {
        route("/refresh") {
            post {
                val principal = call.principal<JWTPrincipal>() ?: return@post call.respondText(
                    "Missing or invalid jwt token",
                    status = HttpStatusCode.Unauthorized
                )

                val uuid = principal.payload.getClaim("uuid").asString().toUUID()
                val username = principal.payload.getClaim("username").asString()
                if (uuid.toOfflinePlayer().name != username) {
                    return@post call.respondText(
                        "Missing or invalid jwt token",
                        status = HttpStatusCode.Unauthorized
                    )
                }

                val publicKey = jwkProvider.get(WebAPI.keyId).publicKey
                val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKey))
                val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

                val token = JWT.create()
                    .withAudience(*(WebAPI.audience.toTypedArray()))
                    .withClaim("uuid", uuid.toString())
                    .withClaim("username", username)
                    .withExpiresAt(Date(System.currentTimeMillis() + (3 * 24 * 60 * 60)))
                    .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))

                return@post call.respond(hashMapOf("token" to token))
            }
        }
    }
}

@Serializable
data class UserData(val username: String, val password: String)