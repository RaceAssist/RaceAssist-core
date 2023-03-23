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
import dev.nikomaru.raceassist.data.utils.json
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.horse.data.HorseData
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcJump
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcMaxHealth
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcSpeed
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.saveData
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import java.time.ZonedDateTime
import java.util.*

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

        mother.saveData()
        father.saveData()

        val data = HorseData(
            horse.uniqueId,
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
            null
        )

        if (!RaceAssist.plugin.dataFolder.resolve("horse").exists()) {
            RaceAssist.plugin.dataFolder.resolve("horse").mkdirs()
        }

        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("${horse.uniqueId}.json")
        val dataString = json.encodeToString(data)

        withContext(Dispatchers.IO) { file.writeText(dataString) }


        withContext(Dispatchers.IO) {
            Config.config.webAPI?.recordUrl?.forEach {
                var editUrl = it.url
                if (editUrl.last() != '/') {
                    editUrl += "/"
                }
                editUrl += "v1/horse/push/${horse.uniqueId}"

                Utils.client.post(editUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(data)
                    headers {
                        val token = Base64.getEncoder().encodeToString("${it.name}:${it.password}".toByteArray())
                        append("Authorization", "Basic $token")
                    }
                }
            }
        }

        val url = Config.config.webAPI?.webPage?.run {
            if (this.last() == '/') this + "horse/${horse.uniqueId}" else this + "/horse/${horse.uniqueId}"
        } ?: ""
        event.breeder?.sendRichMessage("UUID : ${horse.uniqueId} の馬のデータを記録しました <click:open_url:'$url'><yellow>[</yellow>クリックで開く<yellow>]</yellow></click>")
        event.breeder?.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.BLOCK, 1f, 1f))
    }

}

