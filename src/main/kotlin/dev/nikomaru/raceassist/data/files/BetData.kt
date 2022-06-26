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

package dev.nikomaru.raceassist.data.files

import dev.nikomaru.raceassist.data.files.RaceUtils.getRaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BetData {

    suspend fun getAvailable(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).bet.available
    }

    suspend fun getReturnPercent(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).bet.returnPercent
    }

    suspend fun getSpreadSheetId(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).bet.spreadSheetId
    }

    suspend fun setAvailable(raceId: String, available: Boolean) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.bet.available = available
        data.save(raceId)
    }

    suspend fun setReturnPercent(raceId: String, returnPercent: Int) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.bet.returnPercent = returnPercent
        data.save(raceId)
    }

    suspend fun setSpreadSheetId(raceId: String, spreadSheetId: String) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.bet.spreadSheetId = spreadSheetId
        data.save(raceId)
    }
}