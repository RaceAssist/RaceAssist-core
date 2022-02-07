/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
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

import dev.nikomaru.raceassist.race.commands.CommandUtils.canSetOutsideCircuit
import dev.nikomaru.raceassist.race.commands.CommandUtils.circuitRaceID
import dev.nikomaru.raceassist.race.utils.OutsideCircuit
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import net.kyori.adventure.text.format.TextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.*

class SetOutsideCircuitEvent : Listener {
    @EventHandler
    suspend fun onSetOutsideCircuitEvent(event: PlayerInteractEvent) {
        if (canSetOutsideCircuit[event.player.uniqueId] != true) {
            return
        }
        val player = event.player
        if (event.action == Action.RIGHT_CLICK_AIR || (event.action == Action.RIGHT_CLICK_BLOCK)) {
            player.sendMessage(text(Lang.getText("to-suspend-process", player.locale()), TextColor.color(YELLOW)))
            canSetOutsideCircuit.remove(player.uniqueId)
            return
        }
        if (event.action == Action.LEFT_CLICK_AIR) {
            event.player.sendMessage(text(Lang.getText("to-click-block", player.locale()), TextColor.color(YELLOW)))
            return
        }

        OutsideCircuit.outsideCircuit(player,
            circuitRaceID[player.uniqueId]!!,
            Objects.requireNonNull(event.clickedBlock)!!.x,
            event.clickedBlock!!.z)
    }
}