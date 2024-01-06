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
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.github.shynixn.mccoroutine.bukkit.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import dev.nikomaru.raceassist.api.VaultAPI
import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.api.core.RaceAssistAPI
import dev.nikomaru.raceassist.api.core.manager.*
import dev.nikomaru.raceassist.bet.commands.*
import dev.nikomaru.raceassist.bet.event.BetGuiClickEvent
import dev.nikomaru.raceassist.data.database.BetList
import dev.nikomaru.raceassist.data.database.UserAuthData
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.horse.commands.HorseDetectCommand
import dev.nikomaru.raceassist.horse.commands.OwnerDeleteCommand
import dev.nikomaru.raceassist.horse.events.HorseBreedEvent
import dev.nikomaru.raceassist.horse.events.HorseKillEvent
import dev.nikomaru.raceassist.horse.events.HorseTamedEvent
import dev.nikomaru.raceassist.packet.event.HorsePacketSendEvent
import dev.nikomaru.raceassist.race.commands.HelpCommand
import dev.nikomaru.raceassist.race.commands.ReloadCommand
import dev.nikomaru.raceassist.race.commands.audience.AudienceJoinCommand
import dev.nikomaru.raceassist.race.commands.audience.AudienceLeaveCommand
import dev.nikomaru.raceassist.race.commands.audience.AudienceListCommand
import dev.nikomaru.raceassist.race.commands.place.*
import dev.nikomaru.raceassist.race.commands.player.*
import dev.nikomaru.raceassist.race.commands.race.RaceDebugCommand
import dev.nikomaru.raceassist.race.commands.race.RaceHorseCommand
import dev.nikomaru.raceassist.race.commands.race.RaceStartCommand
import dev.nikomaru.raceassist.race.commands.race.RaceStopCommand
import dev.nikomaru.raceassist.race.commands.setting.*
import dev.nikomaru.raceassist.race.event.SetCentralPointEvent
import dev.nikomaru.raceassist.race.event.SetInsideCircuitEvent
import dev.nikomaru.raceassist.race.event.SetOutsideCircuitEvent
import dev.nikomaru.raceassist.utils.CommandSuggestions
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.TestCommand
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.client
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import dev.nikomaru.raceassist.web.WebCommand
import dev.nikomaru.raceassist.web.api.WebAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import org.apache.commons.lang.StringUtils
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*

class RaceAssist : SuspendingJavaPlugin(), RaceAssistAPI {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun onEnableAsync() {
        // Plugin startup logic
        plugin = this
        api = this
        protocolManager = ProtocolLibrary.getProtocolManager()
        Lang.load()
        Config.load()
        settingDatabase()
        setCommand()
        loadResources()
        registerEvents()
        HorsePacketSendEvent()
        withContext(Dispatchers.minecraft) {
            VaultAPI.setupEconomy()
        }
        settingWebAPI()
    }

    fun settingWebAPI() {
        if (Config.config.webAPI != null) {
            if (Config.config.mySQL == null) {
                plugin.logger.warning("MySQLが設定されていないため、WebAPIを起動できません。")
                return
            }
            launch {
                async(Dispatchers.async) {
                    WebAPI.settingServer()
                    WebAPI.startServer()
                }
            }
        }
    }

    private suspend fun loadResources() {
        withContext(Dispatchers.IO) {
            val conf = Properties()
            conf.load(
                InputStreamReader(
                    this.javaClass.classLoader.getResourceAsStream("MapColorDefault.properties")!!,
                    "UTF-8"
                )
            )
            Utils.mapColor = conf
        }
    }

    private fun settingDatabase() {
        if (Config.config.mySQL != null) {
            Class.forName("com.mysql.cj.jdbc.Driver")
            Database.connect(
                url = "jdbc:mysql://${Config.config.mySQL!!.url}",
                driver = "com.mysql.cj.jdbc.Driver",
                user = Config.config.mySQL!!.username,
                password = Config.config.mySQL!!.password,
            )

            transaction {
                SchemaUtils.create(BetList, UserAuthData)
            }
        } else {
            Database.connect(
                url = "jdbc:sqlite:${plugin.dataFolder}${File.separator}RaceAssist.db",
                driver = "org.sqlite.JDBC"
            )

            transaction {
                SchemaUtils.create(BetList)
            }
        }

    }

    override fun onDisable() {
        // Plugin shutdown logic
        WebAPI.stopServer()
        client.close()
    }

    private fun setCommand() {

        val commandManager: PaperCommandManager<CommandSender> = PaperCommandManager(
            this,
            AsynchronousCommandExecutionCoordinator.newBuilder<CommandSender>().build(),
            java.util.function.Function.identity(),
            java.util.function.Function.identity()
        )


        if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
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
            parse(PlaceCreateCommand())
            parse(PlaceDegreeCommand())
            parse(PlaceFinishCommand())
            parse(PlaceReverseCommand())
            parse(PlaceSetCommand())
            parse(PlaceStaffCommand())

            parse(PlayerAddCommand())
            parse(PlayerDeleteCommand())
            parse(PlayerListCommand())
            parse(PlayerRemoveCommand())
            parse(PlayerReplacementCommand())

            parse(RaceStartCommand())
            parse(RaceStopCommand())
            parse(RaceDebugCommand())
            parse(RaceHorseCommand())

            parse(BetCanCommand())
            parse(BetDeleteCommand())
            parse(BetListCommand())
            parse(BetOpenCommand())
            parse(BetRateCommand())
            parse(BetRevertCommand())
            parse(BetSheetCommand())
            parse(BetPayCommand())
            parse(BetUnitCommand())

            parse(SettingCopyCommand())
            parse(SettingCreateCommand())
            parse(SettingDeleteCommand())
            parse(SettingLapCommand())
            parse(SettingPlaceIdCommand())
            parse(SettingReplacemcntCommand())
            parse(SettingStaffCommand())
            parse(SettingViewCommand())

            parse(HelpCommand())
            parse(ReloadCommand())

            parse(OwnerDeleteCommand())
            parse(HorseDetectCommand())

            parse(TestCommand())

        }
        if (Config.config.webAPI != null) {
            with(annotationParser) {
                parse(WebCommand())
            }
        }



        commandManager.command(commandManager.commandBuilder("ra-help").handler { context ->
            var string = ""
            commandManager.createCommandHelpHandler().allCommands
                .forEach { command ->
                    string += """
                    コマンド : ` ${command.syntaxString} `
                    説明 : ${command.description}
                    権限 : ${command.command.commandPermission}
                    
                """.trimIndent()
                }
            context.sender.sendRichMessage(string)
        })

        val debug = true
        if (debug) {
            val commandFile = File("C:\\Desktop\\command.txt")
            if (!commandFile.exists()) {
                commandFile.createNewFile()
            }
            var commandString = ""

            commandManager.createCommandHelpHandler().allCommands
                .forEach { command ->
                    commandString += """
                    コマンド : `${command.syntaxString}`
                    説明 : ${command.description}
                    権限 : ${command.command.commandPermission}
                    
                """.trimIndent()
                    print(commandString)
                }
            commandFile.writeText(commandString, StandardCharsets.UTF_8)
        }
    }

    private fun registerEvents() {
        server.pluginManager.registerSuspendingEvents(SetInsideCircuitEvent(), this)
        server.pluginManager.registerSuspendingEvents(SetOutsideCircuitEvent(), this)
        server.pluginManager.registerSuspendingEvents(SetCentralPointEvent(), this)
        server.pluginManager.registerSuspendingEvents(BetGuiClickEvent(), this)
        server.pluginManager.registerSuspendingEvents(HorseBreedEvent(), this)
        server.pluginManager.registerSuspendingEvents(HorseKillEvent(), this)
        server.pluginManager.registerSuspendingEvents(HorseTamedEvent(), this)
    }

    companion object {
        lateinit var plugin: RaceAssist
            private set

        lateinit var api: RaceAssistAPI
            private set

        lateinit var protocolManager: ProtocolManager
            private set

    }

    override fun getBetManager(raceId: String): BetManager? {
        if (!RaceUtils.existsRace(raceId)) return null
        return BetManager(raceId)
    }

    override fun getHorseManager(): HorseManager {
        return HorseManager()
    }


    override fun getPlaceManager(placeId: String): PlaceManager? {
        if (!RaceUtils.existsPlace(placeId)) return null
        if (RaceUtils.getPlaceType(placeId) == PlaceType.PLAIN) return PlaceManager.PlainPlaceManager(placeId)
        if (RaceUtils.getPlaceType(placeId) == PlaceType.PLANE_VECTOR) return PlaceManager.PlaneVectorPlaceManager(
            placeId
        )
        return null
    }


    override fun getRaceManager(raceId: String): RaceManager? {
        if (!RaceUtils.existsRace(raceId)) return null
        return RaceManager(raceId)
    }

    override fun getWebManager(): WebManager? {
        if (Config.config.webAPI == null) return null

        return WebManager()
    }

    override fun getDataManager(): DataManager {
        return DataManager()
    }

    override fun getPlaceType(placeId: String): PlaceType? {
        return RaceUtils.getPlaceType(placeId)
    }

}


