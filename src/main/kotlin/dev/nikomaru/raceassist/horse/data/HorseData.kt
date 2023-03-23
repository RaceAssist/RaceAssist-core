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

package dev.nikomaru.raceassist.horse.data

import dev.nikomaru.raceassist.data.utils.UUIDSerializer
import dev.nikomaru.raceassist.web.data.History
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.util.*

@Serializable
data class HorseData(
    val horse: @Serializable(with = UUIDSerializer::class) UUID,
    val breeder: @Serializable(with = UUIDSerializer::class) UUID?,
    val owner: @Serializable(with = UUIDSerializer::class) UUID?,
    val mother: @Serializable(with = UUIDSerializer::class) UUID?,
    val father: @Serializable(with = UUIDSerializer::class) UUID?,
    val history: ArrayList<History>,
    val color: String,
    val style: String,
    val speed: Double,
    val jump: Double,
    val health: Double,
    val name: String?,
    val birthDate: @Serializable(with = KZonedDateTimeSerializer::class) ZonedDateTime?,
    val lastRecordDate: @Serializable(with = KZonedDateTimeSerializer::class) ZonedDateTime,
    val deathDate: @Serializable(with = KZonedDateTimeSerializer::class) ZonedDateTime?
)

object KZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        val string = decoder.decodeString()
        return ZonedDateTime.parse(string)
    }
}