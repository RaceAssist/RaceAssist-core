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

import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcJump
import dev.nikomaru.raceassist.horse.utlis.HorseUtils.getCalcSpeed
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent

class HorseBreedEvent : Listener {
    @EventHandler
    suspend fun onHorseBreed(event: EntityBreedEvent) {
        if (event.entity.type != EntityType.HORSE) {
            return
        }
        val horse = event.entity as Horse

        if (horse.getCalcSpeed() < 13.85 && horse.getCalcJump() < 4.0) {
            return
        }
        val mother = event.mother as Horse
        val father = event.father as Horse

    }

}
