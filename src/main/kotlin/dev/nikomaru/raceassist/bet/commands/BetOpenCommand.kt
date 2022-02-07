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
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.gui.BetChestGui
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.TempBetData
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist bet")
class BetOpenCommand {

    @CommandMethod("open <raceId>")
    fun openVending(player: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (!raceExist(raceID)) {
                player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                return@launch
            }
            val vending = BetChestGui()
            val canBet = newSuspendedTransaction(Dispatchers.IO) { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.canBet] }
            if (!canBet) {
                player.sendMessage(Lang.getText("now-cannot-bet-race", player.locale()))
                return@launch
            }

            newSuspendedTransaction(Dispatchers.IO) {
                TempBetData.deleteWhere { (TempBetData.playerUUID eq player.uniqueId.toString()) and (TempBetData.raceID eq raceID) }
                withContext(Dispatchers.minecraft) {
                    player.openInventory(vending.getGUI(player, raceID))
                }
            }

            newSuspendedTransaction(Dispatchers.IO) {
                BetChestGui.AllPlayers[raceID]?.forEach { jockey ->
                    TempBetData.insert {
                        it[TempBetData.raceID] = raceID
                        it[playerUUID] = player.uniqueId.toString()
                        it[TempBetData.jockey] = jockey.toString()
                        it[bet] = 0
                    }
                }
            }

        }
    }

    private suspend fun raceExist(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetSetting.select { BetSetting.raceID eq raceID }.count() > 0
    }
}