/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.api.core.manager

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.plugin.BetConfig
import dev.nikomaru.raceassist.data.plugin.PlainPlaceConfig
import dev.nikomaru.raceassist.data.plugin.RaceConfig
import dev.nikomaru.raceassist.data.utils.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.encodeToJsonElement
import org.bukkit.OfflinePlayer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.Polygon
import java.io.*


class DataManager : KoinComponent {
    val plugin: RaceAssist by inject()

    /**
     * 新しい競技場を作成します。
     * @param placeId 競技場のID
     * @param owner 競技場のオーナー
     */
    suspend fun createPlace(placeId: String, owner: OfflinePlayer): Boolean = withContext(Dispatchers.IO) {
        val file = File(File(plugin.dataFolder, "PlaceData"), "$placeId.json")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.exists()) {
            return@withContext false
        }

        val plainPlaceConfig =
            PlainPlaceConfig(
                PlaceType.PLAIN,
                placeId,
                placeId,
                null,
                null,
                null,
                0,
                false,
                Polygon(),
                Polygon(),
                null,
                owner,
                arrayListOf(owner)
            )
        val json = json.encodeToJsonElement(plainPlaceConfig)
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
            val file = File(File(plugin.dataFolder, "RaceData"), "$raceId.json")
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            if (file.exists()) {
                return@withContext false
            }

            val betConfig = BetConfig()
            val raceConfig = RaceConfig(
                raceId = raceId,
                raceName = raceId,
                raceImageUrl = null,
                placeId = placeId,
                betConfig = betConfig,
                owner = owner,
                staff = arrayListOf(owner),
                jockeys = arrayListOf(),
                lap = 0,
                replacement = hashMapOf(),
                horse = hashMapOf()
            )
            val jsonString = json.encodeToJsonElement(raceConfig)
            val string = jsonString.toString()

            file.createNewFile()
            val fw = PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8")))
            fw.write(string)
            fw.close()

            return@withContext true
        }

    fun deleteRace(raceId: String) {
        val file = File(File(plugin.dataFolder, "RaceData"), "$raceId.json")
        if (file.exists()) {
            file.delete()
        }
    }
}