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

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.RaceData
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist setting")
class SettingCopyCommand {

    @CommandPermission("RaceAssist.commands.setting.copy")
    @CommandMethod("copy <raceId_1> <raceId_2>")
    suspend fun copy(sender: CommandSender,
        @Argument(value = "raceId_1", suggestions = "raceId") raceId_1: String,
        @Argument(value = "raceId_2") raceId_2: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val locale = sender.locale()
        if (RaceData.existsRace(raceId_2)) {
            sender.sendMessage(Lang.getComponent("already-used-the-name-race", locale))
            return
        }
        RaceData.copyRace(raceId_1, raceId_2, sender)

    }
}
