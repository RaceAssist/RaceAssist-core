/*
 * Copyright © 2022 Nikomaru <nikomaru@nikomaru.dev>
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

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Single
import co.aikar.commands.annotation.Subcommand
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.bet.gui.BetChestGui
import dev.nikomaru.raceassist.bet.gui.BetChestGui.Companion.AllPlayers
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.TempBetData
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandAlias("ra|RaceAssist")
class OpenBetGuiCommand : BaseCommand() {

    @Subcommand("bet open")
    @CommandCompletion("@RaceID")
    fun openVending(player: Player, @Single raceID: String) {
        plugin.launch {
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
                player.openInventory(vending.getGUI(player, raceID))
            }

            newSuspendedTransaction(Dispatchers.IO) {
                AllPlayers[raceID]?.forEach { jockey ->
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