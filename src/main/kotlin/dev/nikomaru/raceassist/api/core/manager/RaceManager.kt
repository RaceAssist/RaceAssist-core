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
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import dev.nikomaru.raceassist.data.plugin.RaceConfig
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*

class RaceManager(val raceId: String) {


    init {
        runBlocking {
            raceConfig[raceId] = RaceUtils.getRaceConfig(raceId)
        }
    }

    /**
     * 今のレースコンフィグをベースとして新しいレースコンフィグを作成します。
     * @param newRaceId 新しいレースID
     * @param owner 新しいレースのオーナー
     */
    fun copyRace(newRaceId: String, owner: OfflinePlayer) {
        plugin.launch {
            withContext(Dispatchers.IO) {
                val afterBetData = raceConfig[raceId]!!.betConfig.copy(available = false)
                val afterData = raceConfig[raceId]!!.copy(
                    raceId = newRaceId,
                    owner = owner,
                    staff = arrayListOf(owner),
                    betConfig = afterBetData,
                    jockeys = arrayListOf()
                )
                afterData.save()
            }
        }
    }

    /**
     * CommandSenderがレースを管理できるかを確認します。
     * @param sender CommandSender
     */

    fun senderHasControlPermission(sender: CommandSender): Boolean {
        if (sender is ConsoleCommandSender) {
            return true
        }
        (sender as Player)
        if (!existStaff(sender)) {
            sender.sendMessage(Lang.getComponent("only-race-creator-can-setting", sender.locale()))
            return false
        }
        return true
    }

    /**
     * レースの競技場Idを取得します。
     * @return 競技場Id
     */

    fun getPlaceId(): String {
        return raceConfig[raceId]!!.placeId
    }


    /**
     * レースのオーナーを取得します。
     * @return オーナー
     */
    fun getOwner(): OfflinePlayer {
        return raceConfig[raceId]!!.owner
    }

    /**
     * レースのスタッフを取得します。
     * @return スタッフのリスト
     */
    fun getJockeys(): ArrayList<OfflinePlayer> {
        return raceConfig[raceId]!!.jockeys
    }

    /**
     * レースのスタッフを取得します。
     * @return スタッフのリスト
     */

    fun getStaffs(): ArrayList<OfflinePlayer> {
        return raceConfig[raceId]!!.staff
    }

    /**
     * レースのスタッフを追加します。
     * @param player 追加するプレイヤー
     * @return 追加できたかどうか(存在したらfalse)
     */

    fun addStaff(player: OfflinePlayer): Boolean {

        if (player in raceConfig[raceId]!!.staff) {
            return false
        }
        raceConfig[raceId] = raceConfig[raceId]!!.copy(staff = raceConfig[raceId]!!.staff.apply { add(player) })
        save()
        return true
    }

    /**
     * レースのスタッフを削除します。
     * @param player 削除するプレイヤー
     * @return 削除できたかどうか(存在しなかったらfalse)
     */

    fun removeStaff(player: OfflinePlayer): Boolean {
        if (player !in raceConfig[raceId]!!.staff) {
            return false
        }
        raceConfig[raceId] = raceConfig[raceId]!!.copy(staff = raceConfig[raceId]!!.staff.apply { remove(player) })
        save()
        return true
    }


    /**
     * レースのスタッフにプレイヤーが含まれているか確認します。
     * @param player プレイヤー
     */

    fun existStaff(player: OfflinePlayer): Boolean {
        return player in raceConfig[raceId]!!.staff || player == raceConfig[raceId]!!.owner
    }

    /**
     * レースの騎手を追加します。
     * @param jockey 追加する騎手
     */
    fun addJockey(jockey: OfflinePlayer) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(jockeys = raceConfig[raceId]!!.jockeys.apply { add(jockey) })
        save()
    }

    /**
     * レースの騎手を削除します。
     * @param jockey 削除する騎手
     */

    fun removeJockey(jockey: OfflinePlayer) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(jockeys = raceConfig[raceId]!!.jockeys.apply { remove(jockey) })
        save()
    }


    /**
     * レースのラップ数を取得します。
     * @return ラップ数
     */

    fun getLap(): Int {
        return raceConfig[raceId]!!.lap
    }


    /**
     * 騎手と名前の置き換えを取得します。
     * @return 騎手と名前の置き換え
     */
    fun getReplacement(): HashMap<UUID, String> {
        return raceConfig[raceId]!!.replacement
    }

    /**
     * 騎手と名前の置き換えを追加
     * @param uniqueId 騎手のUUID
     */

    fun addReplacement(uniqueId: UUID, replacement: String) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(replacement = raceConfig[raceId]!!.replacement.apply {
            put(
                uniqueId,
                replacement
            )
        })
        save()
    }

    /**
     * 騎手と名前の置き換えを削除
     * @param uniqueId 騎手のUUID
     */

    fun removeReplacement(uniqueId: UUID) {
        raceConfig[raceId] =
            raceConfig[raceId]!!.copy(replacement = raceConfig[raceId]!!.replacement.apply { remove(uniqueId) })
        save()
    }


    fun deleteReplacement() {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(replacement = hashMapOf())
        save()
    }

    /**
     * レースの騎手と馬のUUIDを取得します。
     * @return 騎手と馬のUUIDのMap
     */

    fun getHorse(): HashMap<UUID, UUID> {
        return raceConfig[raceId]!!.horse
    }

    /**
     * レースに出走する馬を削除します。
     * @param uniqueId 馬のUUID
     */

    fun removeHorse(uniqueId: UUID) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(horse = raceConfig[raceId]!!.horse.apply { remove(uniqueId) })
        save()
    }

    /**
     * レースに出走する馬をすべて削除します。
     */

    fun deleteHorse() {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(horse = hashMapOf())
        save()
    }

    /**
     * レースの競技場を設定します。
     * @param placeId 競技場
     */
    fun setPlaceId(placeId: String) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(placeId = placeId)
        save()
    }

    /**
     * レースのLapを設定します。
     * @param lap Lap
     */

    fun setLap(lap: Int) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(lap = lap)
        save()
    }

    /**
     * 騎手と名前の置き換えを設定します。
     * @param player 騎手
     * @param replacement 置き換える名前
     */

    fun setReplacement(player: UUID, replacement: String) {
        raceConfig[raceId] =
            raceConfig[raceId]!!.copy(replacement = raceConfig[raceId]!!.replacement.apply { put(player, replacement) })
        save()
    }

    /**
     * 騎手と馬のUUIDを設定します。
     * @param player 騎手
     * @param horse 馬
     */

    fun setHorse(player: UUID, horse: UUID) {
        raceConfig[raceId] = raceConfig[raceId]!!.copy(horse = raceConfig[raceId]!!.horse.apply { put(player, horse) })
        save()
    }


    /**
     * レースのコンフィグを保存します。
     */
    private fun save() {
        plugin.launch {
            withContext(Dispatchers.IO) {
                raceConfig[raceId]!!.save()
            }
        }
    }

    companion object {
        val raceConfig = hashMapOf<String, RaceConfig>()
    }


}