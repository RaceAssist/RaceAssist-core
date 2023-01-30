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

package dev.nikomaru.raceassist.api.core.manager

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.Bet
import dev.nikomaru.raceassist.data.files.PlaceConfig
import dev.nikomaru.raceassist.data.files.RaceConfig
import dev.nikomaru.raceassist.data.files.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.encodeToJsonElement
import org.bukkit.OfflinePlayer
import java.awt.Polygon
import java.io.*


class DataManager {

    /**
     * 新しい競技場を作成します。
     * @param placeId 競技場のID
     * @param owner 競技場のオーナー
     */
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

    /**
     * 新しいレースを作成します。
     * @param raceId レースのID
     * @param placeId レースが行われる競技場のID
     * @param owner レースのオーナー
     */

    suspend fun createRace(raceId: String, placeId: String, owner: OfflinePlayer): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            if (file.exists()) {
                return@withContext false
            }
            val raceName = raceId.split("-")[0]

            val bet = Bet()
            val raceConfig = RaceConfig(
                raceId,
                raceName,
                placeId,
                bet,
                owner,
                arrayListOf(owner),
                arrayListOf(),
                0,
                hashMapOf(),
                hashMapOf()
            )
            val json = json.encodeToJsonElement(raceConfig)
            val string = json.toString()

            file.createNewFile()
            val fw = PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8")))
            fw.write(string)
            fw.close()

            return@withContext true
        }

    fun deleteRace(raceId: String) {
        val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
        if (file.exists()) {
            file.delete()
        }
    }
}