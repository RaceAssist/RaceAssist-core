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
package dev.nikomaru.raceassist.race.utils

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.manager.PlaceManager
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils.canSetInsideCircuit
import dev.nikomaru.raceassist.utils.Utils.circuitPlaceId
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.Polygon

object InsideCircuit : KoinComponent {
    val plugin: RaceAssist by inject()
    private val insidePolygonMap = HashMap<String, Polygon>()

    fun insideCircuit(player: Player, raceId: String, x: Int, z: Int) {
        insidePolygonMap.putIfAbsent(raceId, Polygon())
        insidePolygonMap[raceId]!!.addPoint(x, z)
        player.sendActionBar(Lang.getComponent("to-click-next-point", player.locale(), x, z))
        canSetInsideCircuit.remove(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            canSetInsideCircuit[player.uniqueId] = true
        }, 5)
    }

    fun finish(player: Player) {
        val placeManager =
            RaceAssist.api.getPlaceManager(circuitPlaceId[player.uniqueId]!!) as PlaceManager.PlainPlaceManager
        placeManager.setInside(insidePolygonMap[circuitPlaceId[player.uniqueId]]!!)
        insidePolygonMap.remove(circuitPlaceId[player.uniqueId])
    }
}