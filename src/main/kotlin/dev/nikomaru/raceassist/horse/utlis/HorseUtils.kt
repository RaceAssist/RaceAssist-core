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

import org.bukkit.attribute.Attribute
import org.bukkit.entity.Horse
import kotlin.math.pow

object HorseUtils {

    fun Horse.getCalcSpeed(): Double {
        return this.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.value * 42.162962963
    }

    fun Horse.getCalcJump(): Double {
        return this.jumpStrength.pow(1.7) * 5.293;
    }

    fun Horse.getCalcMaxHealth(): Double {
        return this.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
    }
}