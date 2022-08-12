/*
 *     Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 *
 *     This program is free software: you can redistribute it and/or modify
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

import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.bukkit.CloudBukkitCapabilities
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.kotlin.coroutines.annotations.installCoroutineSupport
import cloud.commandframework.meta.SimpleCommandMeta
import cloud.commandframework.paper.PaperCommandManager
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.bet.commands.*
import dev.nikomaru.raceassist.bet.event.BetGuiClickEvent
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.horse.commands.OwnerDeleteCommand
import dev.nikomaru.raceassist.horse.events.HorseBreedEvent
import dev.nikomaru.raceassist.race.commands.HelpCommand
import dev.nikomaru.raceassist.race.commands.ReloadCommand
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
import kotlinx.serialization.ExperimentalSerializationApi
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

class RaceAssist : SuspendingJavaPlugin() {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun onEnableAsync() {
        // Plugin startup logic
        plugin = this
        Lang.load()
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
            SchemaUtils.create(BetList)
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun setCommand() {

        val commandManager: PaperCommandManager<CommandSender> = PaperCommandManager(this,
            AsynchronousCommandExecutionCoordinator.newBuilder<CommandSender>().build(),
            java.util.function.Function.identity(),
            java.util.function.Function.identity())


        if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions()
        }

        val annotationParser = AnnotationParser(commandManager, CommandSender::class.java) {
            SimpleCommandMeta.empty()
        }.installCoroutineSupport()

        with(annotationParser) {
            parse(CommandSuggestions())

            parse(AudienceJoinCommand())
            parse(AudienceLeaveCommand())
            parse(AudienceListCommand())

            parse(PlaceCentralCommand())
            parse(PlaceDegreeCommand())
            parse(PlaceFinishCommand())
            parse(PlaceLapCommand())
            parse(PlaceReverseCommand())
            parse(PlaceSetCommand())

            parse(PlayerAddCommand())
            parse(PlayerDeleteCommand())
            parse(PlayerListCommand())
            parse(PlayerRemoveCommand())
            parse(PlayerReplacementCommand())

            parse(RaceStartCommand())
            parse(RaceStopCommand())
            parse(RaceDebugCommand())

            parse(BetCanCommand())
            parse(BetDeleteCommand())
            parse(BetListCommand())
            parse(BetOpenCommand())
            parse(BetRateCommand())
            parse(BetRevertCommand())
            parse(BetSheetCommand())
            parse(BetReturnCommand())
            parse(BetUnitCommand())

            parse(SettingCreateCommand())
            parse(SettingDeleteCommand())
            parse(SettingCopyCommand())
            parse(SettingStaffCommand())
            parse(SettingViewCommand())

            parse(HelpCommand())
            parse(ReloadCommand())

            parse(OwnerDeleteCommand())
        }
    }

    private fun registerEvents() {
        server.pluginManager.registerSuspendingEvents(SetInsideCircuitEvent(), this)
        server.pluginManager.registerSuspendingEvents(SetOutsideCircuitEvent(), this)
        server.pluginManager.registerSuspendingEvents(SetCentralPointEvent(), this)
        server.pluginManager.registerSuspendingEvents(BetGuiClickEvent(), this)
        server.pluginManager.registerSuspendingEvents(HorseBreedEvent(), this)
    }

    companion object {
        lateinit var plugin: RaceAssist
            private set
    }
}


