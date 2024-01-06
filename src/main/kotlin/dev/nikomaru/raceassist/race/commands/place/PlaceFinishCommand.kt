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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import dev.nikomaru.raceassist.race.utils.InsideCircuit
import dev.nikomaru.raceassist.race.utils.OutsideCircuit
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.canSetInsideCircuit
import dev.nikomaru.raceassist.utils.Utils.canSetOutsideCircuit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceFinishCommand {
    @CommandPermission("raceassist.commands.place.finish")
    @CommandMethod("finish")
    suspend fun finish(sender: CommandSender) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }

        if (canSetOutsideCircuit[sender.uniqueId] == true && canSetInsideCircuit[sender.uniqueId] == true) {
            sender.sendMessage(Lang.getComponent("now-you-not-setting-mode", sender.locale()))
            return
        }
        if (canSetInsideCircuit[sender.uniqueId] == true) {
            canSetInsideCircuit.remove(sender.uniqueId)
            InsideCircuit.finish(sender)
            sender.sendMessage(Lang.getComponent("to-finish-inside-course-setting", sender.locale()))
        }
        if (canSetOutsideCircuit[sender.uniqueId] == true) {
            canSetOutsideCircuit.remove(sender.uniqueId)
            OutsideCircuit.finish(sender)
            sender.sendMessage(Lang.getComponent("to-finish-outside-course-setting", sender.locale()))
        }

    }
}