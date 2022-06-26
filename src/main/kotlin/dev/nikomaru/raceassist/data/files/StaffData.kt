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
import org.bukkit.OfflinePlayer

object StaffData {

    suspend fun getStaffs(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).staff
    }

    suspend fun addStaff(raceId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        if (existStaff(raceId, player)) return@withContext false
        val data = getRaceConfig(raceId)
        data.staff.add(player)
        data.save(raceId)
        return@withContext true
    }

    suspend fun removeStaff(raceId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        if (RaceData.getOwner(raceId) == player || !existStaff(raceId, player)) {
            return@withContext false //Owner can't be removed or staff can't be removed if they aren't in the list
        }
        val data = getRaceConfig(raceId)
        data.staff.remove(player)
        data.save(raceId)
        return@withContext true
    }

    suspend fun existStaff(raceId: String, staff: OfflinePlayer) = withContext(Dispatchers.IO) {
        return@withContext getRaceConfig(raceId).staff.contains(staff)
    }
}