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

@file:Suppress("BooleanMethodIsAlwaysInverted")

package dev.nikomaru.raceassist.data.files

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.RaceUtils.getRaceConfig
import dev.nikomaru.raceassist.data.files.RaceUtils.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.encodeToJsonElement
import org.bukkit.OfflinePlayer
import java.awt.Polygon
import java.io.*
import java.util.*

object RaceSettingData {

    fun existsRace(raceId: String): Boolean {
        val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
        return file.exists()
    }

    suspend fun createRace(raceId: String, owner: OfflinePlayer): Boolean = withContext(Dispatchers.IO) {
        val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.exists()) {
            return@withContext false
        }
        val raceName = raceId.split("-")[0]

        val place = Place(1, null, null, 0, false, Polygon(), Polygon())
        val bet = Bet(false, 75, null, 100)
        val raceConfig = RaceConfig(raceId, raceName, owner, arrayListOf(owner), arrayListOf(), place, bet, hashMapOf())
        val json = json.encodeToJsonElement(raceConfig)
        val string = json.toString()

        file.createNewFile()
        val fw = PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8")))
        fw.write(string)
        fw.close()

        return@withContext true
    }

    suspend fun copyRace(raceId_1: String, raceId_2: String, owner: OfflinePlayer) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId_1)
        data.raceId = raceId_2
        data.owner = owner
        data.staff = arrayListOf(owner)
        data.bet.available = false
        data.jockeys = arrayListOf()
        data.save(raceId_2)

    }

    fun deleteRace(raceId: String) {
        val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
        file.delete()
    }

    suspend fun getJockeys(raceId: String): ArrayList<OfflinePlayer> = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).jockeys
    }

    suspend fun getOwner(raceId: String): OfflinePlayer = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).owner
    }

    suspend fun getReplacement(raceId: String): HashMap<UUID, String> = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).replacement
    }

    suspend fun addJockey(raceId: String, jockey: OfflinePlayer) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.jockeys.add(jockey)
        data.save(raceId)
    }

    suspend fun setReplacement(raceId: String, uuid: UUID, name: String) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.replacement[uuid] = name
        data.save(raceId)
    }

    suspend fun removeJockey(raceId: String, jockey: OfflinePlayer) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.jockeys.remove(jockey)
        data.save(raceId)
    }

    suspend fun removeReplacement(raceId: String, uuid: UUID) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.replacement.remove(uuid)
        data.save(raceId)
    }

    suspend fun deleteReplacement(raceId: String) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.replacement.clear()
        data.save(raceId)
    }

}