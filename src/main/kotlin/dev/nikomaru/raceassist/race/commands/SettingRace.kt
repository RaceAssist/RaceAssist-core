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
package dev.nikomaru.raceassist.race.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Subcommand
import dev.nikomaru.raceassist.database.Database
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.awt.Polygon
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import kotlin.math.abs
import kotlin.math.atan2

@CommandAlias("ra|RaceAssist")
@Subcommand("race")
class SettingRace : BaseCommand() {


    @Subcommand("start")
    @CommandCompletion("@RaceID")
    suspend fun start(sender: CommandSender, raceID: String) {
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(text("レース作成者しか開始することはできません", TextColor.color(RED)))
        }
        if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
            sender.sendMessage(text("レースが存在しません", TextColor.color(YELLOW)))
            return
        }
        val jockeys: ArrayList<Player> = ArrayList()
        getAllJockeys(raceID)?.forEach { jockey ->
            if (jockey.isOnline) {
                jockeys.add(jockey as Player)
                sender.sendMessage(text("${jockey.name}が参加しました", TextColor.color(GREEN)))
            } else {
                sender.sendMessage(text("${jockey.name}はオフラインです", TextColor.color(YELLOW)))
            }
        }
        if (jockeys.size < 2) {
            sender.sendMessage(text("開催には2人以上のユーザーが必要です", TextColor.color(YELLOW)))
            return
        }
        val centralXPoint: Int =
            getCentralPoint(raceID, true) ?: return sender.sendMessage(text("中心点が存在しません", TextColor.color(YELLOW)))
        val centralYPoint: Int =
            getCentralPoint(raceID, false) ?: return sender.sendMessage(text("中心点が存在しません", TextColor.color(YELLOW)))
        val goalDegree: Int =
            getGoalDegree(raceID) ?: return sender.sendMessage(text("ゴール角度が存在しません", TextColor.color(YELLOW)))
        val lap: Int = getLapCount(raceID)
        val beforePoint: HashMap<UUID, Int> = HashMap()
        val currentLap: HashMap<UUID, Int> = HashMap()
        val threshold = 40 % 90
        jockeys.forEach {
            it.sendMessage(text("${sender.name}が開始しました", TextColor.color(GREEN)))
            beforePoint[it.uniqueId] = 0
            currentLap[it.uniqueId] = 0
        }
        while (jockeys.size >= 1) {
            jockeys.forEach {
                val nowX = it.location.blockX
                val nowY = it.location.blockZ
                val currentDegree = atan2((nowX - centralXPoint).toDouble(), (nowY - centralYPoint).toDouble()).toInt()
                //TODO さすがにゴミコードすぎるのでカスタマイズ性を犠牲にして90度ごとに変更する
                if (goalDegree + threshold >= 360) {
                    if ((beforePoint[it.uniqueId]!! > goalDegree + threshold && beforePoint[it.uniqueId]!! <= goalDegree) && (currentDegree > goalDegree || currentDegree < (goalDegree + threshold) - 360)) {
                        currentLap[it.uniqueId] = currentLap[it.uniqueId]!! + 1
                    } else if ((currentDegree > goalDegree - threshold && currentDegree < goalDegree) && (beforePoint[it.uniqueId]!! > goalDegree && beforePoint[it.uniqueId]!! < (goalDegree + threshold) - 360)) {
                        currentLap[it.uniqueId] = currentLap[it.uniqueId]!! - 1
                    }
                } else if (goalDegree - threshold < 0) {
                    if ((360 - abs(goalDegree - threshold) < beforePoint[it.uniqueId]!! || goalDegree >= beforePoint[it.uniqueId]!!) && ((currentDegree > goalDegree) && (currentDegree < goalDegree + threshold))) {
                        currentLap[it.uniqueId] = currentLap[it.uniqueId]!! + 1
                    } else if ((360 - abs(goalDegree - threshold) < currentDegree || goalDegree >= currentDegree) && ((beforePoint[it.uniqueId]!! > goalDegree) && (beforePoint[it.uniqueId]!! < goalDegree + threshold))) {
                        currentLap[it.uniqueId] = currentLap[it.uniqueId]!! - 1
                    }
                } else {
                    if ((beforePoint[it.uniqueId]!! > (goalDegree - threshold) && beforePoint[it.uniqueId]!! <= goalDegree) && ((currentDegree > goalDegree) && (currentDegree < goalDegree + threshold))) {
                        currentLap[it.uniqueId] = currentLap[it.uniqueId]!! + 1
                    } else if ((currentDegree > (goalDegree - threshold) && currentDegree <= goalDegree) && (beforePoint[it.uniqueId]!! > goalDegree) && (beforePoint[it.uniqueId]!! < goalDegree + threshold)) {
                        currentLap[it.uniqueId] = currentLap[it.uniqueId]!! - 1
                    }
                }
                beforePoint[it.uniqueId] = currentDegree
                if (currentLap[it.uniqueId]!! >= lap) {
                    jockeys.remove(it)
                    it.sendMessage(text("レースが終わったよ", TextColor.color(GREEN)))
                }
                //TODO 順位表示
            }
            delay(1000)
        }
    }


    @Subcommand("stop")
    @CommandCompletion("@RaceID")
    fun stop() {
        //TODO 6 stop レース終了
    }

    @Subcommand("create")
    @CommandCompletion("@RaceID")
    fun create(sender: CommandSender, raceID: String) {
        //TODO 1 create 競馬場作成
        if (getRaceCreator(raceID) != null) {
            sender.sendMessage("その名前のレース場は既に設定されています")
            return
        }
        val connection: Connection = Database.connection ?: return
        try {
            val statement =
                connection.prepareStatement("INSERT INTO RaceList(RaceID,Creator,Reverse,Lap,XCentralXPoint,CentralYPoint,GoalDegree) VALUES (?,?,false,1,null,null,null)")
            statement.setString(1, raceID)
            statement.setString(2, (sender as Player).uniqueId.toString())
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    fun delete(sender: CommandSender, raceID: String) {
        val connection: Connection = Database.connection ?: return

        if (getRaceCreator(raceID) == null) {
            sender.sendMessage("レース場が設定されていません")
            return
        }
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage("レース場作成者が設定してください")
            return
        }
        try {
            val statement = connection.prepareStatement("DELETE FROM RaceList WHERE RaceID = ?")
            statement.setString(1, raceID)
            statement.execute()
            statement.close()
            val statement2 = connection.prepareStatement("DELETE FROM CircuitPoint WHERE RaceID = ?")
            statement2.setString(1, raceID)
            statement2.execute()
            statement2.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    private fun getRaceCreator(RaceID: String): UUID? {
        val connection: Connection = Database.connection ?: return null
        var creatorUUID: UUID? = null
        try {
            val statement = connection.prepareStatement("SELECT FROM RaceList WHERE RaceID = ?")
            statement.setString(1, RaceID)
            val rs: ResultSet = statement.executeQuery()
            while (rs.next()) {
                creatorUUID = UUID.fromString(rs.getString(2))
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return creatorUUID
    }

    private fun getLapCount(raceID: String): Int {
        val connection: Connection = Database.connection ?: return 1
        var lapCount = 1
        try {
            val statement = connection.prepareStatement("SELECT FROM RaceList WHERE RaceID = ?")
            statement.setString(1, raceID)
            val rs: ResultSet = statement.executeQuery()
            while (rs.next()) {
                lapCount = rs.getInt(4)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return lapCount
    }

    private fun getCentralPoint(raceID: String, xPoint: Boolean): Int? {
        try {
            val connection: Connection = Database.connection ?: return 0
            val statement = connection.prepareStatement("SELECT CentralXPoint FROM RaceList WHERE RaceID = ?")
            statement.setString(1, raceID)
            val rs: ResultSet = statement.executeQuery()
            if (rs.next()) {
                return if (xPoint) {
                    rs.getInt(5)
                } else {
                    rs.getInt(6)
                }
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getAllJockeys(raceID: String): ArrayList<OfflinePlayer>? {
        val jockeys: ArrayList<OfflinePlayer> = ArrayList()
        try {
            val connection: Connection = Database.connection ?: return null
            val statement = connection.prepareStatement(
                "SELECT * FROM PlayerList WHERE RaceID = ?"
            )
            statement.setString(1, raceID)
            val rs = statement.executeQuery()
            if (rs.next()) {
                jockeys.add(Bukkit.getOfflinePlayer(UUID.fromString(rs.getString(2))))
            }
            rs.close()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return jockeys
    }

    private fun getGoalDegree(raceID: String): Int? {
        try {
            val connection: Connection = Database.connection ?: return 0
            val statement = connection.prepareStatement("SELECT * FROM RaceList WHERE RaceID = ?")
            statement.setString(1, raceID)
            val rs: ResultSet = statement.executeQuery()
            if (rs.next()) {
                return rs.getInt(7)
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getCircuitExist(raceID: String, inside: Boolean): Boolean {
        val connection: Connection = Database.connection ?: return false
        var raceExist = false
        try {
            val statement = connection.prepareStatement("SELECT FROM CircuitPoint WHERE RaceID = ? AND Inside = ?")
            statement.setString(1, raceID)
            statement.setBoolean(2, inside)
            val rs: ResultSet = statement.executeQuery()
            if (rs.next()) {
                raceExist = true
            }
            rs.close()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return raceExist
    }

    private fun getPolygon(raceID: String, inside: Boolean): Polygon {
        val polygon = Polygon()
        val connection: Connection = Database.connection ?: return polygon
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

        return polygon
    }

    companion object {
        fun getRaceCreator(raceID: String): UUID? {
            var uuid: UUID? = null
            try {
                val connection: Connection = Database.connection ?: return null
                val statement = connection.prepareStatement("SELECT * FROM RaceList WHERE RaceID = ?")
                statement.setString(1, raceID)
                val rs = statement.executeQuery()
                if (rs.next()) {
                    uuid = UUID.fromString(rs.getString(2))
                }
                rs.close()
                statement.close()
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
            return uuid

        }
    }

}
