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

package dev.nikomaru.raceassist.api.core.manager

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import dev.nikomaru.raceassist.data.plugin.CalculatePolygon
import dev.nikomaru.raceassist.data.plugin.PlainPlaceConfig
import dev.nikomaru.raceassist.data.plugin.PlaneVectorPlaceConfig
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.Polygon
import kotlin.math.hypot
import kotlin.math.roundToInt

sealed class PlaceManager(val placeId: String) : KoinComponent {
    val plugin: RaceAssist by inject()

    class PlainPlaceManager(placeId: String) : PlaceManager(placeId) {

        init {
            runBlocking {
                plainPlaceConfig[placeId] = RaceUtils.getPlainPlaceConfig(placeId)
            }
        }

        /**
         * 競技場の中心点のX座標を取得します。
         * @return 競技場の中心点のX座標
         */
        fun getCentralPointX(): Int? {
            return plainPlaceConfig[placeId]!!.centralX
        }

        /**
         * 競技場の中心点のY座標を取得します。
         * @return 競技場の中心点のY座標
         */

        fun getCentralPointY(): Int? {
            return plainPlaceConfig[placeId]!!.centralY
        }

        /**
         * 競技場のゴールの角度を取得します。
         * @return 競技場のゴールの角度
         */

        fun getGoalDegree(): Int {
            return plainPlaceConfig[placeId]!!.goalDegree
        }

        /**
         * 競技場の方向を取得します。
         * @return 競技場の方向
         */

        fun getReverse(): Boolean {
            return plainPlaceConfig[placeId]!!.reverse
        }

        /**
         * 競技場の内側のポリゴンを取得します。
         * @return 競技場の内側のポリゴン
         */

        fun getInside(): Polygon {
            return plainPlaceConfig[placeId]!!.inside
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
            return plainPlaceConfig[placeId]!!.outside
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
         * 競技場の画像を取得します。
         * @return　競技場の画像のbase64
         */

        fun getImage(): String? {
            return plainPlaceConfig[placeId]!!.image
        }

        /**
         * 競技場のオーナーを取得します。
         * @return 競技場のオーナー
         */

        override fun getOwner(): OfflinePlayer {
            return plainPlaceConfig[placeId]!!.owner
        }

        /**
         * 競技場のスタッフを取得します。
         * @return 競技場のスタッフ
         */

        override fun getStaffs(): List<OfflinePlayer> {
            return plainPlaceConfig[placeId]!!.staff
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
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(centralX = x)
            save()
        }

        /**
         * 競技場の中心点のY座標を設定します。
         * @param y 競技場の中心点のY座標
         */

        fun setCentralPointY(y: Int) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(centralY = y)
            save()
        }

        /**
         * 競技場のゴールの角度を設定します。
         * @param degree 競技場のゴールの角度
         */

        fun setGoalDegree(degree: Int) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(goalDegree = degree)
            save()
        }

        /**
         * 競技場の方向を設定します。
         * @param reverse 競技場の方向
         */

        fun setReverse(reverse: Boolean) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(reverse = reverse)
            save()
        }

        /**
         * 競技場の内側のポリゴンを設定します。
         * @param polygon 競技場の内側のポリゴン
         */

        fun setInside(polygon: Polygon) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(inside = polygon)
            save()
        }

        /**
         * 競技場の外側のポリゴンを設定します。
         * @param polygon 競技場の外側のポリゴン
         */

        fun setOutside(polygon: Polygon) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(outside = polygon)
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { refreshImage() })
            save()
        }

        fun refreshImage() {
            val rectangle = getOutside().bounds2D
            lateinit var image: String
            plugin.launch {
                withContext(Dispatchers.IO) {
                    image = Utils.createImage(
                        rectangle.minX.roundToInt() - 10,
                        rectangle.maxX.roundToInt() + 10,
                        rectangle.minY.roundToInt() - 10,
                        rectangle.maxY.roundToInt() + 10
                    )
                    plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(image = image)
                    save()
                }
            }
        }

        /**
         * 競技場のオーナーを設定します。
         * @param owner 競技場のオーナー
         */

        override fun setOwner(owner: OfflinePlayer) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(owner = owner)
            save()
        }

        /**
         * 競技場のスタッフにプレイヤーが含まれているか確認します。
         * @param player プレイヤー
         */

        override fun existStaff(player: OfflinePlayer): Boolean {
            return player in plainPlaceConfig[placeId]!!.staff || player == plainPlaceConfig[placeId]!!.owner
        }

        /**
         * 競技場のスタッフを設定します。
         * @param staffs 競技場のスタッフ
         */

        override fun setStaffs(staffs: ArrayList<OfflinePlayer>) {
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(staff = staffs)
            save()
        }

        /**
         * 競技場のスタッフを追加します。
         * @param player 競技場のスタッフ
         */

        override fun addStaff(player: OfflinePlayer): Boolean {
            val staffs = plainPlaceConfig[placeId]!!.staff
            if (player in staffs) return false
            staffs.add(player)
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(staff = staffs)
            save()
            return true
        }

        /**
         * 競技場のスタッフを削除します。
         * @param player 競技場のスタッフ
         */

        override fun removeStaff(player: OfflinePlayer): Boolean {
            val staffs = plainPlaceConfig[placeId]!!.staff
            if (player !in staffs) return false
            staffs.remove(player)
            plainPlaceConfig[placeId] = plainPlaceConfig[placeId]!!.copy(staff = staffs)
            save()
            return true
        }

        /**
         * CommandSenderが競技場を管理できるかを確認します。
         * @param sender CommandSender
         */

        override fun senderHasControlPermission(sender: CommandSender): Boolean {
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
                    plainPlaceConfig[placeId]!!.save()
                }
            }
        }

        companion object {
            val plainPlaceConfig: HashMap<String, PlainPlaceConfig> = HashMap()
        }
    }

    class PlaneVectorPlaceManager(placeId: String) : PlaceManager(placeId) {
        init {
            runBlocking {
                planeVectorPlaceConfig[placeId] = RaceUtils.getPlaneVectorPlaceConfig(placeId)
            }
        }


        /**
         * 競技場の内側のポリゴンを取得します。
         * @return 競技場の内側のポリゴン
         */

        fun getInside(): Polygon {
            return planeVectorPlaceConfig[placeId]!!.inside
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
            return planeVectorPlaceConfig[placeId]!!.outside
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
         * 計算用のポリゴンを取得します。
         * @return 計算用のポリゴン
         */

        fun getCalculatePolygonList(): ArrayList<CalculatePolygon> {
            return planeVectorPlaceConfig[placeId]!!.calculatePolygonList
        }


        /**
         * 競技場の画像を取得します。
         * @return　競技場の画像のbase64
         */

        fun getImage(): String? {
            return planeVectorPlaceConfig[placeId]!!.image
        }

        /**
         * 競技場のオーナーを取得します。
         * @return 競技場のオーナー
         */

        override fun getOwner(): OfflinePlayer {
            return planeVectorPlaceConfig[placeId]!!.owner
        }

        /**
         * 競技場のスタッフを取得します。
         * @return 競技場のスタッフ
         */

        override fun getStaffs(): List<OfflinePlayer> {
            return planeVectorPlaceConfig[placeId]!!.staff
        }

        fun calculateLength(): Double {
            var total = 0.0
            getCalculatePolygonList().forEach {
                total += hypot((it.start.x - it.end.x).toDouble(), (it.start.y - it.end.y).toDouble())
            }
            return total
        }


        /**
         * 競技場の内側のポリゴンを設定します。
         * @param polygon 競技場の内側のポリゴン
         */

        fun setInside(polygon: Polygon) {
            planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(inside = polygon)
            save()
        }

        /**
         * 競技場の外側のポリゴンを設定します。
         * @param polygon 競技場の外側のポリゴン
         */

        fun setOutside(polygon: Polygon) {
            planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(outside = polygon)
            plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { refreshImage() })
            save()
        }

        fun refreshImage() {
            val rectangle = getOutside().bounds2D
            lateinit var image: String
            plugin.launch {
                withContext(Dispatchers.IO) {
                    image = Utils.createImage(
                        rectangle.minX.roundToInt() - 10,
                        rectangle.maxX.roundToInt() + 10,
                        rectangle.minY.roundToInt() - 10,
                        rectangle.maxY.roundToInt() + 10
                    )
                    planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(image = image)
                    save()
                }
            }
        }

        /**
         * 競技場のオーナーを設定します。
         * @param owner 競技場のオーナー
         */

        override fun setOwner(owner: OfflinePlayer) {
            planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(owner = owner)
            save()
        }

        /**
         * 競技場のスタッフにプレイヤーが含まれているか確認します。
         * @param player プレイヤー
         */

        override fun existStaff(player: OfflinePlayer): Boolean {

            return player in planeVectorPlaceConfig[placeId]!!.staff || player == planeVectorPlaceConfig[placeId]!!.owner
        }

        /**
         * 競技場のスタッフを設定します。
         * @param staffs 競技場のスタッフ
         */

        override fun setStaffs(staffs: ArrayList<OfflinePlayer>) {
            planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(staff = staffs)
            save()
        }

        /**
         * 競技場のスタッフを追加します。
         * @param player 競技場のスタッフ
         */

        override fun addStaff(player: OfflinePlayer): Boolean {
            val staffs = planeVectorPlaceConfig[placeId]!!.staff
            if (player in staffs) return false
            staffs.add(player)
            planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(staff = staffs)
            save()
            return true
        }

        /**
         * 競技場のスタッフを削除します。
         * @param player 競技場のスタッフ
         */

        override fun removeStaff(player: OfflinePlayer): Boolean {
            val staffs = planeVectorPlaceConfig[placeId]!!.staff
            if (player !in staffs) return false
            staffs.remove(player)
            planeVectorPlaceConfig[placeId] = planeVectorPlaceConfig[placeId]!!.copy(staff = staffs)
            save()
            return true
        }

        /**
         * CommandSenderが競技場を管理できるかを確認します。
         * @param sender CommandSender
         */

        override fun senderHasControlPermission(sender: CommandSender): Boolean {
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
                    planeVectorPlaceConfig[placeId]!!.save()
                }
            }
        }

        companion object {
            private val planeVectorPlaceConfig: HashMap<String, PlaneVectorPlaceConfig> = HashMap()
        }
    }

    abstract fun senderHasControlPermission(sender: CommandSender): Boolean
    abstract fun getOwner(): OfflinePlayer
    abstract fun setOwner(owner: OfflinePlayer)
    abstract fun getStaffs(): List<OfflinePlayer>

    abstract fun existStaff(player: OfflinePlayer): Boolean
    abstract fun setStaffs(staffs: ArrayList<OfflinePlayer>)
    abstract fun addStaff(player: OfflinePlayer): Boolean
    abstract fun removeStaff(player: OfflinePlayer): Boolean

}