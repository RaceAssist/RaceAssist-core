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
package dev.nikomaru.keibaassist

import co.aikar.commands.PaperCommandManager
import dev.nikomaru.keibaassist.commands.CommandManager
import dev.nikomaru.keibaassist.commands.ControlHorse
import dev.nikomaru.keibaassist.database.Database
import dev.nikomaru.keibaassist.files.Config
import dev.nikomaru.keibaassist.race.commands.SettingCircuit
import dev.nikomaru.keibaassist.race.commands.SettingRace
import dev.nikomaru.keibaassist.race.event.SetInsideCircuitEvent
import dev.nikomaru.keibaassist.race.event.SetOutsideCircuitEvent
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.*

class KeibaAssist : JavaPlugin() {
    private var sql: Database? = null

    override fun onEnable() {
        // Plugin startup logic
        plugin = this
        val config = Config()
        config.load()
        sqlConnection()
        registerCommands()
        registerEvents()
        tabCompletion()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun tabCompletion() {
        //TODO staticにしたい
        val database = Database()
        database.initializeDatabase()
        val manager = PaperCommandManager(this)
        manager.commandCompletions.registerAsyncCompletion("AddedPlayer") {
            val groupList = ArrayList<String?>()
            try {
                val connection: Connection = database.connection!!
                val statement: PreparedStatement =
                    connection.prepareStatement("SELECT DISTINCT PlayerUUID FROM PlayerList")
                val rs = statement.executeQuery()
                while (rs.next()) {
                    groupList.add(Bukkit.getOfflinePlayer(UUID.fromString(rs.getString("PlayerUUID"))).name)
                }
                rs.close()
                statement.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
            groupList
        }
        manager.commandCompletions.registerAsyncCompletion("GroupID") {
            val groupList = ArrayList<String>()
            try {
                val connection: Connection = database.connection!!
                val statement: PreparedStatement = connection.prepareStatement("SELECT GroupID FROM GroupList")
                val rs = statement.executeQuery()
                while (rs.next()) {
                    groupList.add(rs.getString("GroupID"))
                }
                rs.close()
                statement.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
            groupList
        }
    }

    private fun registerCommands() {
        val manager = PaperCommandManager(plugin)
        manager.registerCommand(ControlHorse())
        manager.registerCommand(CommandManager())
        manager.registerCommand(SettingCircuit())
        manager.registerCommand(SettingRace())
    }

    private fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(SetInsideCircuitEvent(), plugin!!)
        Bukkit.getPluginManager().registerEvents(SetOutsideCircuitEvent(), plugin!!)
    }

    private fun sqlConnection() {
        sql = Database()
        try {
            sql!!.connect()
        } catch (e: ClassNotFoundException) {
            logger.warning("Database not connected 1")
        } catch (e: SQLException) {
            logger.warning("Database not connected 2")
        }
        if (sql!!.isConnected) {
            logger.info("Database is connected!")
        }
    }

    companion object {
        var plugin: KeibaAssist? = null

    }
}


