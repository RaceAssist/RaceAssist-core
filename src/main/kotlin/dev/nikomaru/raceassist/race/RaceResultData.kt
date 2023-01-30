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

package dev.nikomaru.raceassist.race

import dev.nikomaru.raceassist.data.files.PolygonSerializer
import dev.nikomaru.raceassist.data.files.UUIDSerializer
import dev.nikomaru.raceassist.horse.data.KZonedDateTimeSerializer
import kotlinx.serialization.Serializable
import java.awt.Polygon
import java.time.ZonedDateTime
import java.util.*

@Serializable
data class RaceResultData(
    val ver: String = "1.0",
    val raceId: String,
    val administrator: String,
    val horse: HashMap<@Serializable(with = UUIDSerializer::class) UUID, @Serializable(with = UUIDSerializer::class) UUID>,
    val start: @Serializable(with = KZonedDateTimeSerializer::class) ZonedDateTime,
    var finish: @Serializable(with = KZonedDateTimeSerializer::class) ZonedDateTime,
    var suspend: Boolean,
    val result: HashMap<Int, @Serializable(with = UUIDSerializer::class) UUID>,
    val lap: Int,
    val distance: Int,
    val uuidToName: HashMap<@Serializable(with = UUIDSerializer::class) UUID, String>,
    val replacement: HashMap<@Serializable(with = UUIDSerializer::class) UUID, String>,
    val rectangleData: RectangleData,
    val insidePolygon: @Serializable(with = PolygonSerializer::class) Polygon,
    val outsidePolygon: @Serializable(with = PolygonSerializer::class) Polygon,
    val currentRaceData: ArrayList<CurrentRaceData>,
    var image: String?
)

@Serializable
data class CurrentRaceData(
    val time: Double,
    val playerRaceData: ArrayList<PlayerRaceData>,
)

@Serializable
data class PlayerRaceData(
    val uuid: @Serializable(with = UUIDSerializer::class) UUID,
    val finish: Boolean,
    val distance: Int?,
    val blockX: Int?,
    val blockY: Int?
)

@Serializable
data class RectangleData(val x1: Int, val y1: Int, val x2: Int, val y2: Int)