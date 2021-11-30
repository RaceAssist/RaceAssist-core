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
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.SQLException

@CommandAlias("ka|KeibaAssist")
@Subcommand("race")
class SettingRace : BaseCommand() {
    @Subcommand("start")
    @CommandCompletion("@RaceID")
    fun start() {
        //TODO 5 start レース開始
    }

    @Subcommand("stop")
    @CommandCompletion("@RaceID")
    fun stop() {
        //TODO 6 stop レース終了
    }

    @Subcommand("create")
    @CommandCompletion("@RaceID")
    fun create(sender: CommandSender, raceID: String?) {
        //TODO 3 delete 競馬場削除
        val database = Database()
        val connection: Connection = database.connection ?: return
        try {
            val statement = connection.prepareStatement(
                "INSERT INTO RaceList(RaceID,Creator) VALUES (?,?)"
            )
            statement.setString(1, raceID)
            statement.setString(2, (sender as Player).uniqueId.toString())
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    //TODO finalをできるだけつける
    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    fun delete(sender: CommandSender?, raceID: String?) {
        //TODO 1 create 競馬場作成
        val database = Database()
        val connection: Connection = database.connection ?: return
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
}