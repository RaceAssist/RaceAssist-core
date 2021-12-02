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

object OutsideCircuit {
    private var outsidePolygonMap = HashMap<String, Polygon>()
    private var insidePolygonMap = HashMap<String, Polygon>()
    fun outsideCircuit(player: Player, RaceID: String, x: Int, z: Int) {
        outsidePolygonMap.computeIfAbsent(RaceID) { Polygon() }
        insidePolygonMap.computeIfAbsent(RaceID) { Polygon() }
        if (insidePolygonMap[RaceID]!!.npoints == 0) {
            try {
                val connection: Connection = Database.connection ?: return
                val statement = connection.prepareStatement(
                    "SELECT * FROM circuitPoint WHERE RaceID = ? AND Inside = ?"
                )
                statement.setString(1, RaceID)
                statement.setBoolean(2, true)
                val rs = statement.executeQuery()
                while (rs.next()) {
                    insidePolygonMap[RaceID]!!.addPoint(rs.getInt("XPoint"), rs.getInt("YPoint"))
                }
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
        }
        if (!insidePolygonMap[RaceID]!!.contains(x, z)) {
            player.sendActionBar(text("設定する点は内側に設定した物より外にしてください"))
            return
        }
        outsidePolygonMap[RaceID]!!.addPoint(x, z)
        player.sendActionBar(text("現在の設定位置:  X = $x, Z =$z   次の点をクリックしてください"))
        SettingCircuit.removeCanSetOutsideCircuit(player.uniqueId)
        Bukkit.getScheduler().runTaskLater(RaceAssist.plugin!!, Runnable {
            SettingCircuit.putCanSetOutsideCircuit(player.uniqueId, true)
        }, 5)
    }

    fun finish(player: Player) {
        val connection: Connection = Database.connection ?: return
        try {
            val statement = connection.prepareStatement(
                "DELETE FROM circuitPoint WHERE RaceID = ? AND Inside = ?"
            )
            statement.setString(1, SettingCircuit.getRaceID()[player.uniqueId])
            statement.setBoolean(2, false)
            statement.execute()
            statement.close()
        } catch (ex: SQLException) {
            ex.printStackTrace()
        }
        val x = outsidePolygonMap[SettingCircuit.getRaceID()[player.uniqueId]]!!.xpoints
        val y = outsidePolygonMap[SettingCircuit.getRaceID()[player.uniqueId]]!!.ypoints
        val n = outsidePolygonMap[SettingCircuit.getRaceID()[player.uniqueId]]!!.npoints
        for (i in 0 until n) {
            try {
                val statement = connection.prepareStatement(
                    "INSERT INTO circuitPoint (RaceID,Inside,XPoint,YPoint) VALUES (?, ?, ?, ?)"
                )
                statement.setString(1, SettingCircuit.getRaceID()[player.uniqueId])
                statement.setBoolean(2, false)
                statement.setInt(3, x[i])
                statement.setInt(4, y[i])
                statement.execute()
                statement.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        outsidePolygonMap.remove(SettingCircuit.getRaceID()[player.uniqueId])
        player.sendMessage(text("設定完了しました", TextColor.color(GREEN)))
    }
}