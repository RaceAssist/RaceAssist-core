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
import dev.nikomaru.raceassist.horse.data.HorseData
import dev.nikomaru.raceassist.web.data.History
import kotlinx.serialization.decodeFromString
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Horse
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
        return data.breader
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
}