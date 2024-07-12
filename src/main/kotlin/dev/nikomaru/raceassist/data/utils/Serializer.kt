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

package dev.nikomaru.raceassist.data.utils

import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.plugin.PlaceConfig
import dev.nikomaru.raceassist.data.plugin.PolygonData
import dev.nikomaru.raceassist.utils.Utils.toUUID
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.awt.Polygon
import java.util.*

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

object PlaceConfigSerializer : JsonContentPolymorphicSerializer<PlaceConfig>(PlaceConfig::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PlaceConfig> {
        return when (element.jsonObject["placeType"]?.jsonPrimitive?.content) {
            PlaceType.PLAIN.name -> PlaceConfig.PlainPlaceConfig.serializer()
            PlaceType.PLANE_VECTOR.name -> PlaceConfig.PlaneVectorPlaceConfig.serializer()
            else -> throw SerializationException("Unknown PlaceType")
        }
    }
}


val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    prettyPrint = true
}