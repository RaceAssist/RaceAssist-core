/*
 *  Copyright © 2021 Nikomaru
 *
 *  This program is free software: you can redistribute it and/or modify
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
package dev.nikomaru.keibaassist.race.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Subcommand
import dev.nikomaru.keibaassist.database.Database
import dev.nikomaru.keibaassist.race.utils.InsideCircuit
import dev.nikomaru.keibaassist.race.utils.OutsideCircuit
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.awt.Polygon
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection
import java.sql.SQLException
import java.util.*

@CommandAlias("ka|KeibaAssist")
@Subcommand("place")
class SettingCircuit : BaseCommand() {
    private fun getPolygon(raceID: String, inside: Boolean): Polygon {
        val polygon = Polygon()
        val database = Database()
        val connection: Connection? = database.connection

        if (connection != null) {
            try {
                val statement = connection.prepareStatement(
                    "SELECT * FROM CircuitPoint WHERE RaceID = ? AND Inside = ?"
                )
                statement.setString(1, raceID)
                statement.setBoolean(2, inside)
                val rs = statement.executeQuery()
                while (rs.next()) {
                    polygon.addPoint(rs.getInt(3), rs.getInt(4))
                }
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
        }
        return polygon
    }


    @Subcommand("Circuit")
    @CommandCompletion("@RaceID in|out")
    operator fun set(sender: CommandSender, raceID: String, type: String) {
        //TODO sender check
        val player = sender as Player
        try {
            val database = Database()
            val connection: Connection = database.connection ?: return
            val statement = connection.prepareStatement("SELECT * FROM RaceList WHERE RaceID = ?")
            statement.setString(1, raceID)
            val rs = statement.executeQuery()
            if (!rs.next()) {
                player.sendMessage(text("レースが存在しません", TextColor.color(RED)))
                return
            }
            if (player.uniqueId != UUID.fromString(rs.getString(2))) {
                player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
                return
            }
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        if (Objects.nonNull(canSetOutsideCircuit[player.uniqueId]) && Objects.nonNull(
                canSetInsideCircuit[player.uniqueId]
            )
        ) {
            player.sendMessage("すでに設定モードになっています")
            return
        }
        if (type == "in") {
            canSetInsideCircuit[player.uniqueId] = true
            player.sendMessage(text("内側のコース設定モードになりました", TextColor.color(GREEN)))
        } else if (type == "out") {
            var existRaceInsidePoints = false
            try {
                val database = Database()
                val connection: Connection = database.connection ?: return
                val statement = connection.prepareStatement(
                    "SELECT * FROM CircuitPoint WHERE Inside = true"
                )
                val rs = statement.executeQuery()
                if (rs.next()) {
                    existRaceInsidePoints = true
                }
                rs.close()
                statement.close()
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
            if (!existRaceInsidePoints) {
                player.sendMessage("内側のコースが設定されていません")
                return
            }
            canSetOutsideCircuit[player.uniqueId] = true
            player.sendMessage(text("外側のコース設定モードになりました", TextColor.color(GREEN)))
        }
        Companion.raceID[player.uniqueId] = raceID
        player.sendMessage(text("左クリックで設定を開始し,右クリックで中断します", TextColor.color(GREEN)))
        player.sendMessage("設定を終了する場合は/KeibaAssist race circuit finish と入力してください")
    }


    @Subcommand("finish")
    fun finish(sender: CommandSender) {
        val player = sender as Player
        if (Objects.isNull(canSetOutsideCircuit[player.uniqueId]) && Objects.isNull(canSetInsideCircuit[player.uniqueId])) {
            player.sendMessage("設定にあなたは現在なっていません")
            return
        }
        if (Objects.nonNull(canSetInsideCircuit[player.uniqueId])) {
            canSetInsideCircuit.remove(player.uniqueId)
            InsideCircuit.finish(player)
            player.sendMessage("内側のコース設定を終了しました")
        }
        if (Objects.nonNull(canSetOutsideCircuit[player.uniqueId])) {
            canSetOutsideCircuit.remove(player.uniqueId)
            OutsideCircuit.finish(player)
            player.sendMessage("外側のコース設定を終了しました")
        }
    }

    companion object {
        //TODO 2  set circuit in out 競馬場作成  右クリック 削除 左クリック 点追加 outは polygon内に点が入らないように 内外判定を実施
        //TODO 3.1 set  finish
        //TODO 4 set  judge 測定機構を追加
        private var canSetInsideCircuit = HashMap<UUID, Boolean>()
        private var canSetOutsideCircuit = HashMap<UUID, Boolean>()
        private var raceID = HashMap<UUID, String>()

        private fun round(num: Double): Double {
            val numDecBefore = BigDecimal(num)
            val numDecAfter = numDecBefore.setScale(5, RoundingMode.HALF_UP)
            return numDecAfter.toDouble()
        }

        fun getCanSetInsideCircuit(): HashMap<UUID, Boolean> {
            return canSetInsideCircuit
        }

        fun getCanSetOutsideCircuit(): HashMap<UUID, Boolean> {
            return canSetOutsideCircuit
        }

        fun getRaceID(): HashMap<UUID, String> {
            return raceID
        }

        fun putCanSetInsideCircuit(uniqueId: UUID, b: Boolean) {
            canSetInsideCircuit[uniqueId] = b
        }

        fun putCanSetOutsideCircuit(uniqueId: UUID, b: Boolean) {
            canSetOutsideCircuit[uniqueId] = b
        }

        fun removeCanSetInsideCircuit(uniqueId: UUID) {
            canSetInsideCircuit.remove(uniqueId)
        }

        fun removeCanSetOutsideCircuit(uniqueId: UUID) {
            canSetOutsideCircuit.remove(uniqueId)
        }
    }
}