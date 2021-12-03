package dev.nikomaru.raceassist.race.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Subcommand
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
class SettingPlayer : BaseCommand() {

    @Subcommand("add")
    @CommandCompletion("@RaceID @Player")
    private fun addPlayer(sender: CommandSender, raceID: String, player: Player) {

        if (SettingRace.getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか追加することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }
        if(getRacePlayerExist(raceID,player.uniqueId)){
            sender.sendMessage(Component.text("すでにそのプレイヤーは既に存在します", TextColor.color(NamedTextColor.YELLOW)))
            return
        }
        val connection: Connection = Database.connection ?: return

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

    @Subcommand("remove")
    @CommandCompletion("@RaceID @Player")
    private fun removePlayer(sender: CommandSender, raceID: String, player: Player){
        if (SettingRace.getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(Component.text("レース作成者しか削除することはできません", TextColor.color(NamedTextColor.RED)))
            return
        }
        if(!getRacePlayerExist(raceID,player.uniqueId)){
            sender.sendMessage(Component.text("すでにそのプレイヤーは既に存在しません", TextColor.color(NamedTextColor.YELLOW)))
            return
        }
        val connection: Connection = Database.connection ?: return

        try {
            val statement: PreparedStatement =
                connection.prepareStatement("DELETE FROM PlayerList WHERE RaceID = ? AND PlayerUUID = ?")
            statement.setString(1, raceID)
            statement.setString(2, player.uniqueId.toString())
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
        sender.sendMessage("${player.name} を $raceID に追加しました ")
    }


    private fun getRacePlayerExist(RaceID: String, playerUUID: UUID): Boolean {
        val connection: Connection = Database.connection!!
        var playerExist = false
        try {
            val statement = connection.prepareStatement("SELECT FROM PlayerList WHERE RaceID = ? AND PlayerUUID = ?")
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