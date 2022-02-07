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
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.text.MessageFormat
import java.util.*

@CommandMethod("ra|RaceAssist bet")
class BetRevertCommand {

    @CommandMethod("revert <raceId>")
    fun revert(player: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        val eco: Economy = VaultAPI.getEconomy()
        RaceAssist.plugin.launch {
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
                        player.sendMessage(MessageFormat.format(Lang.getText("bet-revert-return-message-owner", player.locale()),
                            receiver.name,
                            it[BetList.betting]))

                        if (receiver.isOnline) {
                            (receiver as Player).sendMessage(MessageFormat.format(Lang.getText("bet-revert-return-message-player", receiver.locale()),
                                player.name,
                                it[BetList.raceID],
                                it[BetList.jockey],
                                it[BetList.betting]))
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

    private suspend fun getRaceCreator(raceID: String) =
        newSuspendedTransaction(Dispatchers.IO) { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] }

    private suspend fun raceExist(raceID: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceID eq raceID }.count() > 0
        }
        return exist
    }

    private suspend fun getBetSum(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.select { BetList.raceID eq raceID }.sumOf {
            it[BetList.betting]
        }
    }

    companion object {
        val canRevert: HashMap<UUID, Boolean> = HashMap()
    }
}