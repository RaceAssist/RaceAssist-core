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

@file:Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")

package dev.nikomaru.raceassist.api.core.manager

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.data.files.PlaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import dev.nikomaru.raceassist.utils.event.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.awt.Polygon
import kotlin.math.hypot

@Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")
class PlaceManager(val placeId: String) {

    private lateinit var placeConfig: PlaceConfig

    init {
        plugin.launch {
            placeConfig = RaceUtils.getPlaceConfig(placeId)
        }
    }


    /**
     * 競技場の中心点のX座標を取得します。
     * @return 競技場の中心点のX座標
     */
    fun getCentralPointX(): Int? {
        return placeConfig.centralX
    }

    /**
     * 競技場の中心点のY座標を取得します。
     * @return 競技場の中心点のY座標
     */

    fun getCentralPointY(): Int? {
        return placeConfig.centralY
    }

    /**
     * 競技場のゴールの角度を取得します。
     * @return 競技場のゴールの角度
     */

    fun getGoalDegree(): Int {
        return placeConfig.goalDegree
    }

    /**
     * 競技場の方向を取得します。
     * @return 競技場の方向
     */

    fun getReverse(): Boolean {
        return placeConfig.reverse
    }

    /**
     * 競技場の内側のポリゴンを取得します。
     * @return 競技場の内側のポリゴン
     */

    fun getInside(): Polygon {
        return placeConfig.inside
    }


    /**
     * 競技場の内側のポリゴンがきちんと形を保っているかどうかを取得します。
     * @return 競技場の内側のポリゴンがきちんと形を保っているかどうか
     */
    fun getInsideRaceExist(): Boolean {
        return getInside().npoints > 0
    }


    /**
     * 競技場の外側のポリゴンを取得します。
     * @return 競技場の外側のポリゴン
     */

    fun getOutside(): Polygon {
        return placeConfig.outside
    }

    /**
     * 競技場の外側のポリゴンがきちんと形を保っているかどうかを取得します。
     * @return 競技場の外側のポリゴンがきちんと形を保っているかどうか
     */
    fun getOutsideRaceExist(): Boolean {
        return getOutside().npoints > 0
    }

    /**
     * 競技場のトラックが存在するかどうかを取得します。
     * @return 競技場のトラックが存在するかどうか
     */

    fun getTrackExist(): Boolean {
        return getInsideRaceExist() && getOutsideRaceExist()
    }

    /**
     * 競技場のオーナーを取得します。
     * @return 競技場のオーナー
     */

    fun getOwner(): OfflinePlayer {
        return placeConfig.owner
    }

    /**
     * 競技場のスタッフを取得します。
     * @return 競技場のスタッフ
     */

    fun getStaffs(): List<OfflinePlayer> {
        return placeConfig.staff
    }

    fun calculateLength(): Double {
        var total = 0.0
        val insidePolygon = getInside()
        val insideX = insidePolygon.xpoints
        val insideY = insidePolygon.ypoints
        for (i in 0 until insidePolygon.npoints) {
            total += if (i <= insidePolygon.npoints - 2) {
                hypot((insideX[i] - insideX[i + 1]).toDouble(), (insideY[i] - insideY[i + 1]).toDouble())
            } else {
                hypot((insideX[i] - insideX[0]).toDouble(), (insideY[i] - insideY[0]).toDouble())
            }
        }
        return total
    }

    /**
     * 競技場の中心点のX座標を設定します。
     * @param x 競技場の中心点のX座標
     */

    fun setCentralPointX(x: Int) {
        placeConfig = placeConfig.copy(centralX = x)
        save()
    }

    /**
     * 競技場の中心点のY座標を設定します。
     * @param y 競技場の中心点のY座標
     */

    fun setCentralPointY(y: Int) {
        placeConfig = placeConfig.copy(centralY = y)
        save()
    }

    /**
     * 競技場のゴールの角度を設定します。
     * @param degree 競技場のゴールの角度
     */

    fun setGoalDegree(degree: Int) {
        placeConfig = placeConfig.copy(goalDegree = degree)
        save()
    }

    /**
     * 競技場の方向を設定します。
     * @param reverse 競技場の方向
     */

    fun setReverse(reverse: Boolean) {
        placeConfig = placeConfig.copy(reverse = reverse)
        save()
    }

    /**
     * 競技場の内側のポリゴンを設定します。
     * @param polygon 競技場の内側のポリゴン
     */

    fun setInside(polygon: Polygon) {
        placeConfig = placeConfig.copy(inside = polygon)
        save()
    }

    /**
     * 競技場の外側のポリゴンを設定します。
     * @param polygon 競技場の外側のポリゴン
     */

    fun setOutside(polygon: Polygon) {
        placeConfig = placeConfig.copy(outside = polygon)
        save()
    }

    /**
     * 競技場のオーナーを設定します。
     * @param owner 競技場のオーナー
     */

    fun setOwner(owner: OfflinePlayer) {
        placeConfig = placeConfig.copy(owner = owner)
        save()
    }

    /**
     * 競技場のスタッフにプレイヤーが含まれているか確認します。
     * @param player プレイヤー
     */

    fun existStaff(player: OfflinePlayer): Boolean {
        return player in placeConfig.staff || player == placeConfig.owner
    }

    /**
     * 競技場のスタッフを設定します。
     * @param staffs 競技場のスタッフ
     */

    fun setStaffs(staffs: ArrayList<OfflinePlayer>) {
        placeConfig = placeConfig.copy(staff = staffs)
        save()
    }

    /**
     * 競技場のスタッフを追加します。
     * @param player 競技場のスタッフ
     */

    fun addStaff(player: OfflinePlayer): Boolean {
        val staffs = placeConfig.staff
        if (player in staffs) return false
        staffs.add(player)
        placeConfig = placeConfig.copy(staff = staffs)
        save()
        return true
    }

    /**
     * 競技場のスタッフを削除します。
     * @param player 競技場のスタッフ
     */

    fun removeStaff(player: OfflinePlayer): Boolean {
        val staffs = placeConfig.staff
        if (player !in staffs) return false
        staffs.remove(player)
        placeConfig = placeConfig.copy(staff = staffs)
        save()
        return true
    }

    /**
     * CommandSenderが競技場を管理できるかを確認します。
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
     * 競技場の設定を保存します。
     */

    private fun save() {
        plugin.launch {
            withContext(Dispatchers.IO) {
                placeConfig.save()
            }
        }
    }


}