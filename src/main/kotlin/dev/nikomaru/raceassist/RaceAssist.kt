/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
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

import cloud.commandframework.bukkit.CloudBukkitCapabilities
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.meta.SimpleCommandMeta
import cloud.commandframework.paper.PaperCommandManager
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.commands.*
import dev.nikomaru.raceassist.bet.event.BetGuiClickEvent
import dev.nikomaru.raceassist.database.*
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.race.commands.HelpCommand
import dev.nikomaru.raceassist.race.commands.audience.*
import dev.nikomaru.raceassist.race.commands.place.*
import dev.nikomaru.raceassist.race.commands.player.*
import dev.nikomaru.raceassist.race.commands.race.*
import dev.nikomaru.raceassist.race.commands.setting.*
import dev.nikomaru.raceassist.race.event.*
import dev.nikomaru.raceassist.utils.CommandSuggestions
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.jetbrains.exposed.sql.SchemaUtils
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
        setCommand()
        registerEvents()
        withContext(minecraft) {
            VaultAPI.setupEconomy()
        }
    }

    private fun settingDatabase() {
        org.jetbrains.exposed.sql.Database.connect(url = "jdbc:sqlite:${plugin.dataFolder}${File.separator}RaceAssist.db", driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(CircuitPoint, PlayerList, RaceList, BetList, BetSetting, RaceStaff)
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun setCommand() {

        val commandManager: PaperCommandManager<CommandSender> = PaperCommandManager(this,
            AsynchronousCommandExecutionCoordinator.simpleCoordinator(),
            java.util.function.Function.identity(),
            java.util.function.Function.identity())


        if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions()
        }
        val annotationParser: cloud.commandframework.annotations.AnnotationParser<CommandSender> =
            cloud.commandframework.annotations.AnnotationParser(commandManager, CommandSender::class.java) {
                SimpleCommandMeta.empty()
            }

        annotationParser.parse(CommandSuggestions())

        annotationParser.parse(AudienceJoinCommand())
        annotationParser.parse(AudienceLeaveCommand())
        annotationParser.parse(AudienceListCommand())

        annotationParser.parse(PlaceCentralCommand())
        annotationParser.parse(PlaceDegreeCommand())
        annotationParser.parse(PlaceFinishCommand())
        annotationParser.parse(PlaceLapCommand())
        annotationParser.parse(PlaceReverseCommand())
        annotationParser.parse(PlaceSetCommand())

        annotationParser.parse(PlayerAddCommand())
        annotationParser.parse(PlayerDeleteCommand())
        annotationParser.parse(PlayerListCommand())
        annotationParser.parse(PlayerRemoveCommand())

        annotationParser.parse(RaceStartCommand())
        annotationParser.parse(RaceStopCommand())
        annotationParser.parse(RaceDebugCommand())

        annotationParser.parse(BetCanCommand())
        annotationParser.parse(BetDeleteCommand())
        annotationParser.parse(BetListCommand())
        annotationParser.parse(BetOpenCommand())
        annotationParser.parse(BetRateCommand())
        annotationParser.parse(BetRevertCommand())
        annotationParser.parse(BetSheetCommand())
        annotationParser.parse(BetRemoveCommand())
        annotationParser.parse(BetReturnCommand())
        annotationParser.parse(BetTransfarCommand())

        annotationParser.parse(SettingCreateCommand())
        annotationParser.parse(SettingDeleteCommand())
        annotationParser.parse(SettingCopyCommand())
        annotationParser.parse(SettingStaffCommand())

        annotationParser.parse(HelpCommand())
    }

    private fun registerEvents() {
        server.pluginManager.registerSuspendingEvents(SetInsideCircuitEvent(), this)
        server.pluginManager.registerSuspendingEvents(SetOutsideCircuitEvent(), this)
        server.pluginManager.registerSuspendingEvents(SetCentralPointEvent(), this)
        server.pluginManager.registerSuspendingEvents(BetGuiClickEvent(), this)
    }

    companion object {
        lateinit var plugin: RaceAssist
            private set
    }
}


