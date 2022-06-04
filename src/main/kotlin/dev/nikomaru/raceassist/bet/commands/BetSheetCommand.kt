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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.*
import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.api.services.sheets.v4.model.*
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil.getSheetsService
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.CommandUtils.returnRaceSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@CommandMethod("ra|RaceAssist bet")
class BetSheetCommand {
    @CommandPermission("RaceAssist.commands.bet.sheet")
    @CommandMethod("sheet <raceId> <sheet>")
    fun sheet(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String, @Argument(value = "sheet") sheetId: String) {
        plugin.launch {
            withContext(Dispatchers.IO) {
                if (returnRaceSetting(raceId, sender)) return@withContext
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceId eq raceId }) {
                    it[spreadsheetId] = sheetId
                }
            }
            createNewSheets(sheetId, raceId)
        }
    }

    private suspend fun createNewSheets(sheetId: String, raceId: String) = withContext(Dispatchers.IO) {
        val sheetsService = getSheetsService(sheetId) ?: return@withContext
        val content = BatchUpdateSpreadsheetRequest()
        val requests: ArrayList<Request> = ArrayList()
        val request1 = Request()
        val request2 = Request()
        val addSheet1 = AddSheetRequest()
        val addSheet2 = AddSheetRequest()
        val properties1 = SheetProperties()
        val properties2 = SheetProperties()
        //賭けを表示するためのシート
        properties1.title = "${raceId}_RaceAssist_Bet"
        //将来の結果のため
        properties2.title = "${raceId}_RaceAssist_Result"
        addSheet1.properties = properties1
        addSheet2.properties = properties2
        request1.addSheet = addSheet1
        request2.addSheet = addSheet2
        requests.add(request1)
        requests.add(request2)
        content.requests = requests
        sheetsService.spreadsheets()?.batchUpdate(sheetId, content)?.execute()
    }

}