/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.bet.gui.BetChestGui
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.TempBetData
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist bet")
class BetOpenCommand {
    @CommandPermission("RaceAssist.commands.bet.open")
    @CommandMethod("open <raceId>")
    fun openVending(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        plugin.launch {
            if (!raceExist(raceId)) {
                sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
                return@launch
            }
            val vending = BetChestGui()
            val canBet = newSuspendedTransaction(Dispatchers.IO) { BetSetting.select { BetSetting.raceId eq raceId }.first()[BetSetting.canBet] }
            if (!canBet) {
                sender.sendMessage(Lang.getComponent("now-cannot-bet-race", sender.locale()))
                return@launch
            }

            val iterator = TempBetDatas.iterator()
            while (iterator.hasNext()) {
                val it = iterator.next()
                if (it.uuid == sender.uniqueId) {
                    iterator.remove()
                }
            }
            withContext(minecraft) {
                sender.openInventory(vending.getGUI(sender, raceId))
            }


            BetChestGui.AllPlayers[raceId]?.forEach { jockey ->
                TempBetDatas.add(TempBetData(raceId, sender.uniqueId, jockey, 0))
            }

        }

    }

    private suspend fun raceExist(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetSetting.select { BetSetting.raceId eq raceId }.count() > 0
    }

    companion object {
        val TempBetDatas = ArrayList<TempBetData>()
    }
}