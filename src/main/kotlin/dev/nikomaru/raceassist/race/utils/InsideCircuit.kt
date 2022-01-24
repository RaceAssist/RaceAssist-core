/*
 * Copyright © 2022 Nikomaru <nikomaru@nikomaru.dev>
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

import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.awt.Polygon
import java.text.MessageFormat

object InsideCircuit {
    private var insidePolygonMap = HashMap<String, Polygon>()

    fun insideCircuit(player: Player, RaceID: String, x: Int, z: Int) {
        insidePolygonMap.computeIfAbsent(RaceID) { Polygon() }
        insidePolygonMap[RaceID]!!.addPoint(x, z)
        player.sendActionBar(text(MessageFormat.format(Lang.getText("to-click-next-point"), x, z)))
        PlaceCommands.removeCanSetInsideCircuit(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(plugin!!, Runnable {
            PlaceCommands.putCanSetInsideCircuit(player.uniqueId, true)
        }, 5)
    }

    fun finish(player: Player) {
        plugin!!.launch {
            newSuspendedTransaction(Dispatchers.IO) {
                CircuitPoint.deleteWhere { (CircuitPoint.raceID eq PlaceCommands.getCircuitRaceID()[player.uniqueId]!!) and (CircuitPoint.inside eq true) }
            }

            val x = insidePolygonMap[PlaceCommands.getCircuitRaceID()[player.uniqueId]]!!.xpoints
            val y = insidePolygonMap[PlaceCommands.getCircuitRaceID()[player.uniqueId]]!!.ypoints
            val n = insidePolygonMap[PlaceCommands.getCircuitRaceID()[player.uniqueId]]!!.npoints

            for (i in 0 until n) {
                newSuspendedTransaction(Dispatchers.IO) {
                    CircuitPoint.insert {
                        it[raceID] = PlaceCommands.getCircuitRaceID()[player.uniqueId]!!
                        it[inside] = true
                        it[XPoint] = x[i]
                        it[YPoint] = y[i]
                    }
                }
            }
        }
        insidePolygonMap.remove(PlaceCommands.getCircuitRaceID()[player.uniqueId])
        player.sendMessage(text("設定完了しました", TextColor.color(GREEN)))
    }
}