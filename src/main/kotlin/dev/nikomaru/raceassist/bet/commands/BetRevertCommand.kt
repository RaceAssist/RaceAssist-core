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
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toUUID
import kotlinx.coroutines.Dispatchers
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist bet revert")
class BetRevertCommand {

    @CommandPermission("raceassist.commands.bet.revert.row")
    @CommandMethod("row <operateRaceId> <uuid>")
    @CommandDescription("そのレースの指定した番号の行のベットを返金します")
    @Confirmation
    suspend fun returnRow(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "uuid", suggestions = "betUniqueId") uuid: String
    ) {
        val row = uuid.toUUID()
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        if (!BetUtils.playerCanPay(raceId, BetUtils.getRowBet(raceId, row), sender)) return
        returnRowBet(row, raceId, sender)
        sender.sendMessage(Lang.getComponent("bet-revert-complete-message", sender.locale()))
    }

    @CommandPermission("raceassist.commands.bet.revert.jockey")
    @CommandMethod("player <operateRaceId> <playerName>")
    @CommandDescription("そのレースに対して特定のプレイヤーのものを返金します")
    @Confirmation
    suspend fun returnPlayer(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "playerName", suggestions = SuggestionId.PLAYER_NAME) playerName: String
    ) {
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        val jockey =
            Bukkit.getOfflinePlayerIfCached(playerName)
                ?: return sender.sendMessage(Lang.getComponent("player-add-not-exist", sender.locale()))
        val raceManager = RaceAssist.api.getRaceManager(raceId)
            ?: return sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
        if (!raceManager.getJockeys().contains(jockey)) {
            sender.sendMessage(Lang.getComponent("player-not-jockey", sender.locale(), jockey.name))
            return
        }
        if (!BetUtils.playerCanPay(raceId, BetUtils.getJockeyBetSum(raceId, jockey), sender)) return
        returnPlayerBet(raceId, jockey, sender)
        sender.sendMessage(Lang.getComponent("bet-revert-complete-message", sender.locale()))
    }

    @CommandPermission("raceassist.commands.bet.revert.all")
    @CommandMethod("all <operateRaceId>")
    @CommandDescription("そのレースに対して賭けられたものをすべて返金します")
    @Confirmation
    suspend fun returnAll(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        if (!BetUtils.playerCanPay(raceId, BetUtils.getBetSum(raceId), sender)) return
        revertAllBet(raceId, sender)
        sender.sendMessage(Lang.getComponent("bet-revert-complete-message", sender.locale()))
    }

    private suspend fun revertAllBet(raceId: String, executor: CommandSender) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)!!
        val betManager = RaceAssist.api.getBetManager(raceId)!!
        val owner = raceManager.getOwner()
        newSuspendedTransaction(Dispatchers.IO) {
            BetList.select { BetList.raceId eq raceId }.forEach {
                val receiver = Bukkit.getOfflinePlayer(it[BetList.playerUniqueId].toUUID())
                betManager.depositToPlayer(receiver, it[BetList.betting].toDouble())
                executor.sendMessage(
                    Lang.getComponent(
                        "bet-revert-return-message-owner",
                        executor.locale(),
                        receiver.name,
                        it[BetList.betting]
                    )
                )
                sendRevertMessage(receiver, owner, it)
            }
            BetList.deleteWhere { BetList.raceId eq raceId }
        }
    }

    private suspend fun returnPlayerBet(raceId: String, jockey: OfflinePlayer, executor: CommandSender) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)!!
        val betManager = RaceAssist.api.getBetManager(raceId)!!
        val owner = raceManager.getOwner()
        newSuspendedTransaction(Dispatchers.IO) {
            BetList.select { (BetList.raceId eq raceId) and (BetList.playerUniqueId eq jockey.uniqueId.toString()) }
                .forEach {
                    val receiver = Bukkit.getOfflinePlayer(it[BetList.playerUniqueId].toUUID())
                    betManager.depositToPlayer(receiver, it[BetList.betting].toDouble())
                    executor.sendMessage(
                        Lang.getComponent(
                            "bet-revert-return-message-owner",
                            executor.locale(),
                            receiver.name,
                            it[BetList.betting]
                        )
                    )
                    sendRevertMessage(receiver, owner, it)
                }
            BetList.deleteWhere { (BetList.raceId eq raceId) and (playerUniqueId eq jockey.uniqueId.toString()) }
        }
    }

    private suspend fun returnRowBet(row: UUID, raceId: String, executor: CommandSender) {
        val raceManager = RaceAssist.api.getRaceManager(raceId)!!
        val betManager = RaceAssist.api.getBetManager(raceId)!!
        val owner = raceManager.getOwner()
        val locale = executor.locale()
        newSuspendedTransaction(Dispatchers.IO) {
            BetList.select { (BetList.rowUniqueId eq row.toString()) and (BetList.raceId eq raceId) }.forEach {
                val receiver = Bukkit.getOfflinePlayer(it[BetList.playerUniqueId].toUUID())
                betManager.depositToPlayer(receiver, it[BetList.betting].toDouble())
                executor.sendMessage(
                    Lang.getComponent(
                        "bet-revert-return-message-owner",
                        locale,
                        receiver.name,
                        it[BetList.betting]
                    )
                )
                sendRevertMessage(receiver, owner, it)
            }
            BetList.deleteWhere { (rowUniqueId eq row.toString()) and (BetList.raceId eq raceId) }
        }
    }


    private fun sendRevertMessage(receiver: OfflinePlayer, owner: OfflinePlayer, it: ResultRow) {
        if (receiver.isOnline) {
            (receiver as Player).sendMessage(
                Lang.getComponent(
                    "bet-revert-return-message-player",
                    receiver.locale(),
                    owner.name,
                    it[BetList.raceId],
                    it[BetList.jockeyUniqueId].toUUID().toOfflinePlayer().name,
                    it[BetList.betting]
                )
            )
        }
    }
}