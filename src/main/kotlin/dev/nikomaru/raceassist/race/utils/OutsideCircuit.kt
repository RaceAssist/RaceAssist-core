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
import dev.nikomaru.raceassist.utils.Utils.canSetOutsideCircuit
import dev.nikomaru.raceassist.utils.Utils.circuitPlaceId
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.Polygon

object OutsideCircuit : KoinComponent {
    val plugin: RaceAssist by inject()
    private var outsidePolygonMap = HashMap<String, Polygon>()
    private var insidePolygonMap = HashMap<String, Polygon>()
    suspend fun outsideCircuit(player: Player, placeId: String, x: Int, z: Int) {
        outsidePolygonMap.putIfAbsent(placeId, Polygon())
        val placeManager = RaceAssist.api.getPlaceManager(placeId) as PlaceManager.PlainPlaceManager
        insidePolygonMap.putIfAbsent(placeId, placeManager.getInside())

        if (insidePolygonMap[placeId]!!.contains(x, z)) {
            player.sendActionBar(Lang.getComponent("to-click-inside-point", player.locale()))
            return
        }
        outsidePolygonMap[placeId]!!.addPoint(x, z)
        player.sendActionBar(Lang.getComponent("to-click-next-point", player.locale(), x, z))
        canSetOutsideCircuit.remove(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            canSetOutsideCircuit[player.uniqueId] = true
        }, 5)
    }

    suspend fun finish(player: Player) {
        val placeManager =
            RaceAssist.api.getPlaceManager(circuitPlaceId[player.uniqueId]!!) as PlaceManager.PlainPlaceManager
        placeManager.setOutside(outsidePolygonMap[circuitPlaceId[player.uniqueId]]!!)
        outsidePolygonMap.remove(circuitPlaceId[player.uniqueId])
    }
}