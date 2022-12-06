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

package dev.nikomaru.raceassist.horse.events

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.data.files.json
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.horse.data.HorseData
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcJump
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcMaxHealth
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcSpeed
import dev.nikomaru.raceassist.utils.Utils.client
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import java.time.ZonedDateTime

class HorseBreedEvent : Listener {
    @EventHandler
    suspend fun onHorseBreed(event: EntityBreedEvent) {
        if (event.entity.type != EntityType.HORSE) {
            return
        }
        val horse = event.entity as Horse

        if (horse.getCalcSpeed() < Config.config.recordHorse.minSpeed && horse.getCalcJump() < Config.config.recordHorse.minJump) {
            return
        }
        val mother = event.mother as Horse
        val father = event.father as Horse

        val data = HorseData(horse.uniqueId,
            event.breeder?.uniqueId,
            null,
            mother.uniqueId,
            father.uniqueId,
            arrayListOf(),
            horse.color.name,
            horse.style.name,
            horse.getCalcSpeed(),
            horse.getCalcJump(),
            horse.getCalcMaxHealth(),
            horse.customName()?.toPlainText(),
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            null)

        if (!RaceAssist.plugin.dataFolder.resolve("horse").exists()) {
            RaceAssist.plugin.dataFolder.resolve("horse").mkdirs()
        }

        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("${horse.uniqueId}.json")
        val dataString = json.encodeToString(data)

        withContext(Dispatchers.IO) { file.writeText(dataString) }

        val body: RequestBody = dataString.toRequestBody("application/json; charset=utf-8".toMediaType())
        withContext(Dispatchers.IO) {
            Config.config.resultWebhook.forEach {
                var editUrl = it.url
                if (editUrl.last() != '/') {
                    editUrl += "/"
                }
                editUrl += "v1/horse/push/"

                val request: Request =
                    Request.Builder().url(editUrl + horse.uniqueId).header("Authorization", Credentials.basic(it.name, it.password)).post(body)
                        .build()
                client.newCall(request).execute().body?.close()
            }
        }
    }

}

