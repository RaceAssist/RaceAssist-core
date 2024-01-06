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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetListCommand {
    @CommandPermission("raceassist.commands.bet.list")
    @CommandMethod("list <operateRaceId>")
    @CommandDescription("現在賭けられている一覧を表示します")
    suspend fun list(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        val locale = sender.locale()
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        val list = BetUtils.listBetData(raceId)
        if (list.isEmpty()) {
            sender.sendMessage(Lang.getComponent("no-one-betting", locale))
            return
        }
        list.forEach {
            val jockeyName = Bukkit.getOfflinePlayer(it.jockeyUniqueId).name
            val betPlayerName = Bukkit.getOfflinePlayer(it.playerUniqueId).name
            sender.sendMessage(
                Lang.getComponent(
                    "bet-list-detail-message",
                    locale,
                    it.rowUniqueId,
                    it.timeStamp,
                    betPlayerName,
                    jockeyName,
                    it.betting
                )
            )
        }
    }
}