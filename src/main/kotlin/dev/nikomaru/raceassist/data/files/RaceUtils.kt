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
import dev.nikomaru.raceassist.utils.Utils.toUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.awt.Polygon
import java.util.*

object RaceUtils {

    suspend fun RaceConfig.save(raceId: String) {
        val file = plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        val json = json.encodeToJsonElement(this)
        val string = json.toString()
        withContext(Dispatchers.IO) {
            file.createNewFile()
            file.writeText(string)
        }
    }

    suspend fun getRaceConfig(raceId: String) = withContext(Dispatchers.IO) {
        val file = plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        return@withContext json.decodeFromString<RaceConfig>(file.readText())
    }

}

//TODO 馬に関するデータを保存する
@Serializable
data class RaceConfig(var raceId: String,
    var raceName: String,
    var owner: @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer,
    var staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
    var jockeys: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
    val place: Place,
    val bet: Bet,
    val replacement: HashMap<@Serializable(with = UUIDSerializer::class) UUID, String>,
    val horse: HashMap<@Serializable(with = UUIDSerializer::class) UUID, @Serializable(with = UUIDSerializer::class) UUID>)

@Serializable
data class Place(var lap: Int,
    var centralX: Int?,
    var centralY: Int?,
    var goalDegree: Int,
    var reverse: Boolean,
    var inside: @Serializable(with = PolygonSerializer::class) Polygon,
    var outside: @Serializable(with = PolygonSerializer::class) Polygon)

@Serializable
data class Bet(var available: Boolean, var returnPercent: Int, var spreadSheetId: String?, var betUnit: Int)

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
