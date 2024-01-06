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

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.data.utils.UUIDSerializer
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toUUID
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.*

object BetRouter {
    fun Route.betRouter() {
        route("/bet") {
            get("/jockeys/{raceId}") {
                val raceId = call.parameters["raceId"] ?: return@get call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                val raceManager = RaceAssist.api.getRaceManager(raceId) ?: return@get call.respondText(
                    "Race not found",
                    status = HttpStatusCode.NotFound
                )
                val jockeys = raceManager.getJockeys()
                val jockeyData = arrayListOf<WebRaceJockeyData>()

                jockeys.forEach {
                    val webRaceJockeyData =
                        WebRaceJockeyData(
                            it.uniqueId,
                            raceManager.getHorse()[it.uniqueId],
                            BetUtils.getOdds(raceId, it)
                        )
                    jockeyData.add(webRaceJockeyData)
                }

                val betManager = RaceAssist.api.getBetManager(raceId)!!

                val data = WebRaceJockeyDataList(raceId, betManager.getBetUnit(), jockeyData)
                return@get call.respond(hashMapOf("data" to data))
            }
            get("/available/{raceId}") {
                val raceId = call.parameters["raceId"] ?: return@get call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                val betManager = RaceAssist.api.getBetManager(raceId) ?: return@get call.respondText(
                    "Race not found",
                    status = HttpStatusCode.NotFound
                )

                return@get call.respond(hashMapOf("data" to betManager.getAvailable()))
            }
        }
    }

    fun Route.jwtBetRouter() {
        route("/bet") {
            post("/push/{raceId}") {
                val raceId = call.parameters["raceId"] ?: return@post call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                val betManager = RaceAssist.api.getBetManager(raceId) ?: return@post call.respondText(
                    "Race not found",
                    status = HttpStatusCode.NotFound
                )

                val betData = call.receive<BetData>()

                val principal = call.principal<JWTPrincipal>() ?: return@post call.respondText(
                    "Missing or invalid jwt token",
                    status = HttpStatusCode.Unauthorized
                )

                val uuid = principal.payload.getClaim("uuid").asString().toUUID()

                if (!betManager.getAvailable()) return@post call.respond(
                    hashMapOf(
                        "data" to BetPushResponse(
                            BetError.NOT_AVAILABLE,
                            arrayListOf(),
                            0
                        )
                    )
                )

                val sum = betData.data.sumOf { it.multiple } * betManager.getBetUnit()
                val player = uuid.toOfflinePlayer()
                val eco = VaultAPI.getEconomy()

                if (eco.getBalance(player) < sum) return@post call.respond(
                    hashMapOf(
                        "data" to BetPushResponse(
                            BetError.NO_MONEY,
                            arrayListOf(),
                            0
                        )
                    )
                )


                val responseUniqueIdArray = arrayListOf<UUID>()
                var error: BetError? = null
                var paymentSum = 0

                betData.data.forEach { bet ->
                    val (betError, betUniqueId, price) = betManager.pushBet(
                        player,
                        bet.jockeyUniqueId.toOfflinePlayer(),
                        bet.multiple
                    )

                    if (betError != null) {
                        error = betError
                    } else {
                        responseUniqueIdArray.add(betUniqueId!!)
                        paymentSum += price
                    }
                }
                return@post call.respond(hashMapOf("data" to BetPushResponse(error, responseUniqueIdArray, paymentSum)))
            }
        }
    }
}

@Serializable
data class BetDataUnit(
    @Serializable(with = UUIDSerializer::class) val jockeyUniqueId: UUID,
    val multiple: Int,
)

@Serializable
data class BetData(val data: List<BetDataUnit>)

@Serializable
data class BetPushResponse(
    val error: BetError?,
    val betUniqueId: ArrayList<@Serializable(with = UUIDSerializer::class) UUID>,
    val sum: Int,
)

@Serializable
data class WebRaceJockeyDataList(val raceId: String, val betUnit: Int, val dataList: ArrayList<WebRaceJockeyData>)

@Serializable
data class WebRaceJockeyData(
    val jockey: @Serializable(with = UUIDSerializer::class) UUID,
    val horse: @Serializable(with = UUIDSerializer::class) UUID?,
    val odds: Double
)

enum class BetError {
    NO_MONEY, NOT_AVAILABLE,
}