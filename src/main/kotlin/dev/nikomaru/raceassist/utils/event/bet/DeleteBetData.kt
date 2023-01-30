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

package dev.nikomaru.raceassist.utils.event.bet

import dev.nikomaru.raceassist.data.database.BetListData
import dev.nikomaru.raceassist.utils.event.EventData
import dev.nikomaru.raceassist.utils.event.LogDataType
import java.time.ZonedDateTime
import java.util.*

data class DeleteBetData(
    override val type: LogDataType,
    override val executor: UUID?,
    override val date: ZonedDateTime = ZonedDateTime.now(),
    val raceId: String,
    val deleteBetDataList: ArrayList<BetListData>
) : EventData