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

package dev.nikomaru.raceassist.race.commands.player

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist player")
class PlayerAddCommand {

    @CommandPermission("raceassist.commands.player.add")
    @CommandMethod("add <operateRaceId> <playerName>")
    suspend fun addPlayer(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "playerName", suggestions = SuggestionId.PLAYER_NAME) playerName: String
    ) {

        val raceManager = RaceAssist.api.getRaceManager(raceId)
        if (raceManager?.senderHasControlPermission(sender) != true) return

        val locale = sender.locale()

        val jockey: OfflinePlayer =
            Bukkit.getOfflinePlayerIfCached(playerName)
                ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))


        if (raceManager.getJockeys().contains(jockey)) {
            sender.sendMessage(Lang.getComponent("already-exist-this-user", locale))
            return
        }
        if (raceManager.getJockeys().size > 7) {
            sender.sendMessage(Lang.getComponent("max-player-is-eight", locale))
            return
        }
        raceManager.addJockey(jockey)
        sender.sendMessage(Lang.getComponent("player-add-to-race-group", locale, jockey.name.toString(), raceId))

    }
}