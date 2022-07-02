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
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

@CommandMethod("ra|RaceAssist setting")
@CommandPermission("raceassist.commands.setting.replacement")
class SettingReplacemcntCommand {

    @CommandMethod("replacement set <raceId> <playerName> <replacement>")
    suspend fun add(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String,
        @Argument(value = "replacement") replacement: String) {
        if (CommandUtils.returnRaceSetting(raceId, sender)) return
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()
        val player = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))

        RaceSettingData.setReplacement(raceId, player.uniqueId, replacement)
        sender.sendMessage(Lang.getComponent("command-result-replacement-set-success", locale, player.name, replacement))

    }

    @CommandMethod("replacement remove <raceId> <playerName>")
    suspend fun remove(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        if (CommandUtils.returnRaceSetting(raceId, sender)) return
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()
        val player = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))

        RaceSettingData.removeReplacement(raceId, player.uniqueId)
        sender.sendMessage(Lang.getComponent("command-result-replacement-remove-success", locale))
    }

    @CommandMethod("replacement delete <raceId>")
    suspend fun delete(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (CommandUtils.returnRaceSetting(raceId, sender)) return
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()

        RaceSettingData.deleteReplacement(raceId)
        sender.sendMessage(Lang.getComponent("command-result-delete-success", locale))

    }

    @CommandMethod("replacement list <raceId>")
    suspend fun list(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (CommandUtils.returnRaceSetting(raceId, sender)) return
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()
        val replacement = RaceSettingData.getReplacement(raceId)
        if (replacement.isEmpty()) {
            sender.sendMessage(Lang.getComponent("command-result-replacement-list-empty", locale))
            return
        }
        replacement.forEach { (t, u) ->
            sender.sendMessage(Lang.getComponent("command-result-replacement-list-message", locale, Bukkit.getOfflinePlayer(t).name, u))
        }

    }

}