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
import java.io.*
import java.util.*

object RaceSettingData {

    suspend fun existsRace(raceId: String) = withContext(Dispatchers.IO) {
        val file = RaceAssist.plugin.dataFolder.resolve("RaceData").resolve("$raceId.json")
        return@withContext file.exists()
    }

    suspend fun createRace(raceId: String, placeId: String, owner: OfflinePlayer): Boolean = withContext(Dispatchers.IO) {
        val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.exists()) {
            return@withContext false
        }
        val raceName = raceId.split("-")[0]

        val bet = Bet(false, 75, null, 100)
        val raceConfig = RaceConfig(raceId, raceName, owner, arrayListOf(owner), arrayListOf(), 0, placeId, bet, hashMapOf(), hashMapOf())
        val json = json.encodeToJsonElement(raceConfig)
        val string = json.toString()

        file.createNewFile()
        val fw = PrintWriter(BufferedWriter(OutputStreamWriter(FileOutputStream(file), "UTF-8")))
        fw.write(string)
        fw.close()

        return@withContext true
    }

    suspend fun copyRace(raceId_1: String, raceId_2: String, owner: OfflinePlayer) = withContext(Dispatchers.IO) {
        val beforeData = getRaceConfig(raceId_1)
        val afterBetData = beforeData.bet.copy(available = false)
        val afterData = beforeData.copy(raceId = raceId_2, owner = owner, staff = arrayListOf(owner), bet = afterBetData, jockeys = arrayListOf())
        afterData.save()
    }

    suspend fun getPlaceId(raceId: String) = withContext(Dispatchers.IO) {
        val raceConfig = getRaceConfig(raceId)
        return@withContext raceConfig.placeId
    }

    suspend fun setPlaceId(raceId: String, placeId: String) = withContext(Dispatchers.IO) {
        val beforeData = getRaceConfig(raceId)
        val afterData = beforeData.copy(placeId = placeId)
        afterData.save()
    }

    fun deleteRace(raceId: String) {
        val file = File(File(RaceAssist.plugin.dataFolder, "RaceData"), "$raceId.json")
        file.delete()
    }

    suspend fun getOwner(raceId: String): OfflinePlayer = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).owner
    }

    suspend fun getJockeys(raceId: String): ArrayList<OfflinePlayer> = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).jockeys
    }

    suspend fun addJockey(raceId: String, jockey: OfflinePlayer) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.jockeys.add(jockey)
        data.save()
    }

    suspend fun removeJockey(raceId: String, jockey: OfflinePlayer) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.jockeys.remove(jockey)
        data.save()
    }

    suspend fun getReplacement(raceId: String): HashMap<UUID, String> = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).replacement
    }

    suspend fun setReplacement(raceId: String, uuid: UUID, name: String) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.replacement[uuid] = name
        data.save()
    }

    suspend fun removeReplacement(raceId: String, uuid: UUID) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.replacement.remove(uuid)
        data.save()
    }

    suspend fun deleteReplacement(raceId: String) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.replacement.clear()
        data.save()
    }

    suspend fun getHorse(raceId: String): HashMap<UUID, UUID> = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).horse
    }

    suspend fun setHorse(raceId: String, playerUniqueId: UUID, horseUniqueId: UUID) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.horse[playerUniqueId] = horseUniqueId
        data.save()
    }

    suspend fun removeHorse(raceId: String, uuid: UUID) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.horse.remove(uuid)
        data.save()
    }

    suspend fun deleteHorse(raceId: String) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId)
        data.horse.clear()
        data.save()
    }

    suspend fun getStaffs(raceId: String) = withContext(Dispatchers.IO) {
        getRaceConfig(raceId).staff
    }

    suspend fun addStaff(raceId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        if (existStaff(raceId, player)) return@withContext false
        val data = getRaceConfig(raceId)
        data.staff.add(player)
        data.save()
        return@withContext true
    }

    suspend fun removeStaff(raceId: String, player: OfflinePlayer) = withContext(Dispatchers.IO) {
        if (getOwner(raceId) == player || !existStaff(raceId, player)) {
            return@withContext false //Owner can't be removed or staff can't be removed if they aren't in the list
        }
        val data = getRaceConfig(raceId)
        data.staff.remove(player)
        data.save()
        return@withContext true
    }

    suspend fun existStaff(raceId: String, staff: OfflinePlayer) = withContext(Dispatchers.IO) {
        return@withContext getRaceConfig(raceId).staff.contains(staff)
    }

    suspend fun getLap(raceId: String) = withContext(Dispatchers.IO) {
        return@withContext getRaceConfig(raceId).lap
    }

    suspend fun setLap(raceId: String, lap: Int) = withContext(Dispatchers.IO) {
        val data = getRaceConfig(raceId).copy(lap = lap)
        data.save()
    }

}