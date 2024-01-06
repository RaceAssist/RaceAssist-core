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

package dev.nikomaru.raceassist.utils

import cloud.commandframework.annotations.suggestions.Suggestions
import cloud.commandframework.context.CommandContext
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.database.BetList
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.File

open class CommandSuggestions {

    @Suggestions(SuggestionId.PLAYER_NAME)
    fun suggestPlayerName(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        Bukkit.getServer().onlinePlayers.forEach {
            list.add(it.name)
        }
        return list
    }

    @Suggestions(SuggestionId.RACE_ID)
    fun suggestRaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        File(plugin.dataFolder, "RaceData").listFiles()?.forEach {
            list.add(it.nameWithoutExtension)
        }
        return list
    }

    @Suggestions(SuggestionId.PLACE_ID)
    fun suggestPlaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        File(plugin.dataFolder, "PlaceData").listFiles()?.forEach {
            list.add(it.nameWithoutExtension)
        }
        return list
    }

    @Suggestions(SuggestionId.PLAIN_PLACE_ID)
    fun suggestPlainPlaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        File(plugin.dataFolder, "PlaceData").listFiles()?.forEach {
            val placeId = it.nameWithoutExtension
            RaceAssist.api.getPlaceType(placeId)?.let { placeType ->
                if (placeType == PlaceType.PLAIN) {
                    list.add(placeId)
                }
            }
        }
        return list
    }

    @Suggestions(SuggestionId.PLANE_VECTOR_PLACE_ID)
    fun suggestPlaneVectorPlaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        File(plugin.dataFolder, "PlaceData").listFiles()?.forEach {
            val placeId = it.nameWithoutExtension
            RaceAssist.api.getPlaceType(placeId)?.let { placeType ->
                if (placeType == PlaceType.PLANE_VECTOR) {
                    list.add(placeId)
                }
            }
        }
        return list
    }


    @Suggestions(SuggestionId.OPERATE_RACE_ID)
    fun suggestOperateRaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = runBlocking {
            val raceIds = ArrayList<String>()
            File(plugin.dataFolder, "RaceData").listFiles()?.forEach {
                val raceId = it.nameWithoutExtension
                if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender.sender) == true) {
                    raceIds.add(raceId)
                }
            }
            raceIds
        }

        return list
    }

    @Suggestions(SuggestionId.OPERATE_PLACE_ID)
    fun suggestOperatePlaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = runBlocking {
            val placeIds = ArrayList<String>()
            File(plugin.dataFolder, "PlaceData").listFiles()?.forEach {
                val placeId = it.nameWithoutExtension
                if (RaceAssist.api.getPlaceManager(placeId)?.senderHasControlPermission(sender.sender) == true) {
                    placeIds.add(placeId)
                }
            }
            placeIds
        }
        return list
    }

    @Suggestions(SuggestionId.OPERATE_PLAIN_PLACE_ID)
    fun suggestOperatePlainPlaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = runBlocking {
            val placeIds = ArrayList<String>()
            File(plugin.dataFolder, "PlaceData").listFiles()?.forEach {
                val placeId = it.nameWithoutExtension
                if (RaceAssist.api.getPlaceManager(placeId)?.senderHasControlPermission(sender.sender) == true) {
                    RaceAssist.api.getPlaceType(placeId)?.let { placeType ->
                        if (placeType == PlaceType.PLAIN) {
                            placeIds.add(placeId)
                        }
                    }
                }
            }
            placeIds
        }
        return list
    }

    @Suggestions(SuggestionId.OPERATE_PLANE_VECTOR_PLACE_ID)
    fun suggestOperatePlaneVectorPlaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = runBlocking {
            val placeIds = ArrayList<String>()
            File(plugin.dataFolder, "PlaceData").listFiles()?.forEach {
                val placeId = it.nameWithoutExtension
                if (RaceAssist.api.getPlaceManager(placeId)?.senderHasControlPermission(sender.sender) == true) {
                    RaceAssist.api.getPlaceType(placeId)?.let { placeType ->
                        if (placeType == PlaceType.PLANE_VECTOR) {
                            placeIds.add(placeId)
                        }
                    }
                }
            }
            placeIds
        }
        return list
    }

    @Suggestions("placeType")
    fun suggestPlaceType(sender: CommandContext<CommandSender>, input: String?): List<String> {
        return listOf("in", "out")
    }

    @Suggestions("betType")
    fun suggestBetType(sender: CommandContext<CommandSender>, input: String?): List<String> {
        return listOf("on", "off")
    }

    @Suggestions("betUniqueId")
    fun suggestBetUniqueId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        plugin.launch {
            newSuspendedTransaction {
                BetList.selectAll().forEach {
                    list.add(it[BetList.rowUniqueId].toString())
                }
            }
        }
        return list
    }

}




