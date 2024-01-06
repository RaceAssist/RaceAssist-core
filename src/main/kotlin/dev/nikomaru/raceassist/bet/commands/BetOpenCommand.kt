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
import dev.nikomaru.raceassist.bet.gui.BetChestGui
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist bet")
class BetOpenCommand {
    @CommandPermission("raceassist.commands.bet.open")
    @CommandMethod("open <raceId>")
    @CommandDescription("賭けるためのGUIを表示します")
    suspend fun openVending(
        sender: CommandSender,
        @Argument(value = "raceId", suggestions = SuggestionId.RACE_ID) raceId: String
    ) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val betManager = RaceAssist.api.getBetManager(raceId)
            ?: return sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
        if (!betManager.getAvailable()) {
            sender.sendMessage(Lang.getComponent("now-cannot-bet-race", sender.locale()))
            return
        }
        BetUtils.removePlayerTempBetData(sender)
        withContext(Dispatchers.minecraft) {
            sender.openInventory(BetChestGui().getGUI(sender, raceId))
        }
        BetUtils.initializePlayerTempBetData(raceId, sender)
    }

}