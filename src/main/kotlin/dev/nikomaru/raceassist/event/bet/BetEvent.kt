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

package dev.nikomaru.raceassist.event.bet

import dev.nikomaru.raceassist.event.LogDataType
import org.bukkit.OfflinePlayer
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class BetEvent(
    val type: LogDataType,
    val raceId: String,
    val executor: OfflinePlayer,
    val jockey: OfflinePlayer,
    val betAmount: Int
) :
    Event(true) {
    override fun getHandlers(): HandlerList {
        return HANDLER_LIST
    }

    companion object {
        private val HANDLER_LIST = HandlerList()
    }
}