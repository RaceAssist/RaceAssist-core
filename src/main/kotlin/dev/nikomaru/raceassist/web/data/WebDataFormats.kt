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

package dev.nikomaru.raceassist.web.data

import dev.nikomaru.raceassist.data.files.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class WebBetData(val jockey: @Serializable(with = UUIDSerializer::class) UUID, var betPerUnit: Int)

@Serializable
data class WebRaceJockeyDataList(val raceId: String, val betUnit: Int, val dataList: ArrayList<WebRaceJockeyData>)

@Serializable
data class WebRaceJockeyData(
    val jockey: @Serializable(with = UUIDSerializer::class) UUID,
    val horse: @Serializable(with = UUIDSerializer::class) UUID?,
    val odds: Double
)

@Serializable
data class History(val raceId: String, val rank: Int)