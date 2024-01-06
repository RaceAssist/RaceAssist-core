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

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.Regex
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist setting")
class SettingCopyCommand {

    @CommandPermission("raceassist.commands.setting.copy")
    @CommandMethod("copy <raceId1> <raceId2>")
    suspend fun copy(
        sender: CommandSender,
        @Regex(value = "[a-zA-Z]+-\\d+$") @Argument(
            value = "raceId1",
            suggestions = SuggestionId.RACE_ID
        ) raceId1: String,
        @Regex(value = "[a-zA-Z]+-\\d+$") @Argument(value = "raceId2") raceId2: String
    ) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val locale = sender.locale()

        if (RaceUtils.existsRace(raceId2)) {
            sender.sendMessage(Lang.getComponent("already-used-the-name-race", locale))
            return
        }
        RaceAssist.api.getRaceManager(raceId1)?.copyRace(raceId2, sender)
    }
}
