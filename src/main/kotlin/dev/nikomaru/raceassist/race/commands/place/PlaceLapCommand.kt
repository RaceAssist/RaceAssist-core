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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.*
import cloud.commandframework.annotations.specifier.Range
import dev.nikomaru.raceassist.data.files.PlaceSettingData
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceLapCommand {
    @CommandPermission("RaceAssist.commands.place.lap")
    @CommandMethod("lap <raceId> <lap>")
    suspend fun setLap(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "lap") @Range(min = "1", max = "100") lap: Int) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }

        if (CommandUtils.returnRaceSetting(raceId, sender)) return

        if (lap < 1) {
            sender.sendMessage(Lang.getComponent("to-need-enter-over-1", sender.locale()))
            return
        }
        PlaceSettingData.setLap(raceId, lap)
        sender.sendMessage(Lang.getComponent("to-set-lap", sender.locale()))

    }
}