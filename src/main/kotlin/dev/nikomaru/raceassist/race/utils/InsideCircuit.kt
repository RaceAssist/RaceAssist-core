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
package dev.nikomaru.raceassist.race.utils

import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.data.files.PlaceSettingData
import dev.nikomaru.raceassist.utils.CommandUtils.canSetInsideCircuit
import dev.nikomaru.raceassist.utils.CommandUtils.circuitRaceId
import dev.nikomaru.raceassist.utils.Lang
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.awt.Polygon

object InsideCircuit {
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

    suspend fun finish(player: Player) {
        PlaceSettingData.setInsidePolygon(circuitRaceId[player.uniqueId]!!, insidePolygonMap[circuitRaceId[player.uniqueId]]!!)
        insidePolygonMap.remove(circuitRaceId[player.uniqueId])
    }
}