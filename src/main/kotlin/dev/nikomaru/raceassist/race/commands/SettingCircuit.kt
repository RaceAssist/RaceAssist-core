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
import dev.nikomaru.raceassist.race.utils.InsideCircuit
import dev.nikomaru.raceassist.race.utils.OutsideCircuit

import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("place")
class SettingCircuit : BaseCommand() {

    //TODO reverse
    @Subcommand("reverse")
    @CommandCompletion("@raceID")
    fun reverse(sender: CommandSender, raceID: String){
        val player = sender as Player
        if (SettingRace.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        val nowDirection = getDirection(raceID)
        try {
            val connection: Connection = Database.connection ?: return
            val statement = connection.prepareStatement("UPDATE RaceList SET Reverse = ? WHERE RaceID = ?")
            statement.setBoolean(1,!nowDirection)
            statement.setString(2,raceID)
            statement.execute()
        }catch (e:SQLException){
            e.printStackTrace()
        }
        sender.sendMessage(text("レースの向きを変更しました", TextColor.color(GREEN)))
    }

    @Subcommand("set")
    @CommandCompletion("@RaceID in|out")
    fun set(sender: CommandSender, raceID: String, type: String) {
        //TODO sender check
        val player = sender as Player

        if (SettingRace.getRaceCreator(raceID) == null) {
            player.sendMessage(text("レースが存在しません", TextColor.color(RED)))
            return
        } else if (SettingRace.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        if (canSetOutsideCircuit[player.uniqueId] != null || canSetInsideCircuit[player.uniqueId] != null) {
            player.sendMessage("すでに設定モードになっています")
            return
        }
        if (type == "in") {
            canSetInsideCircuit[player.uniqueId] = true
            player.sendMessage(text("内側のコース設定モードになりました", TextColor.color(GREEN)))
        } else if (type == "out") {
            val insideCircuitExist: Boolean = getInsideRaceExist(raceID)
            if (!insideCircuitExist) {
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

    private fun getDirection(raceID:String) :Boolean {
        var direction= false
        try{
            val connection: Connection = Database.connection ?: return false
            val statement = connection.prepareStatement("SELECT * FROM RaceList WHERE RaceID = ?")
            statement.setString(1, raceID)
            val rs : ResultSet = statement.executeQuery()
            if(rs.next() ){
                direction = rs.getBoolean(3)
            }
            rs.close()
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        return direction
    }

    private fun getInsideRaceExist(raceID: String): Boolean {
        var existRaceInside = false
        try {
            val connection: Connection = Database.connection ?: return false
            val statement = connection.prepareStatement(
                "SELECT * FROM CircuitPoint WHERE RaceID = ? AND Inside = true"
            )
            statement.setString(1, raceID)
            val rs = statement.executeQuery()
            if (rs.next()) {
                existRaceInside = true
            }
            rs.close()
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        return existRaceInside
    }



    companion object {
        private var canSetInsideCircuit = HashMap<UUID, Boolean>()
        private var canSetOutsideCircuit = HashMap<UUID, Boolean>()
        private var raceID = HashMap<UUID, String>()


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