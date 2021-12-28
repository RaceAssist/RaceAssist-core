/*
 * Copyright © 2021 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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

package dev.nikomaru.raceassist.race.event

import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class SetCentralPointEvent : Listener {
    @EventHandler
    fun setCentralPoint(event: PlayerInteractEvent) {
        if (PlaceCommands.getCanSetCentral()[event.player.uniqueId] == null || PlaceCommands.getCanSetCentral()[event.player.uniqueId] != true) {
            return
        }
        if (event.action != Action.LEFT_CLICK_BLOCK) {
            return
        }
        transaction {
            RaceList.update({ RaceList.raceID eq (PlaceCommands.getCentralRaceID()[event.player.uniqueId]!!) }) {
                it[centralXPoint] = event.clickedBlock?.location?.blockX ?: 0
                it[centralYPoint] = event.clickedBlock?.location?.blockZ ?: 0
            }
        }
        event.player.sendMessage("§a中心を設定しました")
        PlaceCommands.removeCanSetCentral(event.player.uniqueId)
    }
}