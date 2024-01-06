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

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.manager.PlaceManager
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.data.plugin.PlainPlaceConfig
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
import org.bukkit.command.CommandSender

object PlaceRouter {
    fun Route.placeRouter() {
        route("/place") {
            get("/list") {
                val list = arrayListOf<String>()
                RaceAssist.plugin.dataFolder.resolve("PlaceData").listFiles()?.forEach {
                    list.add(it.nameWithoutExtension)
                }
                call.respond(hashMapOf("data" to PlaceList(list)))
            }
            get("/config/{placeId}") {
                val placeId = call.parameters["placeId"] ?: return@get call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                RaceAssist.api.getPlaceManager(placeId) ?: return@get call.respondText(
                    "Place not found",
                    status = HttpStatusCode.NotFound
                )

                call.respond(hashMapOf("data" to RaceUtils.getPlainPlaceConfig(placeId)))
            }
        }
    }

    fun Route.jwtPlaceRouter() {
        route("/place") {
            post("/config/{placeId}") {
                val principal = call.principal<JWTPrincipal>() ?: return@post call.respondText(
                    "Missing or invalid jwt token",
                    status = HttpStatusCode.Unauthorized
                )
                val config = call.receive<PlainPlaceConfig>()

                val placeId = call.parameters["placeId"] ?: return@post call.respondText(
                    "Missing id",
                    status = HttpStatusCode.BadRequest
                )
                val placeManager = RaceAssist.api.getPlaceManager(placeId) ?: return@post call.respondText(
                    "Place not found",
                    status = HttpStatusCode.NotFound
                )

                val offlinePlayer = principal.payload.getClaim("uuid").asString().toUUID().toOfflinePlayer()

                if (!placeManager.senderHasControlPermission(offlinePlayer as CommandSender)) {
                    return@post call.respondText(
                        "You do not have permission to edit this place",
                        status = HttpStatusCode.Forbidden
                    )
                }

                PlaceManager.PlainPlaceManager.plainPlaceConfig[placeId] = config

                call.respondText("Success", status = HttpStatusCode.OK)
            }
        }
    }
}

@Serializable
data class PlaceList(val list: ArrayList<String>)

