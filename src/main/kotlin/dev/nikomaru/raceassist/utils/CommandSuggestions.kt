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

package dev.nikomaru.raceassist.utils

import cloud.commandframework.annotations.suggestions.Suggestions
import cloud.commandframework.context.CommandContext
import dev.nikomaru.raceassist.database.RaceList
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

open class CommandSuggestions {

    @Suggestions("playerName")
    fun suggestPlayerName(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        Bukkit.getServer().onlinePlayers.forEach {
            list.add(it.name)
        }
        return list
    }

    @Suggestions("raceId")
    fun suggestRaceId(sender: CommandContext<CommandSender>, input: String?): List<String> {
        val list = ArrayList<String>()
        transaction {
            RaceList.selectAll().forEach {
                list.add(it[RaceList.raceID])
            }
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

}