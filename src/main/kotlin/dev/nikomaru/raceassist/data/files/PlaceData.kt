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
import java.awt.Polygon

object PlaceData {

    suspend fun getLap(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.lap
    }

    suspend fun getInsidePolygon(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.inside
    }

    suspend fun getOutsidePolygon(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.outside
    }

    suspend fun getCentralXPoint(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.centralX
    }

    suspend fun getCentralYPoint(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.centralY
    }

    suspend fun getGoalDegree(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.goalDegree
    }

    suspend fun getReverse(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).place.reverse
    }

    suspend fun setLap(raceId: String, lap: Int) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.lap = lap
        data.save(raceId)
    }

    suspend fun setInsidePolygon(raceId: String, polygon: Polygon) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.inside = polygon
        data.save(raceId)
    }

    suspend fun setOutsidePolygon(raceId: String, polygon: Polygon) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.outside = polygon
        data.save(raceId)
    }

    suspend fun setCentralXPoint(raceId: String, x: Int) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.centralX = x
        data.save(raceId)
    }

    suspend fun setCentralYPoint(raceId: String, y: Int) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.centralY = y
        data.save(raceId)
    }

    suspend fun setGoalDegree(raceId: String, degree: Int) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.goalDegree = degree
        data.save(raceId)
    }

    suspend fun setReverse(raceId: String, reverse: Boolean) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.place.reverse = reverse
        data.save(raceId)
    }

}