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

package dev.nikomaru.raceassist.bet

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.data.TempBetData
import dev.nikomaru.raceassist.bet.gui.BetChestGui
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.data.database.BetListData
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.Utils.toUUID
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.RoundingMode
import java.util.*

object BetUtils {

    val tempBetDataList = ArrayList<TempBetData>()

    suspend fun deleteBetData(raceId: String): ArrayList<BetListData> {
        val list = arrayListOf<BetListData>()
        newSuspendedTransaction {
            BetList.selectAll().where { BetList.raceId eq raceId }.forEach {
                list.add(
                    BetListData(
                        it[BetList.rowUniqueId].toUUID(),
                        it[BetList.timeStamp],
                        it[BetList.playerUniqueId].toUUID(),
                        it[BetList.jockeyUniqueId].toUUID(),
                        it[BetList.betting]
                    )
                )
            }
        }
        newSuspendedTransaction(Dispatchers.IO) {
            BetList.deleteWhere { BetList.raceId eq raceId }

        }
        return list
    }

    fun removePlayerTempBetData(player: Player) {
        val removeList = arrayListOf<TempBetData>()
        tempBetDataList.stream().filter { it.player == player }.forEach { removeList.add(it) }
        removeList.forEach { tempBetDataList.remove(it) }
    }

    fun initializePlayerTempBetData(raceId: String, sender: Player) {
        BetChestGui.AllPlayers[raceId]?.forEach { jockey ->
            tempBetDataList.add(TempBetData(raceId, sender, jockey, 0))
        }
    }

    suspend fun listBetData(raceId: String): ArrayList<BetListData> {
        val list = arrayListOf<BetListData>()
        newSuspendedTransaction(Dispatchers.IO) {
            BetList.selectAll().where { BetList.raceId eq raceId }.forEach {
                list.add(
                    BetListData(
                        it[BetList.rowUniqueId].toUUID(),
                        it[BetList.timeStamp],
                        it[BetList.playerUniqueId].toUUID(),
                        it[BetList.jockeyUniqueId].toUUID(),
                        it[BetList.betting]
                    )
                )
            }
        }
        return list
    }

    suspend fun getBetSum(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.selectAll().where { BetList.raceId eq raceId }.sumOf {
            it[BetList.betting]
        }
    }

    suspend fun getJockeyBetSum(raceId: String, jockey: OfflinePlayer) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.selectAll()
            .where { (BetList.jockeyUniqueId eq jockey.uniqueId.toString()) and (BetList.raceId eq raceId) }
            .sumOf { it[BetList.betting] }
    }

    suspend fun getRowBet(raceId: String, row: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        BetList.selectAll().where { (BetList.rowUniqueId eq row.toString()) and (BetList.raceId eq raceId) }
            .sumOf { it[BetList.betting] }
    }

    fun playerCanPay(raceId: String, amount: Int, executor: CommandSender): Boolean {
        val raceManager = RaceAssist.api.getRaceManager(raceId) ?: return false
        val betManager = RaceAssist.api.getBetManager(raceId) ?: return false
        val owner = raceManager.getOwner()
        val can = betManager.getBalance() >= amount
        if (!can) {
            val locale = executor.locale()
            executor.sendMessage(Lang.getComponent("no-have-money", locale))
            if (owner is Player) {
                owner.sendMessage(Lang.getComponent("no-have-money-owner", owner.locale()))
            }
        }
        return can
    }

    //払い戻し
    suspend fun payDividend(jockey: OfflinePlayer, raceId: String, sender: CommandSender, locale: Locale) {
        val odds = getOdds(raceId, jockey)
        val betManager = RaceAssist.api.getBetManager(raceId) ?: return sender.sendMessage("レースが存在しません")

        newSuspendedTransaction(Dispatchers.IO) {
            BetList.selectAll()
                .where { (BetList.jockeyUniqueId eq jockey.uniqueId.toString()) and (BetList.raceId eq raceId) }
                .forEach {
                    val returnAmount = it[BetList.betting] * odds
                    val returnPlayer = Bukkit.getOfflinePlayer(it[BetList.playerUniqueId].toUUID())
                    withContext(Dispatchers.minecraft) {
                        betManager.depositToPlayer(returnPlayer, returnAmount)
                    }
                    sender.sendMessage(Lang.getComponent("paid-bet-creator", locale, returnPlayer.name, returnAmount))
                    returnPlayer.player?.sendMessage(
                        Lang.getComponent(
                            "paid-bet-player",
                            locale,
                            raceId,
                            returnPlayer.name,
                            jockey.name,
                            returnAmount
                        )
                    )
                }
            BetList.deleteWhere { BetList.raceId eq raceId }
        }
    }

    suspend fun getOdds(raceId: String, jockey: OfflinePlayer): Double {
        val sum = getBetSum(raceId)
        val jockeySum = if (getJockeyBetSum(raceId, jockey) == 0) 0.0001 else getJockeyBetSum(raceId, jockey).toDouble()
        val rate = RaceAssist.api.getBetManager(raceId)!!.getReturnPercent().toDouble() / 100
        return ((sum * rate) / jockeySum).toBigDecimal().setScale(2, RoundingMode.DOWN).toDouble()
    }

}