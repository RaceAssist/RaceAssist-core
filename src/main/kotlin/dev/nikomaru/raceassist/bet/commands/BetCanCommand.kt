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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.BetSettingData
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.i18n.LogDataType
import dev.nikomaru.raceassist.utils.i18n.bet.ChangeAvailableBetData
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetCanCommand {
    @CommandPermission("raceassist.commands.bet.can")
    @CommandMethod("can <operateRaceId> <type>")
    @CommandDescription("そのレースに対しての賭けることが可能か設定します")
    suspend fun setCanBet(sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = "operateRaceId") raceId: String,
        @Argument(value = "type", suggestions = "betType") type: String) {
        if (!RaceUtils.hasRaceControlPermission(raceId, sender)) return
        if (type == "on") {
            setCanBet(raceId, sender)
        } else if (type == "off") {
            setCannotBet(raceId, sender)
        }
    }

    private suspend fun setCanBet(raceId: String, sender: CommandSender) {
        BetSettingData.setAvailable(raceId, true)
        ChangeAvailableBetData(type = LogDataType.BET,
            raceId = raceId,
            executor = if (sender is OfflinePlayer) sender.uniqueId else null,
            available = true)
    }

    private suspend fun setCannotBet(raceId: String, sender: CommandSender) {
        BetSettingData.setAvailable(raceId, false)
        ChangeAvailableBetData(type = LogDataType.BET,
            raceId = raceId,
            executor = if (sender is OfflinePlayer) sender.uniqueId else null,
            available = false)
    }

}