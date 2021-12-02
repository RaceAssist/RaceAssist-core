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
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.awt.Polygon
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("race")
class SettingRace : BaseCommand() {

    @Subcommand("addPlayer")
    @CommandCompletion("@RaceID @Player")
    fun addPlayer(sender: CommandSender, raceID: String, player: Player) {
        val connection: Connection = Database.connection!!

        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(text("レース作成者しか追加することはできません", TextColor.color(RED)))
            return
        }
        if(getRacePlayerExist(raceID,player.uniqueId)){
            sender.sendMessage(text("すでにそのプレイヤーは存在します", TextColor.color(YELLOW)))
            return
        }
        try {
            val statement: PreparedStatement =
                connection.prepareStatement("INSERT INTO PlayerList(RaceID,PlayerUUID) VALUES (?,?)")
            statement.setString(1, raceID)
            statement.setString(2, player.uniqueId.toString())
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        sender.sendMessage("${player.name} を $raceID に追加しました ")
    }

    @Subcommand("start")
    @CommandCompletion("@RaceID")
    fun start(sender: CommandSender, raceID: String) {
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(text("レース作成者しか開始することはできません", TextColor.color(RED)))
        }
        if(!getCircuitExist(raceID)){
            sender.sendMessage(text("レースが存在しません", TextColor.color(YELLOW)))

            return
        }

        //TODO 5 start レース開始

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
            val statement = connection.prepareStatement("INSERT INTO RaceList(RaceID,Creator,Reverse,Lap,CentralXPoint,CentralYPoint) VALUES (?,?,?,?,?,?)")
            statement.setString(1, raceID)
            statement.setString(2, (sender as Player).uniqueId.toString())
            statement.setBoolean(3,false)
            statement.setInt(4,0)
            statement.setInt(5,0)
            statement.setInt(6,0)
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
        val connection: Connection = Database.connection!!
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

    private fun getRacePlayerExist(RaceID: String, playerUUID: UUID): Boolean {
        val connection: Connection = Database.connection!!
        var playerExist = false
        try {
            val statement = connection.prepareStatement("SELECT FROM PlayerList RaceID = ? AND PlayerUUID = ?")
            statement.setString(1, RaceID)
            statement.setString(2, playerUUID.toString())
            val rs: ResultSet = statement.executeQuery()
            while (rs.next()) {
                playerExist = true
            }
            rs.close()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return playerExist
    }

    private fun getCircuitExist(raceID: String) : Boolean {
        val connection: Connection = Database.connection!!
        var playerExist = false
        try {
            val statement = connection.prepareStatement("SELECT FROM PlayerList RaceID = ? AND PlayerUUID = ?")
            statement.setString(1, raceID)
            val rs: ResultSet = statement.executeQuery()
            while (rs.next()) {
                playerExist = true
            }
            rs.close()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return playerExist
    }

    private fun getPolygon(raceID: String, inside: Boolean): Polygon {
        val polygon = Polygon()
        val connection: Connection? = Database.connection
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
}