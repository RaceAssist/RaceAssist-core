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

package dev.nikomaru.raceassist.data.files

import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.data.plugin.PlaceConfig
import dev.nikomaru.raceassist.data.plugin.RaceConfig
import dev.nikomaru.raceassist.data.utils.json
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.Utils
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.encodeToJsonElement
import java.util.*

object RaceUtils {


    fun existsRace(raceId: String): Boolean {
        val file = plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        return file.exists()
    }

    fun existsPlace(placeId: String): Boolean {
        val file = plugin.dataFolder.resolve("PlaceData").resolve("$placeId.json")
        return file.exists()
    }

    suspend fun RaceConfig.save() {
        val data = this
        val raceId = data.raceId
        val file = plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        val json = json.encodeToJsonElement(this)
        val string = json.toString()
        withContext(Dispatchers.IO) {
            file.createNewFile()
            file.writeText(string)
        }
        Config.config.webAPI?.recordUrl?.forEach {
            var editUrl = it.url
            if (editUrl.last() != '/') {
                editUrl += "/"
            }
            editUrl += "race/$raceId"

            Utils.client.post(editUrl) {
                contentType(ContentType.Application.Json)
                setBody(data)
                headers {
                    val token = Base64.getEncoder().encodeToString("${it.name}:${it.password}".toByteArray())
                    append("Authorization", "Basic $token")
                }
            }
        }
    }

    suspend fun PlaceConfig.save() {
        val data = this
        val placeId = data.placeId
        val file = plugin.dataFolder.resolve("PlaceData").resolve("$placeId.json")
        val json = json.encodeToJsonElement(this)
        val string = json.toString()
        withContext(Dispatchers.IO) {
            file.createNewFile()
            file.writeText(string)
        }
        Config.config.webAPI?.recordUrl?.forEach {
            var editUrl = it.url
            if (editUrl.last() != '/') {
                editUrl += "/"
            }
            editUrl += "place/$placeId"

            Utils.client.post(editUrl) {
                contentType(ContentType.Application.Json)
                setBody(data)
                headers {
                    val token = Base64.getEncoder().encodeToString("${it.name}:${it.password}".toByteArray())
                    append("Authorization", "Basic $token")
                }
            }
        }
    }

    suspend fun getRaceConfig(raceId: String) = withContext(Dispatchers.IO) {
        val file = plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        return@withContext json.decodeFromString<RaceConfig>(file.readText())
    }

    suspend fun getPlaceConfig(placeId: String) = withContext(Dispatchers.IO) {
        val file = plugin.dataFolder.resolve("PlaceData").resolve("$placeId.json")
        return@withContext json.decodeFromString<PlaceConfig>(file.readText())
    }


}




