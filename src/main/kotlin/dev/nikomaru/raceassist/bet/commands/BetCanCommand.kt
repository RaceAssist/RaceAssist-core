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
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetCanCommand {
    @CommandPermission("raceassist.commands.bet.can")
    @CommandMethod("can <operateRaceId> <type>")
    @CommandDescription("そのレースに対しての賭けることが可能か設定します")
    fun changeBetAvailable(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "type", suggestions = "betType") type: String
    ) {
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        val available = if (type == "on") true else if (type == "off") false else return
        changeBetAvailable(raceId, sender, available)
    }

    private fun changeBetAvailable(raceId: String, sender: CommandSender, available: Boolean) {
        RaceAssist.api.getBetManager(raceId)!!.setAvailable(available)
        if (available) {
            // 賭けを有効化
            sender.sendMessage(Lang.getComponent("can-bet-this-raceid", sender.locale(), raceId))
        } else {
            // 賭けを無効化
            sender.sendMessage(Lang.getComponent("cannot-bet-this-raceid", sender.locale(), raceId))
        }

    }

}