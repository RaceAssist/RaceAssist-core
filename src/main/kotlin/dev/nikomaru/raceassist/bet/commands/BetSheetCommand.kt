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

package dev.nikomaru.raceassist.bet.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import com.github.shynixn.mccoroutine.launch
import com.google.api.services.sheets.v4.model.AddSheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.SheetProperties
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil.getSheetsService
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@CommandMethod("ra|RaceAssist bet")
class BetSheetCommand {
    @CommandMethod("sheet <raceId> <sheet>")
    fun sheet(player: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String, @Argument(value = "sheet") sheetId: String) {
        RaceAssist.plugin.launch {
            withContext(Dispatchers.IO) {
                if (!raceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-exist-this-raceid-race", player.locale()))
                    return@withContext
                }
                if (getRaceCreator(raceID) != player.uniqueId.toString()) {
                    player.sendMessage(Lang.getText("only-race-creator-can-setting", player.locale()))
                    return@withContext
                }
            }
            newSuspendedTransaction(Dispatchers.IO) {
                BetSetting.update({ BetSetting.raceID eq raceID }) {
                    it[spreadsheetId] = sheetId
                }
            }
            createNewSheets(sheetId, raceID)
        }
    }

    private suspend fun createNewSheets(sheetId: String, raceID: String) = withContext(Dispatchers.IO) {
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
        properties1.title = "${raceID}_RaceAssist_Bet"
        //将来の結果のため
        properties2.title = "${raceID}_RaceAssist_Result"
        addSheet1.properties = properties1
        addSheet2.properties = properties2
        request1.addSheet = addSheet1
        request2.addSheet = addSheet2
        requests.add(request1)
        requests.add(request2)
        content.requests = requests
        sheetsService.spreadsheets()?.batchUpdate(sheetId, content)?.execute()
    }

    private suspend fun getRaceCreator(raceID: String) =
        newSuspendedTransaction(Dispatchers.IO) { BetSetting.select { BetSetting.raceID eq raceID }.first()[BetSetting.creator] }

    private suspend fun raceExist(raceID: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceID eq raceID }.count() > 0
        }
        return exist
    }
}