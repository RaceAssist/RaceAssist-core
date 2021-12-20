/*
 * Copyright © 2021 Nikomaru
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

import dev.nikomaru.raceassist.database.Database
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class SetCentralPointEvent : Listener {
    @EventHandler
    fun setCentralPoint(event: PlayerInteractEvent) {
        if (PlaceCommands.getCanSetCentral()[event.player.uniqueId] == null || PlaceCommands.getCanSetCentral()[event.player.uniqueId] != true) {
            return
        }
        if (event.action != Action.LEFT_CLICK_BLOCK) {
            return
        }
        try {
            val connection = Database.connection ?: return
            val statement = connection.prepareStatement("UPDATE RaceList SET CentralXPoint= ? , CentralYPoint = ? WHERE RaceID = ?")
            statement.setInt(1, event.clickedBlock?.location?.blockX ?: 0)
            statement.setInt(2, event.clickedBlock?.location?.blockZ ?: 0)
            statement.setString(3, PlaceCommands.getCentralRaceID()[event.player.uniqueId])
            statement.execute()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        event.player.sendMessage("§a中心を設定しました")
        PlaceCommands.removeCanSetCentral(event.player.uniqueId)
    }
}