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

package dev.nikomaru.raceassist.horse.utlis

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.json
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.horse.data.HorseData
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.web.data.History
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Horse
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.pow

object HorseUtils {

    fun Horse.getCalcSpeed(): Double {
        return this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.value * 42.162962963
    }

    fun Horse.getCalcJump(): Double {
        return this.jumpStrength.pow(1.7) * 5.293
    }

    fun Horse.getCalcMaxHealth(): Double {
        return this.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
    }

    fun Horse.getMotherUniqueId(): UUID? {
        val uuid = this.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return null
        }
        val data = json.decodeFromString<HorseData>(file.readText())
        return data.mother
    }

    fun Horse.getFatherUniqueId(): UUID? {
        val uuid = this.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return null
        }
        val data = json.decodeFromString<HorseData>(file.readText())
        return data.father
    }

    fun Horse.getBreaderUniqueId(): UUID? {
        val uuid = this.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return null
        }
        val data = json.decodeFromString<HorseData>(file.readText())
        return data.breeder
    }

    fun Horse.getHistories(): ArrayList<History> {
        val uuid = this.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return arrayListOf()
        }
        val data = json.decodeFromString<HorseData>(file.readText())
        return data.history
    }

    fun Horse.getBirthDate(): ZonedDateTime? {
        val uuid = this.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return null
        }
        val data = json.decodeFromString<HorseData>(file.readText())
        return data.birthDate
    }

    fun Horse.getDeathDate(): ZonedDateTime? {
        val uuid = this.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return null
        }
        val data = json.decodeFromString<HorseData>(file.readText())
        return data.deathData
    }

    fun Horse.isMatchStatus(): Boolean {
        if (this.getCalcSpeed() < Config.config.recordHorse.minSpeed && this.getCalcJump() < Config.config.recordHorse.minJump) {
            return false
        }
        return true
    }

    suspend fun Horse.saveData() {
        val uuid = this.uniqueId

        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        lateinit var data: HorseData

        if (file.exists()) {
            val beforeData = json.decodeFromString<HorseData>(file.readText())
            data = beforeData.copy(
                owner = this.owner?.uniqueId,
                name = this.customName()?.toPlainText(),
                lastRecordDate = ZonedDateTime.now(),
            )
        } else {
            data = HorseData(this.uniqueId,
                null,
                this.ownerUniqueId,
                null,
                null,
                arrayListOf(),
                this.color.name,
                this.style.name,
                this.getCalcSpeed(),
                this.getCalcJump(),
                this.getCalcMaxHealth(),
                this.customName()?.toPlainText(),
                null,
                ZonedDateTime.now(),
                null)
        }
        val dataString = json.encodeToString(data)
        withContext(Dispatchers.async) {
            file.writeText(dataString)
        }

        val body: RequestBody = dataString.toRequestBody("application/json; charset=utf-8".toMediaType())
        withContext(Dispatchers.IO) {
            Config.config.resultWebhook.forEach {
                var editUrl = it.url
                if (editUrl.last() != '/') {
                    editUrl += "/"
                }
                editUrl += "v1/horse/push/"

                val request: Request =
                    Request.Builder().url(editUrl + uuid).header("Authorization", Credentials.basic(it.name, it.password)).post(body).build()
                Utils.client.newCall(request).execute().body?.close()
            }
        }
    }

    suspend fun updateKilledHorse(horse: Horse) {
        val uuid = horse.uniqueId
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("${uuid}.json")

        if (!file.exists()) {
            return
        }

        val beforeData = json.decodeFromString<HorseData>(file.readText())
        val afterData = beforeData.copy(deathData = ZonedDateTime.now())

        withContext(Dispatchers.IO) {
            val dataString = json.encodeToString(afterData)
            file.writeText(dataString)
            val body: RequestBody = dataString.toRequestBody("application/json; charset=utf-8".toMediaType())
            Config.config.resultWebhook.forEach {
                var editUrl = it.url
                if (editUrl.last() != '/') {
                    editUrl += "/"
                }
                editUrl += "v1/horse/push/"

                val request: Request =
                    Request.Builder().url(editUrl + horse.uniqueId).header("Authorization", Credentials.basic(it.name, it.password)).post(body)
                        .build()
                Utils.client.newCall(request).execute().body?.close()
            }
        }

        return

    }
}