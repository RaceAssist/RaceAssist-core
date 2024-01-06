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

package dev.nikomaru.raceassist.api.core.manager

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.data.database.BetList.rowUniqueId
import dev.nikomaru.raceassist.data.files.RaceUtils.getRaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import dev.nikomaru.raceassist.data.plugin.BetConfig
import dev.nikomaru.raceassist.event.LogDataType
import dev.nikomaru.raceassist.event.bet.BetEvent
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.web.api.BetError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.*

class BetManager(val raceId: String) {


    init {
        runBlocking {
            betConfig[raceId] = getRaceConfig(raceId).betConfig
        }
    }


    /**
     * 賭けが可能かを取得します。
     * @return ベットの可用性
     */
    fun getAvailable(): Boolean {

        return betConfig[raceId]!!.available
    }

    /**
     * 賭けの返却率を取得します。
     * @return ベットの返却率
     */

    fun getReturnPercent(): Int {

        return betConfig[raceId]!!.returnPercent
    }

    /**
     * 賭けのスプレッドシートIDを取得します。
     * @return ベットのスプレッドシートID
     */

    fun getReturnSpreadSheetId(): String? {

        return betConfig[raceId]!!.spreadSheetId
    }

    /**
     * 賭けのベット単位を取得します。
     * @return ベットのベット単位
     */

    fun getBetUnit(): Int {

        return betConfig[raceId]!!.betUnit
    }

    /**
     * 賭けの可用性を設定します。
     * @param available ベットの可用性
     */

    fun setAvailable(available: Boolean) {
        betConfig[raceId] = betConfig[raceId]!!.copy(available = available)
        save()
    }

    /**
     * 賭けの返却率を設定します。
     * @param returnPercent ベットの返却率
     */
    fun setReturnPercent(returnPercent: Int) {

        betConfig[raceId] = betConfig[raceId]!!.copy(returnPercent = returnPercent)
        save()
    }

    /**
     * 賭けのスプレッドシートIDを設定します。
     * @param spreadSheetId ベットのスプレッドシートID
     */

    fun setSpreadSheetId(spreadSheetId: String) {

        betConfig[raceId] = betConfig[raceId]!!.copy(spreadSheetId = spreadSheetId)
        save()
    }

    /**
     * 賭けのベット単位を設定します。
     * @param betUnit ベットのベット単位
     */

    fun setBetUnit(betUnit: Int) {
        betConfig[raceId] = betConfig[raceId]!!.copy(betUnit = betUnit)
        save()
    }

    /**
     * レースの預金残高を取得します。
     * @return レースの預金残高
     */

    fun getBalance(): Double {
        return betConfig[raceId]!!.money
    }

    /**
     * プレイヤーからお金を引き、レースの預金残高に加算します。
     * @param player プレイヤー
     * @param amount 引き出す金額
     *
     */
    fun withdrawFromPlayer(player: OfflinePlayer, amount: Double) {
        runBlocking {
            VaultAPI.getEconomy().withdrawPlayer(player, amount)
            betConfig[raceId] = betConfig[raceId]!!.copy(money = betConfig[raceId]!!.money + amount)
            save()
        }
    }

    /**
     * レースの預金残高からお金を引き、プレイヤーに加算します。
     * @param player プレイヤー
     * @param amount 引き出す金額
     */
    fun depositToPlayer(player: OfflinePlayer, amount: Double) {
        runBlocking {
            VaultAPI.getEconomy().depositPlayer(player, amount)
            betConfig[raceId] = betConfig[raceId]!!.copy(money = betConfig[raceId]!!.money - amount)
            save()
        }
    }

    /**
     *
     *
     */

    fun pushBet(player: OfflinePlayer, jockey: OfflinePlayer, multiple: Int): Triple<BetError?, UUID?, Int> {
        val uniqueId = UUID.randomUUID()
        val price = multiple * getBetUnit()
        val eco = VaultAPI.getEconomy()

        if (eco.getBalance(player) < price) {
            player.player?.sendMessage("no money")
            return Triple(BetError.NO_MONEY, null, 0)
        }
        if (!getAvailable()) {
            player.player?.sendMessage("not available")
            return Triple(BetError.NOT_AVAILABLE, null, 0)
        }


        transaction {
            BetList.insert { bet ->
                bet[raceId] = raceId
                bet[playerUniqueId] = player.uniqueId.toString()
                bet[jockeyUniqueId] = jockey.uniqueId.toString()
                bet[betting] = price
                bet[timeStamp] = LocalDateTime.now()
                bet[rowUniqueId] = uniqueId.toString()
            }
        }

        val onlinePlayer = player.player
        onlinePlayer?.sendMessage(
            Lang.getComponent(
                "bet-complete-message-player",
                onlinePlayer.locale(),
                rowUniqueId.toString(),
                jockey.name.toString(),
                price
            )
        )

        withdrawFromPlayer(player, price.toDouble())
        BetEvent(LogDataType.BET, raceId, player, jockey, price).callEvent()
        return Triple(null, uniqueId, price)
    }

    /**
     * 賭けの設定を保存します。
     */

    private fun save() {
        plugin.launch {
            withContext(Dispatchers.IO) {
                getRaceConfig(raceId).copy(betConfig = betConfig[raceId]!!).save()
            }
        }
    }

    companion object {
        val betConfig = hashMapOf<String, BetConfig>()
    }

}