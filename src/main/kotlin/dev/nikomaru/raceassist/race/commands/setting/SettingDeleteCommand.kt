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

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.Confirmation
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.SuggestionId
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist setting")
class SettingDeleteCommand {
    @CommandPermission("raceassist.commands.setting.delete")
    @CommandMethod("delete <operateRaceId>")
    @Confirmation
    suspend fun delete(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return
        RaceAssist.api.getDataManager().deleteRace(raceId)

    }
}