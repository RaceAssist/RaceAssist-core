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
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.database.BetList
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.text.MessageFormat
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("bet")
class SetBetCommand : BaseCommand() {

    @Subcommand("can")
    @CommandCompletion("@RaceID on|off")
    fun setCanBet(player: Player, @Single raceID: String, @Single type: String) {
        plugin.launch {
            if (!raceExist(raceID)) {
                player.sendMessage(MessageFormat.format(Lang.getText("no-exist-this-raceid-race", player.locale()), raceID))
                return@launch
            }
            if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                return@launch
            }
            if (type == "on") {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.update({ BetSetting.raceID eq raceID }) {
                        it[canBet] = true
                    }
                }
                player.sendMessage(MessageFormat.format(Lang.getText("can-bet-this-raceid", player.locale()), raceID))
            } else if (type == "off") {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetSetting.update({ BetSetting.raceID eq raceID }) {
                        it[canBet] = false
                    }
                }
                player.sendMessage(MessageFormat.format(Lang.getText("cannot-bet-this-raceid", player.locale()), raceID))
            }
        }
    }

    @Subcommand("rate")
    @CommandCompletion("@RaceID")
    fun setRate(player: Player, @Single raceID: String, @Single rate: Int) {
        plugin.launch {
            if (!raceExist(raceID)) {
                player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                return@launch
            }
            if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                return@launch
            }
            if (rate !in 1..100) {
                player.sendMessage(Lang.getText("set-rate-message-in1-100", player.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[returnPercent] = rate
                }
            }
        }
        player.sendMessage(MessageFormat.format(Lang.getText("change-bet-rate-message", player.locale()), raceID, rate))

    }

    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    fun delete(player: Player, @Single raceID: String) {
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                    return@withContext
                }
            }

            if (canDelete[player.uniqueId] == true) {
                newSuspendedTransaction(Dispatchers.IO) {
                    BetList.deleteWhere { BetList.raceID eq raceID }
                }
                player.sendMessage(MessageFormat.format(Lang.getText("bet-remove-race", player.locale()), raceID))
            } else {
                canDelete[player.uniqueId] = true
                player.sendMessage(MessageFormat.format(Lang.getText("bet-remove-race-confirm-message", player.locale()), raceID))
                delay(5000)
                canDelete.remove(player.uniqueId)
            }
        }
    }

    @Subcommand("revert")
    @CommandCompletion("@RaceID")
    fun revert(player: Player, @Single raceID: String) {
        val eco: Economy = VaultAPI.getEconomy()!!
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                    return@withContext
                }
                if (eco.getBalance(player) < getBetSum(raceID)) {
                    player.sendMessage(Lang.getText("no-have-money", player.locale()))
                    return@withContext
                }
            }

            if (canRevert[player.uniqueId] == true) {

                newSuspendedTransaction(Dispatchers.Main) {

                    BetList.select { BetList.raceID eq raceID }.forEach {

                        val receiver = Bukkit.getOfflinePlayer(UUID.fromString(it[BetList.playerUUID].toString()))
                        eco.withdrawPlayer(player, it[BetList.betting].toDouble())

                        eco.depositPlayer(receiver, it[BetList.betting].toDouble())
                        player.sendMessage(
                            MessageFormat.format(
                                Lang.getText("bet-revert-return-message-owner", player.locale()), receiver.name,
                                it[BetList.betting]
                            )
                        )

                        if (receiver.isOnline) {
                            (receiver as Player).sendMessage(
                                MessageFormat.format(
                                    Lang.getText("bet-revert-return-message-player", receiver.locale()),
                                    player.name,
                                    it[BetList.raceID],
                                    it[BetList.jockey],
                                    it[BetList.betting]
                                )
                            )
                        }
                    }
                }
                player.sendMessage(MessageFormat.format(Lang.getText("bet-revert-complete-message", player.locale()), raceID))
            } else {
                canRevert[player.uniqueId] = true
                player.sendMessage(MessageFormat.format(Lang.getText("bet-revert-race-confirm-message", player.locale()), raceID))
                delay(5000)
                canRevert.remove(player.uniqueId)
            }
        }
    }

    private suspend fun getBetSum(raceID: String) =
        newSuspendedTransaction(Dispatchers.IO) {
            BetList.select { BetList.raceID eq raceID }.sumOf {
                it[BetList
                    .betting]
            }
        }

    @Subcommand("sheet")
    @CommandCompletion("@RaceID")
    fun sheet(player: Player, @Single raceID: String, @Single sheetId: String) {
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                    return@withContext
                }
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[spreadsheetId] = sheetId
                }
            }
        }
    }

    @Subcommand("list")
    @CommandCompletion("@RaceID")
    fun list(player: Player, @Single raceID: String) {
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                    return@withContext
                }
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { BetList.raceID eq raceID }.forEach {
                    player.sendMessage(
                        MessageFormat.format(
                            Lang.getText("bet-list-detail-message", player.locale()),
                            it[BetList.rowNum],
                            it[BetList.timeStamp],
                            it[BetList.playerName],
                            it[BetList.jockey],
                            it[BetList.betting]
                        )
                    )
                }
            }
        }
    }

    private suspend fun getRaceCreator(raceID: String) =
        newSuspendedTransaction(Dispatchers.IO) { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] }

    private suspend fun raceExist(raceID: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceID eq raceID }.count() > 0
        }
        return exist
    }

    companion object {
        val canDelete: HashMap<UUID, Boolean> = HashMap()
        val canRevert: HashMap<UUID, Boolean> = HashMap()
    }
}