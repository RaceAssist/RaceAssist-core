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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil
import dev.nikomaru.raceassist.database.*
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.race.commands.CommandUtils.getSheetID
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@CommandMethod("ra|RaceAssist race")
class RaceDeleteCommand {
    @CommandPermission("RaceAssist.commands.race.delete")
    @CommandMethod("delete <raceId>")
    fun delete(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (getRaceCreator(raceID) == null) {
                sender.sendMessage(Lang.getText("no-racetrack-is-set", (sender as Player).locale()))
                return@launch
            }
            if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
                sender.sendMessage(Lang.getText("only-race-creator-can-setting", sender.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.deleteWhere { RaceList.raceID eq raceID }
                CircuitPoint.deleteWhere { CircuitPoint.raceID eq raceID }
                PlayerList.deleteWhere { PlayerList.raceID eq raceID }
                BetList.deleteWhere { BetList.raceID eq raceID }
                BetSetting.deleteWhere { BetSetting.raceID eq raceID }
                TempBetData.deleteWhere { TempBetData.raceID eq raceID }
                val spreadsheetId = getSheetID(raceID)
                val sheetsService = spreadsheetId?.let { SheetsServiceUtil.getSheetsService(it) } ?: return@newSuspendedTransaction

                val range = "${raceID}_RaceAssist_Bet!A1:E"
                val requestBody = ClearValuesRequest()
                val request = sheetsService.spreadsheets().values().clear("RaceAssist", range, requestBody)
                request.execute()

                RaceAssist.setRaceID()
                sender.sendMessage(Lang.getText("to-delete-race-and-so-on", sender.locale()))
            }
        }
    }
}