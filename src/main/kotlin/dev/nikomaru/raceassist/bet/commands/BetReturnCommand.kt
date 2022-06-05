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

import cloud.commandframework.annotations.*
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.event.BetGuiClickEvent.Companion.getBetOwner
import dev.nikomaru.raceassist.database.*
import dev.nikomaru.raceassist.database.BetList.jockey
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*
import kotlin.math.floor

@CommandMethod("ra|RaceAssist bet")
class BetReturnCommand {
    @CommandPermission("RaceAssist.commands.bet.return")
    @CommandMethod("return <raceId> <playerName>")
    fun returnBet(sender: CommandSender,
        @Argument(value = "raceId", suggestions = "raceId") raceId: String,
        @Argument(value = "playerName", suggestions = "playerName") playerName: String) {
        plugin.launch {
            val player = Bukkit.getOfflinePlayer(playerName)
            val locale = if (sender is Player) sender.locale() else Locale.getDefault()
            if (!player.hasPlayedBefore()) {
                sender.sendMessage(Lang.getComponent("player-not-exist", locale, player.name))
                return@launch
            }
            if (!existPlayer(player)) {
                sender.sendMessage(Lang.getComponent("player-not-jockey", locale, player.name))
                return@launch
            }
            var sum = 0
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { BetList.raceId eq raceId }.forEach {
                    sum += it[BetList.betting]
                }
            }
            val rate: Int = newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.select { BetSetting.raceId eq raceId }.first()[BetSetting.returnPercent]
            }
            var jockeySum = 0
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { (jockey eq player.name.toString()) and (BetList.raceId eq raceId) }.forEach {
                    jockeySum += it[BetList.betting]
                }
            }

            val odds = floor(((sum * (rate.toDouble() / 100)) / jockeySum) * 100) / 100

            if (canReturn[raceId] == true) {
                payRefund(player, raceId, sender, locale)
            } else {
                canReturn[raceId] = true
                newSuspendedTransaction(Dispatchers.IO) {
                    BetList.select { (jockey eq player.name.toString()) and (BetList.raceId eq raceId) }.forEach {
                        val returnAmount = it[BetList.betting] * odds
                        val retunrPlayer = Bukkit.getOfflinePlayer(UUID.fromString(it[BetList.playerUUID]))
                        sender.sendMessage(Lang.getComponent("pay-confirm-bet-player",
                            locale,
                            retunrPlayer.name,
                            player.name,
                            it[BetList.betting],
                            returnAmount))
                    }
                }
                sender.sendMessage(Lang.getComponent("return-confirm-message", locale))
                delay(5000)
                canReturn.remove(raceId)
            }
        }
    }

    private suspend fun existPlayer(player: OfflinePlayer): Boolean {
        return newSuspendedTransaction(Dispatchers.IO) {
            PlayerList.select { PlayerList.playerUUID eq player.uniqueId.toString() }.count() > 0
        }
    }

    companion object {
        val canReturn: HashMap<String, Boolean> = HashMap()

        suspend fun payRefund(player: OfflinePlayer, raceId: String, sender: CommandSender, locale: Locale) {
            var sum = 0
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { BetList.raceId eq raceId }.forEach {
                    sum += it[BetList.betting]
                }
            }

            val rate: Int = newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.select { BetSetting.raceId eq raceId }.first()[BetSetting.returnPercent]
            }
            var jockeySum = 0
            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { (jockey eq player.name.toString()) and (BetList.raceId eq raceId) }.forEach {
                    jockeySum += it[BetList.betting]
                }
            }

            val odds = floor(((sum * (rate.toDouble() / 100)) / jockeySum) * 100) / 100

            newSuspendedTransaction(Dispatchers.IO) {
                BetList.select { (jockey eq player.name.toString()) and (BetList.raceId eq raceId) }.forEach {
                    val returnAmount = it[BetList.betting] * odds
                    val retunrPlayer = Bukkit.getOfflinePlayer(UUID.fromString(it[BetList.playerUUID]))
                    withContext(minecraft) {
                        val eco = VaultAPI.getEconomy()
                        eco.depositPlayer(retunrPlayer, returnAmount)
                        eco.withdrawPlayer(getBetOwner(raceId), returnAmount)
                    }
                    sender.sendMessage(Lang.getComponent("paid-bet-creator", locale, retunrPlayer.name, returnAmount))
                    retunrPlayer.player?.sendMessage(Lang.getComponent("paid-bet-player",
                        locale,
                        raceId,
                        retunrPlayer.name,
                        player.name,
                        returnAmount))
                }
                BetList.deleteWhere { BetList.raceId eq raceId }
            }
            sender.sendMessage(Lang.getComponent("finish-pay", locale))
        }
    }
}