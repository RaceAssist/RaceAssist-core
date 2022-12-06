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

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.RaceUtils.getPlaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.encodeToJsonElement
import org.bukkit.OfflinePlayer
import java.awt.Polygon
import java.io.*

object PlaceSettingData {

    suspend fun existsPlace(placeId: String) = withContext(Dispatchers.IO) {
        val file = RaceAssist.plugin.dataFolder.resolve("PlaceData").resolve("$placeId.json")
        return@withContext file.exists()
    }

    suspend fun createPlace(placeId: String, owner: OfflinePlayer): Boolean = withContext(Dispatchers.IO) {
        val file = File(File(RaceAssist.plugin.dataFolder, "PlaceData"), "$placeId.json")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.exists()) {
            return@withContext false
        }

        val placeConfig = PlaceConfig(placeId, null, null, 0, false, Polygon(), Polygon(), owner, arrayListOf(owner))
        val json = json.encodeToJsonElement(placeConfig)
        val string = json.toString()

        file.createNewFile()
        val fw = PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8")))
        fw.write(string)
        fw.close()

        return@withContext true
    }

    suspend fun copyPlace(placeId_1: String, placeId_2: String, owner: OfflinePlayer) = withContext(Dispatchers.IO) {
        val beforeData = RaceUtils.getPlaceConfig(placeId_1)
        val afterData = beforeData.copy(placeId = placeId_2, owner = owner, staff = arrayListOf(owner))
        afterData.save()
    }

    fun deletePlace(placeId: String) {
        val file = File(File(RaceAssist.plugin.dataFolder, "PlaceData"), "$placeId.json")
        file.delete()
    }

    suspend fun getInsidePolygon(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).inside
    }

    suspend fun getOutsidePolygon(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).outside
    }

    suspend fun getCentralXPoint(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).centralX
    }

    suspend fun getCentralYPoint(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).centralY
    }

    suspend fun getGoalDegree(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).goalDegree
    }

    suspend fun getReverse(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).reverse
    }

    suspend fun setInsidePolygon(placeId: String, polygon: Polygon) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).copy(inside = polygon).save()
    }

    suspend fun setOutsidePolygon(placeId: String, polygon: Polygon) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).copy(outside = polygon).save()
    }

    suspend fun setCentralXPoint(placeId: String, x: Int) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).copy(centralX = x).save()
    }

    suspend fun setCentralYPoint(placeId: String, y: Int) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).copy(centralY = y).save()
    }

    suspend fun setGoalDegree(placeId: String, degree: Int) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).copy(goalDegree = degree).save()
    }

    suspend fun setReverse(placeId: String, reverse: Boolean) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).copy(reverse = reverse).save()
    }

    suspend fun getStaffs(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).staff
    }

    suspend fun addStaff(placeId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        if (existStaff(placeId, player)) return@withContext false
        val data = getPlaceConfig(placeId)
        data.staff.add(player)
        data.save()
        return@withContext true
    }

    suspend fun existStaff(placeId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).staff.contains(player)
    }

    suspend fun removeStaff(placeId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        if (getOwner(placeId) == player || !existStaff(placeId, player)) {
            return@withContext false //Owner can't be removed or staff can't be removed if they aren't in the list
        }
        val data = getPlaceConfig(placeId)
        data.staff.remove(player)
        data.save()
        return@withContext true
    }

    suspend fun getOwner(placeId: String) = withContext(Dispatchers.IO) {
        getPlaceConfig(placeId).owner
    }

}