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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.locale
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetPayCommand {

    @CommandPermission("raceassist.commands.bet.return.jockey")
    @CommandMethod("pay <operateRaceId> <playerName>")
    @CommandDescription("払い戻し用のコマンド")
    @Confirmation
    suspend fun returnJockey(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String
    ) {
        val locale = sender.locale()
        val jockey = Bukkit.getOfflinePlayerIfCached(playerName)
            ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", locale))
        val raceManager = RaceAssist.api.getRaceManager(raceId)
            ?: return sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
        if (!raceManager.getJockeys().contains(jockey)) {
            sender.sendMessage(Lang.getComponent("player-not-jockey", locale, jockey.name))
            return
        }
        if (!BetUtils.playerCanPay(raceId, BetUtils.getBetSum(raceId), sender)) return
        BetUtils.payDividend(jockey, raceId, sender, locale)
        sender.sendMessage(Lang.getComponent("finish-pay", locale))
    }
}