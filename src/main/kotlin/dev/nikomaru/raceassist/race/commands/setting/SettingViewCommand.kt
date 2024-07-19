/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.race.commands.setting

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.SuggestionId
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.*

@CommandMethod("ra|RaceAssist setting")
class SettingViewCommand {

    @CommandMethod("view <operateRaceId>")
    @CommandPermission("raceassist.commands.setting.view")
    suspend fun view(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        val raceManager = RaceAssist.api.getRaceManager(raceId) ?: return
        if (!raceManager.senderHasControlPermission(sender)) return
        val raceData = RaceUtils.getRaceConfig(raceId)
        val placeData = RaceUtils.getPlainPlaceConfig(raceId)
        sender.sendMessage("raceId = ${raceData.raceId}")
        sender.sendMessage("raceName = ${raceData.raceName}")
        sender.sendMessage("owner = ${raceData.owner.name}")
        sender.sendMessage("staff = ${raceData.staff.joinToString { it.name.toString() }}")
        sender.sendMessage("jockeys = ${raceData.jockeys.joinToString { it.name.toString() }}")
        sender.sendMessage("replacement = ${raceData.replacement.map { it.key.toName() to it.value }.toMap()}")
        sender.sendMessage("lap = ${raceData.lap}")
        sender.sendMessage("-----place-----")
        sender.sendMessage("placeId = ${raceData.placeId}")
        sender.sendMessage("x = ${placeData.centralX}")
        sender.sendMessage("y = ${placeData.centralY}")
        sender.sendMessage("reverce = ${placeData.reverse}")
        sender.sendMessage("goalDegree = ${placeData.goalDegree}")
        sender.sendMessage("-----bet-----")
        sender.sendMessage("bet-available = ${raceData.betConfig.available}")
        sender.sendMessage("bet-returnPercent = ${raceData.betConfig.returnPercent}")
        sender.sendMessage("bet-betUnit = ${raceData.betConfig.betUnit}")

    }

    private fun UUID.toName(): String {
        return Bukkit.getOfflinePlayer(this).name.toString()
    }
}