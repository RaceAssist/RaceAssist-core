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
import dev.nikomaru.raceassist.data.files.json
import dev.nikomaru.raceassist.horse.data.HorseData
import dev.nikomaru.raceassist.utils.Utils.toUUID
import kotlinx.serialization.decodeFromString
import java.util.*

class HorseManager {

    fun recordHorses(): ArrayList<UUID> {
        val horses = arrayListOf<UUID>()
        RaceAssist.plugin.dataFolder.resolve("horse").listFiles()?.forEach {
            horses.add(it.nameWithoutExtension.toUUID())
        }
        return horses
    }

    fun getHorseData(uuid: UUID): HorseData? {
        val file = RaceAssist.plugin.dataFolder.resolve("horse").resolve("$uuid.json")
        if (!file.exists()) {
            return null
        }
        return json.decodeFromString<HorseData>(file.readText())
    }
}