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

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil.getSheetsService
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender

@CommandMethod("ra|RaceAssist bet")
class BetSheetCommand {
    @CommandPermission("raceassist.commands.bet.sheet")
    @CommandMethod("sheet <operateRaceId> <sheet>")
    @CommandDescription("現在の賭け状況を閲覧できるシートを設定します")
    suspend fun sheet(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String,
        @Argument(value = "sheet") sheetId: String
    ) {
        if (RaceAssist.api.getRaceManager(raceId)?.senderHasControlPermission(sender) != true) return
        val betManager = RaceAssist.api.getBetManager(raceId)
            ?: return sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
        val raceManager = RaceAssist.api.getRaceManager(raceId)
            ?: return sender.sendMessage(Lang.getComponent("no-exist-this-raceid-race", sender.locale()))
        val owner = raceManager.getOwner()
        betManager.setSpreadSheetId(sheetId)
        createNewSheets(sheetId, raceId)
    }

    private suspend fun createNewSheets(sheetId: String, raceId: String) = withContext(Dispatchers.IO) {
        val sheetsService = getSheetsService(sheetId) ?: return@withContext
        val content = BatchUpdateSpreadsheetRequest()
        val requests: ArrayList<Request> = ArrayList()
        val request = Request()
        val addSheet = AddSheetRequest()
        val properties = SheetProperties()
        //賭けを表示するためのシート
        properties.title = "${raceId}_RaceAssist_Bet"
        addSheet.properties = properties
        request.addSheet = addSheet
        requests.add(request)
        content.requests = requests
        sheetsService.spreadsheets()?.batchUpdate(sheetId, content)?.execute()
    }

}