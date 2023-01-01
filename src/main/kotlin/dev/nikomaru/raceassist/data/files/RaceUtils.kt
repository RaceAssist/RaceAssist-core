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
import dev.nikomaru.raceassist.data.files.RaceSettingData.existsRace
import dev.nikomaru.raceassist.utils.Utils.toUUID
import dev.nikomaru.raceassist.utils.i18n.Lang
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
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.awt.Polygon
import java.util.*

object RaceUtils {

    suspend fun RaceConfig.save() {
        val raceId = this.raceId
        val file = plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        val json = json.encodeToJsonElement(this)
        val string = json.toString()
        withContext(Dispatchers.IO) {
            file.createNewFile()
            file.writeText(string)
        }
    }

    suspend fun PlaceConfig.save() {
        val placeId = this.placeId
        val file = plugin.dataFolder.resolve("PlaceData").resolve("$placeId.json")
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

    suspend fun getPlaceConfig(placeId: String) = withContext(Dispatchers.IO) {
        val file = plugin.dataFolder.resolve("PlaceData").resolve("$placeId.json")
        return@withContext json.decodeFromString<PlaceConfig>(file.readText())
    }

    suspend fun hasPlaceControlPermission(placeId: String, player: CommandSender) = withContext(Dispatchers.IO) {
        if (player is ConsoleCommandSender) {
            return@withContext true
        }
        (player as Player)
        if (!PlaceSettingData.existsPlace(placeId)) {
            player.sendMessage(Lang.getComponent("no-exist-this-placeid-race", player.locale(), placeId))
            return@withContext false
        }
        if (!PlaceSettingData.existStaff(placeId, player)) {
            player.sendMessage(Lang.getComponent("only-place-creator-can-setting", player.locale()))
            return@withContext false
        }
        return@withContext true
    }

    suspend fun hasRaceControlPermission(raceId: String, player: CommandSender) = withContext(Dispatchers.IO) {
        if (player is ConsoleCommandSender) {
            return@withContext true
        }
        (player as Player)
        if (!existsRace(raceId)) {
            player.sendMessage(Lang.getComponent("no-exist-this-raceid-race", player.locale(), raceId))
            return@withContext false
        }
        if (!RaceSettingData.existStaff(raceId, player)) {
            player.sendMessage(Lang.getComponent("only-race-creator-can-setting", player.locale()))
            return@withContext false
        }
        return@withContext true
    }

}

//TODO 馬に関するデータを保存する
@Serializable
data class RaceConfig(val raceId: String,
    val raceName: String,
    val owner: @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer,
    val staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
    val jockeys: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
    val lap: Int,
    val placeId: String,
    val bet: Bet,
    val replacement: HashMap<@Serializable(with = UUIDSerializer::class) UUID, String>,
    val horse: HashMap<@Serializable(with = UUIDSerializer::class) UUID, @Serializable(with = UUIDSerializer::class) UUID>)

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
data class Bet(val available: Boolean, val returnPercent: Int, val spreadSheetId: String?, val betUnit: Int)

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
