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
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandMethod("ra|raceassist setting")
@CommandPermission("raceassist.commands.setting.staff")
class SettingStaffCommand {

    @CommandMethod("staff add <operateRaceId> <playerName>")
    suspend fun addStaff(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "playerName", suggestions = SuggestionId.PLAYER_NAME) playerName: String
    ) {

        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return

        val locale = sender.locale()
        val target = Bukkit.getOfflinePlayerIfCached(playerName)
            ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))

        if (raceManager.addStaff(target)) {
            sender.sendMessage(Lang.getComponent("add-staff", locale))
        } else {
            sender.sendMessage(Lang.getComponent("already-added-staff", locale))
        }

    }

    @CommandMethod("staff remove <operateRaceId> <playerName>")
    suspend fun removeStaff(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "playerName", suggestions = SuggestionId.PLAYER_NAME) playerName: String
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return

        val locale = sender.locale()
        val target = Bukkit.getOfflinePlayerIfCached(playerName)
            ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
        if (raceManager.getOwner() == target) {
            return sender.sendMessage(Lang.getComponent("cant-remove-yourself-staff", locale))
        }

        if (raceManager.removeStaff(target)) {
            sender.sendMessage(Lang.getComponent("delete-staff", locale))
        } else {
            sender.sendMessage(Lang.getComponent("not-find-staff", locale))
        }

    }

    @CommandMethod("staff list <operateRaceId>")
    suspend fun listStaff(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return
        raceManager.getStaffs().forEach {
            sender.sendMessage(it.name.toString())
        }

    }
}