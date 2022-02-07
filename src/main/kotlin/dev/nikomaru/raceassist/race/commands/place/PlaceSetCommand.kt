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

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.race.commands.CommandUtils.canSetInsideCircuit
import dev.nikomaru.raceassist.race.commands.CommandUtils.canSetOutsideCircuit
import dev.nikomaru.raceassist.race.commands.CommandUtils.circuitRaceID
import dev.nikomaru.raceassist.race.commands.CommandUtils.getInsideRaceExist
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.utils.Lang
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceSetCommand {
    @CommandPermission("RaceAssist.commands.place.set")
    @CommandMethod("set <raceId> <type>")
    fun set(player: Player,
        @Argument(value = "raceId", suggestions = "raceId") raceID: String,
        @Argument(value = "type", suggestions = "placeType") type: String) {
        RaceAssist.plugin.launch {

            if (getRaceCreator(raceID) == null) {
                player.sendMessage(Component.text(Lang.getText("no-exist-race", player.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            } else if (getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(Component.text(Lang.getText("only-race-creator-can-setting", player.locale()),
                    TextColor.color(NamedTextColor.RED)))
                return@launch
            }

            if (canSetOutsideCircuit[player.uniqueId] != null || canSetInsideCircuit[player.uniqueId] != null) {
                player.sendMessage(Lang.getText("already-setting-mode", player.locale()))
                return@launch
            }
            if (type == "in") {
                canSetInsideCircuit[player.uniqueId] = true
                player.sendMessage(Component.text(Lang.getText("to-be-inside-set-mode", player.locale()), TextColor.color(NamedTextColor.GREEN)))
            } else if (type == "out") {
                if (!getInsideRaceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-inside-course-set", player.locale()))
                    return@launch
                }
                canSetOutsideCircuit[player.uniqueId] = true
                player.sendMessage(Component.text(Lang.getText("to-be-outside-set-mode", player.locale()), TextColor.color(NamedTextColor.GREEN)))
            }
            circuitRaceID[player.uniqueId] = raceID
            player.sendMessage(Component.text(Lang.getText("to-click-left-start-right-finish", player.locale()),
                TextColor.color(NamedTextColor.GREEN)))
            player.sendMessage(Lang.getText("to-enter-finish-message", player.locale()))
        }
    }
}