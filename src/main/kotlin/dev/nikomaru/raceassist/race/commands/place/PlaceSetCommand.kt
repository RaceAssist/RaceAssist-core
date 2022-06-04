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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.*
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.utils.CommandUtils.canSetInsideCircuit
import dev.nikomaru.raceassist.utils.CommandUtils.canSetOutsideCircuit
import dev.nikomaru.raceassist.utils.CommandUtils.circuitRaceId
import dev.nikomaru.raceassist.utils.CommandUtils.getInsideRaceExist
import dev.nikomaru.raceassist.utils.CommandUtils.getRaceExist
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceSetCommand {
    @CommandPermission("RaceAssist.commands.place.set")
    @CommandMethod("set <raceId> <type>")
    fun set(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "type", suggestions = "placeType") type: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        plugin.launch {

            if (!getRaceExist(raceId)) {
                sender.sendMessage(Lang.getComponent("no-exist-race", sender.locale()))
                return@launch
            } else if (returnRaceSetting(raceId, sender)) return@launch

            if (canSetOutsideCircuit[sender.uniqueId] != null || canSetInsideCircuit[sender.uniqueId] != null) {
                sender.sendMessage(Lang.getComponent("already-setting-mode", sender.locale()))
                return@launch
            }
            if (type == "in") {
                canSetInsideCircuit[sender.uniqueId] = true
                sender.sendMessage(Lang.getComponent("to-be-inside-set-mode", sender.locale()))
            } else if (type == "out") {
                if (!getInsideRaceExist(raceId)) {
                    sender.sendMessage(Lang.getComponent("no-inside-course-set", sender.locale()))
                    return@launch
                }
                canSetOutsideCircuit[sender.uniqueId] = true
                sender.sendMessage(Lang.getComponent("to-be-outside-set-mode", sender.locale()))
            }
            circuitRaceId[sender.uniqueId] = raceId
            sender.sendMessage(Lang.getComponent("to-click-left-start-right-finish", sender.locale()))
            sender.sendMessage(Lang.getComponent("to-enter-finish-message", sender.locale()))
        }
    }
}