/*
 * Copyright © 2021 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import dev.nikomaru.raceassist.database.Database
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("player")
class PlayerCommand : BaseCommand() {

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("add")
    @CommandCompletion("@RaceID @players")
    private fun addPlayer(sender: Player, raceID: String, @Single onlinePlayer: OnlinePlayer) {

        val player: Player = onlinePlayer.player
        if (RaceCommand.getRaceCreator(raceID) != sender.uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか追加することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }
        if (getRacePlayerExist(raceID, player.uniqueId)) {
            sender.sendMessage(Component.text("すでにそのプレイヤーは既に存在します", TextColor.color(NamedTextColor.YELLOW)))
            return
        }
        val connection: Connection = Database.connection ?: return

        try {
            val statement: PreparedStatement = connection.prepareStatement("INSERT INTO PlayerList(RaceID,PlayerUUID) VALUES (?,?)")
            statement.setString(1, raceID)
            statement.setString(2, player.uniqueId.toString())
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        sender.sendMessage("${player.name} を $raceID に追加しました ")
    }

    @CommandPermission("RaceAssist.commands.player")
    @Subcommand("remove")
    @CommandCompletion("@RaceID")
    private fun removePlayer(sender: CommandSender, @Single raceID: String) {
        if (RaceCommand.getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか削除することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }

        val connection: Connection = Database.connection ?: return
        try {
            val statement: PreparedStatement = connection.prepareStatement("DELETE FROM PlayerList WHERE RaceID = ?")
            statement.setString(1, raceID)
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        sender.sendMessage("$raceID から全てのプレイヤーを削除しました")
    }

    private fun getRacePlayerExist(RaceID: String, playerUUID: UUID): Boolean {
        val connection: Connection = Database.connection ?: return false
        var playerExist = false
        try {
            val statement = connection.prepareStatement("SELECT * FROM PlayerList WHERE RaceID = ? AND PlayerUUID = ?")
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
}