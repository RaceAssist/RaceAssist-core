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
import dev.nikomaru.raceassist.data.files.RaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import dev.nikomaru.raceassist.utils.event.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*

class RaceManager(val raceId: String) {

    private lateinit var raceConfig: RaceConfig

    init {
        plugin.launch {
            raceConfig = RaceUtils.getRaceConfig(raceId)
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
                val afterBetData = raceConfig.bet.copy(available = false)
                val afterData = raceConfig.copy(
                    raceId = newRaceId,
                    owner = owner,
                    staff = arrayListOf(owner),
                    bet = afterBetData,
                    jockeys = arrayListOf()
                )
                afterData.save()
            }
        }
    }

    /**
     * レースのオーナーを取得します。
     * @return オーナー
     */
    fun getOwner(): OfflinePlayer {
        return raceConfig.owner
    }

    /**
     * レースのスタッフを取得します。
     * @return スタッフのリスト
     */
    fun getJockeys(): ArrayList<OfflinePlayer> {
        return raceConfig.jockeys
    }

    /**
     * レースのスタッフにプレイヤーが含まれているか確認します。
     * @param player プレイヤー
     */

    fun existStaff(player: OfflinePlayer): Boolean {
        return player in raceConfig.staff || player == raceConfig.owner
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
     * レースの騎手と馬のUUIDを取得します。
     * @return 騎手と馬のUUIDのMap
     */

    fun getHorse(): HashMap<UUID, UUID> {
        return raceConfig.horse
    }

    /**
     * レースの競技場Idを取得します。
     * @return 競技場Id
     */

    fun getPlaceId(): String {
        return raceConfig.placeId
    }

    /**
     * レースのラップ数を取得します。
     * @return ラップ数
     */

    fun getLap(): Int {
        return raceConfig.lap
    }

    /**
     * 騎手と名前の置き換えを取得します。
     * @return 騎手と名前の置き換え
     */
    fun getReplacement(): HashMap<UUID, String> {
        return raceConfig.replacement
    }

    /**
     * レースの騎手を追加します。
     * @param jockey 追加する騎手
     */
    fun addJockey(jockey: OfflinePlayer) {
        raceConfig = raceConfig.copy(jockeys = raceConfig.jockeys.apply { add(jockey) })
        save()
    }

    /**
     * レースの騎手を削除します。
     * @param jockey 削除する騎手
     */

    fun removeJockey(jockey: OfflinePlayer) {
        raceConfig = raceConfig.copy(jockeys = raceConfig.jockeys.apply { remove(jockey) })
        save()
    }

    /**
     * レースのコンフィグを保存します。
     */
    private fun save() {
        plugin.launch {
            withContext(Dispatchers.IO) {
                raceConfig.save()
            }
        }
    }

    /**
     * 騎手と名前の置き換えを追加
     * @param uniqueId 騎手のUUID
     */

    fun addReplacement(uniqueId: UUID, replacement: String) {
        raceConfig = raceConfig.copy(replacement = raceConfig.replacement.apply { put(uniqueId, replacement) })
        save()
    }

    /**
     * 騎手と名前の置き換えを削除
     * @param uniqueId 騎手のUUID
     */

    fun removeReplacement(uniqueId: UUID) {
        raceConfig = raceConfig.copy(replacement = raceConfig.replacement.apply { remove(uniqueId) })
        save()
    }

    /**
     * レースに出走する馬を削除します。
     * @param uniqueId 馬のUUID
     */

    fun removeHorse(uniqueId: UUID) {
        raceConfig = raceConfig.copy(horse = raceConfig.horse.apply { remove(uniqueId) })
    }

    /**
     * レースに出走する馬をすべて削除します。
     */

    fun deleteHorse() {
        raceConfig = raceConfig.copy(horse = hashMapOf())
    }

    fun setHorse(player: UUID, horse: UUID) {
        raceConfig = raceConfig.copy(horse = raceConfig.horse.apply { put(player, horse) })
    }

    fun setLap(lap: Int) {
        raceConfig = raceConfig.copy(lap = lap)
    }

    fun setPlaceId(placeId: String) {
        raceConfig = raceConfig.copy(placeId = placeId)
    }

    fun setReplacement(player: UUID, replacement: String) {
        raceConfig = raceConfig.copy(replacement = raceConfig.replacement.apply { put(player, replacement) })
    }

    fun deleteReplacement() {
        raceConfig = raceConfig.copy(replacement = hashMapOf())
    }

    fun addStaff(player: OfflinePlayer): Boolean {
        if (player in raceConfig.staff) {
            return false
        }
        raceConfig = raceConfig.copy(staff = raceConfig.staff.apply { add(player) })
        return true
    }

    fun removeStaff(player: OfflinePlayer): Boolean {
        if (player !in raceConfig.staff) {
            return false
        }
        raceConfig = raceConfig.copy(staff = raceConfig.staff.apply { remove(player) })
        return true
    }

    fun getStaffs(): ArrayList<OfflinePlayer> {
        return raceConfig.staff
    }


}