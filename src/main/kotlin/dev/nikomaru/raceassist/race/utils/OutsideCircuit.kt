/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.utils.CommandUtils.canSetOutsideCircuit
import dev.nikomaru.raceassist.utils.CommandUtils.circuitRaceId
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.awt.Polygon

object OutsideCircuit {
    private var outsidePolygonMap = HashMap<String, Polygon>()
    private var insidePolygonMap = HashMap<String, Polygon>()
    suspend fun outsideCircuit(player: Player, raceId: String, x: Int, z: Int) {
        outsidePolygonMap.putIfAbsent(raceId, Polygon())
        insidePolygonMap.putIfAbsent(raceId, Polygon())
        if (insidePolygonMap[raceId]!!.npoints == 0) {
            newSuspendedTransaction(Dispatchers.IO) {
                CircuitPoint.select { (CircuitPoint.raceId eq raceId) and (CircuitPoint.inside eq true) }.forEach {
                    insidePolygonMap[raceId]!!.addPoint(it[CircuitPoint.XPoint], it[CircuitPoint.YPoint])
                }
            }
        }

        if (insidePolygonMap[raceId]!!.contains(x, z)) {
            player.sendActionBar(Lang.getComponent("to-click-inside-point", player.locale()))
            return
        }
        outsidePolygonMap[raceId]!!.addPoint(x, z)
        player.sendActionBar(Lang.getComponent("to-click-next-point", player.locale(), x, z))
        canSetOutsideCircuit.remove(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            canSetOutsideCircuit[player.uniqueId] = true
        }, 5)
    }

    suspend fun finish(player: Player) {

        newSuspendedTransaction(Dispatchers.IO) {
            CircuitPoint.deleteWhere {
                (CircuitPoint.raceId eq circuitRaceId[player.uniqueId]!!) and (CircuitPoint.inside eq false)
            }
        }
        val x = outsidePolygonMap[circuitRaceId[player.uniqueId]]!!.xpoints
        val y = outsidePolygonMap[circuitRaceId[player.uniqueId]]!!.ypoints
        val n = outsidePolygonMap[circuitRaceId[player.uniqueId]]!!.npoints
        for (i in 0 until n) {
            newSuspendedTransaction(Dispatchers.IO) {
                CircuitPoint.insert {
                    it[raceId] = circuitRaceId[player.uniqueId]!!
                    it[inside] = false
                    it[XPoint] = x[i]
                    it[YPoint] = y[i]
                }
            }
        }
        outsidePolygonMap.remove(circuitRaceId[player.uniqueId])
    }
}