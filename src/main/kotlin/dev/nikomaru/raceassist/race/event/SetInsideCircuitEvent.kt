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
package dev.nikomaru.raceassist.race.event

import dev.nikomaru.raceassist.race.utils.InsideCircuit
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.canSetInsideCircuit
import dev.nikomaru.raceassist.utils.Utils.circuitPlaceId
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class SetInsideCircuitEvent : Listener {
    @EventHandler
    fun onSetInsideCircuitEvent(event: PlayerInteractEvent) {
        if (canSetInsideCircuit[event.player.uniqueId] != true) {
            return
        }
        val player = event.player
        if (event.action == Action.LEFT_CLICK_AIR) {
            event.player.sendMessage(Lang.getComponent("to-click-block", player.locale()))
            return
        }
        InsideCircuit.insideCircuit(
            player,
            circuitPlaceId[player.uniqueId]!!,
            event.clickedBlock!!.x,
            event.clickedBlock!!.z
        )
    }
}



