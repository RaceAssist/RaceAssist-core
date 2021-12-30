/*
 * Copyright © 2021 Nikomaru <nikomaru@nikomaru.dev>
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

import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Polygon

object OutsideCircuit {
    private var outsidePolygonMap = HashMap<String, Polygon>()
    private var insidePolygonMap = HashMap<String, Polygon>()
    fun outsideCircuit(player: Player, RaceID: String, x: Int, z: Int) {
        outsidePolygonMap.computeIfAbsent(RaceID) { Polygon() }
        insidePolygonMap.computeIfAbsent(RaceID) { Polygon() }
        if (insidePolygonMap[RaceID]!!.npoints == 0) {
            transaction {
                CircuitPoint.select { (CircuitPoint.raceID eq RaceID) and (CircuitPoint.inside eq true) }.forEach {
                    insidePolygonMap[RaceID]!!.addPoint(it[CircuitPoint.XPoint], it[CircuitPoint.YPoint])
                }
            }
        }

        if (insidePolygonMap[RaceID]!!.contains(x, z)) {
            player.sendActionBar(text("設定する点は内側に設定した物より外にしてください"))
            return
        }
        outsidePolygonMap[RaceID]!!.addPoint(x, z)
        player.sendActionBar(text("現在の設定位置:  X = $x, Z =$z   次の点をクリックしてください"))
        PlaceCommands.removeCanSetOutsideCircuit(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(RaceAssist.plugin!!, Runnable {
            PlaceCommands.putCanSetOutsideCircuit(player.uniqueId, true)
        }, 5)
    }

    fun finish(player: Player) {


        transaction {
            CircuitPoint.deleteWhere {
                (CircuitPoint.raceID eq PlaceCommands.getCircuitRaceID()[player.uniqueId]!!) and (CircuitPoint.inside eq
                        false)
            }
        }
        val x = outsidePolygonMap[PlaceCommands.getCircuitRaceID()[player.uniqueId]]!!.xpoints
        val y = outsidePolygonMap[PlaceCommands.getCircuitRaceID()[player.uniqueId]]!!.ypoints
        val n = outsidePolygonMap[PlaceCommands.getCircuitRaceID()[player.uniqueId]]!!.npoints
        for (i in 0 until n) {
            transaction {
                CircuitPoint.insert {
                    it[raceID] = PlaceCommands.getCircuitRaceID()[player.uniqueId]!!
                    it[inside] = false
                    it[XPoint] = x[i]
                    it[YPoint] = y[i]
                }
            }
        }
        outsidePolygonMap.remove(PlaceCommands.getCircuitRaceID()[player.uniqueId])
        player.sendMessage(text("設定完了しました", TextColor.color(GREEN)))
    }
}