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

package dev.nikomaru.raceassist.horse.events

import dev.nikomaru.raceassist.horse.utlis.HorseUtils.saveData
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class HorseKillEvent : Listener {
    @EventHandler
    suspend fun horseDeathEvent(event: EntityDeathEvent) {

        val entity = event.entity
        if (entity.type != EntityType.HORSE) {
            return
        }
        val horse = entity as Horse

        horse.saveData()
        return
    }
}