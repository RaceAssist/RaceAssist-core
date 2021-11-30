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
package dev.nikomaru.keibaassist.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import dev.nikomaru.keibaassist.database.Database
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*
import java.util.function.Consumer

@CommandAlias("ka|KeibaAssist")
@Subcommand("control")
class ControlHorse : BaseCommand() {
    @Default
    @Subcommand("help")
    fun help(sender: CommandSender) {
        sender.sendMessage(text("KeibaAssistのコマンド一覧", TextColor.color(YELLOW)))
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control create <グループ名> ",
                TextColor.color(0, 255, 0)
            ).append(text("グループを作成します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control help ",
                TextColor.color(WHITE)
            ).append(text("ヘルプを表示します", TextColor.color(GREEN)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control add <グループ名> ",
                TextColor.color(GREEN)
            ).append(text("グループにプレイヤーを追加します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control remove <グループ名> ",
                TextColor.color(GREEN)
            ).append(text("グループからプレイヤーを削除します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control display <グループ名> ",
                TextColor.color(GREEN)
            ).append(text("グループに入っているプレイヤーを表示します", TextColor.color(WHITE)))
        )
        sender.sendMessage(
            text(
                "/[ka|KeibaAssist] control list ",
                TextColor.color(GREEN)
            ).append(text("グループ一覧を表示します", TextColor.color(WHITE)))
        )
    }

    @Subcommand("create")
    @CommandCompletion("@GroupID")
    @Throws(SQLException::class)
    fun create(sender: CommandSender, groupID: String) {
        val player = sender as Player
        val database = Database()
        val connection: Connection = database.connection ?: return
        if (getGroupCount(groupID) != 0) {
            player.sendMessage(text("すでにそのグループは存在しています", TextColor.color(YELLOW)))
            return
        }
        if (groupID.length < 3 || groupID.length > 25) {
            player.sendMessage(text("グループ名を4~24文字の間に設定してください", TextColor.color(YELLOW)))
            return
        }
        val statement: PreparedStatement =
            connection.prepareStatement("INSERT INTO GroupList(GroupID,PlayerUUID) VALUES (?,?)")
        statement.setString(1, groupID)
        statement.setString(2, player.uniqueId.toString())
        statement.execute()
        player.sendMessage(text("グループに " + groupID + "を追加しました", TextColor.color(GREEN)))
    }

    @Subcommand("add")
    @CommandCompletion("@GroupID @players")
    fun addPlayer(sender: CommandSender, groupID: String?, player: Player) {
        val sendPlayer = sender as Player
        if (getGroupCount(groupID) == 0) {
            sendPlayer.sendMessage(text("そのグループは存在しません", TextColor.color(YELLOW)))
            return
        }
        val database = Database()
        val connection: Connection = database.connection ?: return
        var statement: PreparedStatement
        var playerNum = 0
        try {
            statement =
                connection.prepareStatement("SELECT PlayerUUID FROM PlayerList WHERE GroupID = ? AND PlayerUUID = ?")
            statement.setString(1, groupID)
            statement.setString(2, player.uniqueId.toString())
            val rs = statement.executeQuery()
            while (rs.next()) {
                playerNum++
            }
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        if (playerNum != 0) {
            sendPlayer.sendMessage(text("すでにそのプレイヤーは追加されています", TextColor.color(YELLOW)))
            return
        }
        try {
            statement = connection.prepareStatement("INSERT INTO PlayerList(GroupID,PlayerUUID) VALUES (?,?)")
            statement.setString(1, groupID)
            statement.setString(2, player.uniqueId.toString())
            statement.execute()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        sender.sendMessage(text("グループに " + player.name + "を追加しました", TextColor.color(GREEN)))
    }

    @Subcommand("remove")
    @CommandCompletion("@GroupID @Players")
    fun removePlayer(sender: CommandSender, GroupID: String?, PlayerName: String) {
        val player = sender as Player
        if (Objects.isNull(Bukkit.getOfflinePlayerIfCached(PlayerName))) {
            player.sendMessage(text("そのプレイヤーは存在しません", TextColor.color(YELLOW)))
            return
        }
        try {
            val database = Database()
            val connection: Connection = database.connection ?: return

            val statement: PreparedStatement =
                connection.prepareStatement("DELETE FROM PlayerList WHERE GroupID = ? AND PlayerUUID = ?")
            statement.setString(1, GroupID)
            statement.setString(
                2, Objects.requireNonNull(Bukkit.getOfflinePlayerIfCached(PlayerName))?.uniqueId
                    .toString()
            )
            val rs = statement.executeQuery()
            while (rs.next()) {
                player.sendMessage(text("グループから " + PlayerName + "を削除しました", TextColor.color(AQUA)))
            }
            statement.close()

        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
    }

    @Subcommand("delete")
    @CommandCompletion("@GroupID")
    fun deleteGroup(sender: CommandSender?, GroupID: String?) {
        val database = Database()
        val connection: Connection = database.connection ?: return
        var statement: PreparedStatement
        try {
            statement = connection.prepareStatement("DELETE FROM PlayerList WHERE GroupID = ?")
            statement.setString(1, GroupID)
            statement.executeUpdate()
            statement = connection.prepareStatement("DELETE FROM GroupList WHERE GroupID = ?")
            statement.setString(1, GroupID)
            statement.executeUpdate()
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
    }

    @Subcommand("display")
    @CommandCompletion("@GroupID")
    fun displayAddedPlayers(sender: CommandSender, GroupID: String?) {
        val player = sender as Player
        val addedPlayer = ArrayList<String?>()
        try {
            val database = Database()
            val connection: Connection = database.connection ?: return
            val statement: PreparedStatement = connection.prepareStatement("SELECT playerUUID FROM PlayerList")
            val rs = statement.executeQuery()
            while (rs.next()) {
                addedPlayer.add(Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("PlayerUUID"))).name)
            }
            rs.close()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        addedPlayer.forEach(Consumer { message: String? ->
            player.sendMessage(
                message!!
            )
        })
    }

    private fun getGroupCount(groupID: String?): Int {
        val database = Database()
        val connection: Connection = database.connection ?: return 0
        val statement: PreparedStatement
        var groupCount = 0
        try {
            statement = connection.prepareStatement("SELECT GroupID FROM GroupList WHERE GroupID IN(?)")
            statement.setString(1, groupID)
            val rs = statement.executeQuery()
            while (rs.next()) {
                groupCount++
            }
            rs.close()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        return groupCount
    }

    @Subcommand("List")
    fun displayList(sender: CommandSender) {
        val player = sender as Player
        val database = Database()

        val connection: Connection = database.connection ?: return
        val statement: PreparedStatement
        val groupName = ArrayList<String>()
        try {
            statement = connection.prepareStatement("SELECT GroupID FROM GroupList")
            val rs = statement.executeQuery()
            while (rs.next()) {
                groupName.add(rs.getString("GroupID"))
            }
            rs.close()
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        groupName.forEach(Consumer { message: String? ->
            player.sendMessage(
                message!!
            )
        })
    }
}