/*
 *  Copyright © 2021 Nikomaru
 *
 *  This program is free software: you can redistribute it and/or modify
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
import dev.nikomaru.raceassist.database.Database
import dev.nikomaru.raceassist.race.commands.SettingCircuit
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.awt.Polygon
import java.sql.Connection
import java.sql.SQLException

object InsideCircuit {
    private var insidePolygonMap = HashMap<String, Polygon>()
    fun insideCircuit(player: Player, RaceID: String, x: Int, z: Int) {
        insidePolygonMap.computeIfAbsent(RaceID) { Polygon() }
        insidePolygonMap[RaceID]!!.addPoint(x, z)
        player.sendActionBar(text("現在の設定位置:  X = $x, Z =$z   次の点をクリックしてください"))
        SettingCircuit.removeCanSetInsideCircuit(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(RaceAssist.plugin!!, Runnable {
            SettingCircuit.putCanSetInsideCircuit(player.uniqueId, true)
        }, 5)

    }

    fun finish(player: Player) {
        val connection: Connection = Database.connection ?: return
        try {
            val statement = connection.prepareStatement("DELETE FROM circuitPoint WHERE RaceID = ? AND Inside = ?")
            statement.setString(1, SettingCircuit.getCircuitRaceID()[player.uniqueId])
            statement.setBoolean(2, true)
            statement.execute()
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        val x = insidePolygonMap[SettingCircuit.getCircuitRaceID()[player.uniqueId]]!!.xpoints
        val y = insidePolygonMap[SettingCircuit.getCircuitRaceID()[player.uniqueId]]!!.ypoints
        val n = insidePolygonMap[SettingCircuit.getCircuitRaceID()[player.uniqueId]]!!.npoints
        for (i in 0 until n) {
            try {
                val statement =
                    connection.prepareStatement("INSERT INTO circuitPoint (RaceID,Inside,XPoint,YPoint) VALUES (?, ?, ?, ?)")
                statement.setString(1, SettingCircuit.getCircuitRaceID()[player.uniqueId])
                statement.setBoolean(2, true)
                statement.setInt(3, x[i])
                statement.setInt(4, y[i])
                statement.execute()
                statement.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        insidePolygonMap.remove(SettingCircuit.getCircuitRaceID()[player.uniqueId])
        player.sendMessage(text("設定完了しました", TextColor.color(GREEN)))
    }
}