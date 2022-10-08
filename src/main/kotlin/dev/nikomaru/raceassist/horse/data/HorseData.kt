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

import dev.nikomaru.raceassist.data.files.UUIDSerializer
import dev.nikomaru.raceassist.web.data.History
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class HorseData(val horse: @Serializable(with = UUIDSerializer::class) UUID,
    val breader: @Serializable(with = UUIDSerializer::class) UUID?,
    val mother: @Serializable(with = UUIDSerializer::class) UUID?,
    val father: @Serializable(with = UUIDSerializer::class) UUID?,
    val history: ArrayList<History>)