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
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.i18n.Lang
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist setting")
@CommandPermission("raceassist.commands.setting.replacement")
class SettingReplacemcntCommand {

    @CommandMethod("replacement set <operateRaceId> <playerName> <replacement>")
    suspend fun add(sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String,
        @Argument(value = "replacement") replacement: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        val locale = sender.locale()
        val player = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))

        RaceSettingData.setReplacement(raceId, player.uniqueId, replacement)
        sender.sendMessage(Lang.getComponent("command-result-replacement-set-success", locale, player.name, replacement))

    }

    @CommandMethod("replacement remove <operateRaceId> <playerName>")
    suspend fun remove(sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        val locale = sender.locale()
        val player = Bukkit.getOfflinePlayerIfCached(playerName) ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))

        RaceSettingData.removeReplacement(raceId, player.uniqueId)
        sender.sendMessage(Lang.getComponent("command-result-replacement-remove-success", locale))
    }

    @CommandMethod("replacement delete <operateRaceId>")
    suspend fun delete(sender: CommandSender, @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        val locale = sender.locale()

        RaceSettingData.deleteReplacement(raceId)
        sender.sendMessage(Lang.getComponent("command-result-delete-success", locale))

    }

    @CommandMethod("replacement list <operateRaceId>")
    suspend fun list(sender: CommandSender, @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        val locale = sender.locale()
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