/*
 * Copyright © 2022 Nikomaru <nikomaru@nikomaru.dev>
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
package dev.nikomaru.raceassist

import co.aikar.commands.PaperCommandManager
import com.github.shynixn.mccoroutine.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.registerSuspendingEvents
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.commands.OpenBetGuiCommand
import dev.nikomaru.raceassist.bet.commands.SetBetCommand
import dev.nikomaru.raceassist.bet.event.BetGuiClickEvent
import dev.nikomaru.raceassist.database.*
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.race.commands.AudiencesCommand
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import dev.nikomaru.raceassist.race.commands.PlayerCommand
import dev.nikomaru.raceassist.race.commands.RaceCommand
import dev.nikomaru.raceassist.race.event.SetCentralPointEvent
import dev.nikomaru.raceassist.race.event.SetInsideCircuitEvent
import dev.nikomaru.raceassist.race.event.SetOutsideCircuitEvent
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class RaceAssist : SuspendingJavaPlugin() {

    override suspend fun onEnableAsync() {
        // Plugin startup logic
        plugin = this
        Lang.load()
        saveDefaultConfig()
        Config.config = YamlConfiguration.loadConfiguration(File(dataFolder, "config.yml"))
        Config.load()
        settingDatabase()
        setRaceID()
        registerCommands()
        registerEvents()

        if (!VaultAPI.setupEconomy()) {
            plugin!!.logger.info(Lang.getText("no-economy-plugin-found-disabling-vault"))
            server.pluginManager.disablePlugin(this)
            return
        }
    }

    private fun settingDatabase() {
        org.jetbrains.exposed.sql.Database.connect(
            url = "jdbc:sqlite:${plugin!!.dataFolder}${File.separator}RaceAssist.db",
            driver = "org.sqlite.JDBC"
        )
        transaction {
            SchemaUtils.create(CircuitPoint, PlayerList, RaceList, BetList, TempBetData, BetSetting)
        }
    }

    override suspend fun onDisableAsync() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        val manager = PaperCommandManager(this)
        manager.registerCommand(PlaceCommands())
        manager.registerCommand(RaceCommand())
        manager.registerCommand(AudiencesCommand())
        manager.registerCommand(PlayerCommand())
        manager.registerCommand(OpenBetGuiCommand())
        manager.registerCommand(SetBetCommand())

        manager.commandCompletions.registerCompletion("RaceID") {
            raceID
        }

    }

    private fun registerEvents() {
        Bukkit.getPluginManager().registerEvents(SetInsideCircuitEvent(), this)
        Bukkit.getPluginManager().registerEvents(SetOutsideCircuitEvent(), this)
        Bukkit.getPluginManager().registerEvents(SetCentralPointEvent(), this)
        server.pluginManager.registerSuspendingEvents(BetGuiClickEvent(), this)
    }

    companion object {
        var plugin: RaceAssist? = null

        val raceID = mutableListOf<String>()

        fun setRaceID() {
            transaction {
                RaceList.selectAll().forEach {
                    raceID.add(it[RaceList.raceID])
                }
            }
        }
    }
}


