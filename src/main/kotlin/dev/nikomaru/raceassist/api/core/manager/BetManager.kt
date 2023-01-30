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
import dev.nikomaru.raceassist.data.files.Bet
import dev.nikomaru.raceassist.data.files.RaceUtils.getRaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BetManager(val raceId: String) {

    private lateinit var betConfig: Bet

    init {
        plugin.launch {
            withContext(Dispatchers.IO) {
                betConfig = getRaceConfig(raceId).bet
            }
        }
    }


    /**
     * 賭けが可能かを取得します。
     * @return ベットの可用性
     */
    fun getAvailable(): Boolean {
        return betConfig.available
    }

    /**
     * 賭けの返却率を取得します。
     * @return ベットの返却率
     */

    fun getReturnPercent(): Int {
        return betConfig.returnPercent
    }

    /**
     * 賭けのスプレッドシートIDを取得します。
     * @return ベットのスプレッドシートID
     */

    fun getReturnSpreadSheetId(): String? {
        return betConfig.spreadSheetId
    }

    /**
     * 賭けのベット単位を取得します。
     * @return ベットのベット単位
     */

    fun getBetUnit(): Int {
        return betConfig.betUnit
    }

    /**
     * 賭けの可用性を設定します。
     * @param available ベットの可用性
     */

    fun setAvailable(available: Boolean) {
        betConfig = betConfig.copy(available = available)
        save()
    }

    /**
     * 賭けの返却率を設定します。
     * @param returnPercent ベットの返却率
     */
    fun setReturnPercent(returnPercent: Int) {
        betConfig = betConfig.copy(returnPercent = returnPercent)
        save()
    }

    /**
     * 賭けのスプレッドシートIDを設定します。
     * @param spreadSheetId ベットのスプレッドシートID
     */

    fun setSpreadSheetId(spreadSheetId: String) {
        betConfig = betConfig.copy(spreadSheetId = spreadSheetId)
        save()
    }

    /**
     * 賭けのベット単位を設定します。
     * @param betUnit ベットのベット単位
     */

    fun setBetUnit(betUnit: Int) {
        betConfig = betConfig.copy(betUnit = betUnit)
        save()
    }

    /**
     * 賭けの設定を保存します。
     */

    private fun save() {
        plugin.launch {
            withContext(Dispatchers.IO) {
                getRaceConfig(raceId).copy(bet = betConfig).save()
            }
        }
    }


}