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
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.toUUID
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.awt.Polygon
import java.io.*
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


@Serializable
data class RaceConfig(
    val raceId: String,
    val raceName: String,
    val placeId: String,
    val bet: Bet,
    val owner: @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer,
    val staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
    val jockeys: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer> = arrayListOf(),
    val lap: Int = 0,
    val replacement: HashMap<@Serializable(with = UUIDSerializer::class) UUID, String> = hashMapOf(),
    val horse: HashMap<@Serializable(with = UUIDSerializer::class) UUID, @Serializable(with = UUIDSerializer::class) UUID> = hashMapOf(),
)


@Serializable
data class PlaceConfig(
    val placeId: String,
    val centralX: Int?,
    val centralY: Int?,
    val goalDegree: Int,
    val reverse: Boolean,
    val inside: @Serializable(with = PolygonSerializer::class) Polygon,
    val outside: @Serializable(with = PolygonSerializer::class) Polygon,
    val owner: @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer,
    val staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
)

@Serializable
data class Bet(
    val available: Boolean = false,
    val returnPercent: Int = 75,
    val spreadSheetId: String? = null,
    val betUnit: Int = 100,
    val autoReturn: Boolean = false,
)

// UUID <==> String
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): UUID {
        return decoder.decodeString().toUUID()
    }

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
}

// OfflinePlayer <==> UUID
object OfflinePlayerSerializer : KSerializer<OfflinePlayer> {
    override val descriptor = PrimitiveSerialDescriptor("OfflinePlayer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OfflinePlayer {
        return Bukkit.getOfflinePlayer(decoder.decodeString().toUUID())
    }

    override fun serialize(encoder: Encoder, value: OfflinePlayer) {
        encoder.encodeString(value.uniqueId.toString())
    }
}

// Polygon <==> List<List<Int>,List<Int>>
object PolygonSerializer : KSerializer<Polygon> {
    override val descriptor = PrimitiveSerialDescriptor("Polygon", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Polygon) {
        require(encoder is JsonEncoder)
        val points: ArrayList<Pair<Int, Int>> = ArrayList()
        for (i in 0 until value.npoints) {
            points.add(Pair(value.xpoints[i], value.ypoints[i]))
        }
        val encode = PolygonData(points)
        encoder.encodeJsonElement(json.encodeToJsonElement(encode))
    }

    override fun deserialize(decoder: Decoder): Polygon {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        val polygonData = json.decodeFromJsonElement<PolygonData>(element)

        val polygon = Polygon()

        polygonData.points.forEach {
            polygon.addPoint(it.first, it.second)
        }

        return polygon
    }
}

@Serializable
data class PolygonData(val points: ArrayList<Pair<Int, Int>>)

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    isLenient = true
    prettyPrint = true
    explicitNulls = false
}
