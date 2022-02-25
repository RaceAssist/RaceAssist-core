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

package dev.nikomaru.raceassist.utils

import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.utils.RaceStaffUtils.existStaff
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.BLUE
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.ScoreboardManager
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.awt.Polygon
import java.util.*
import kotlin.math.atan2

object CommandUtils {

    val audience: HashMap<String, ArrayList<UUID>> = HashMap()
    val canSetInsideCircuit = HashMap<UUID, Boolean>()
    val canSetOutsideCircuit = HashMap<UUID, Boolean>()
    val circuitRaceId = HashMap<UUID, String>()
    val canSetCentral = HashMap<UUID, Boolean>()
    val centralRaceId = HashMap<UUID, String>()
    var stop = HashMap<String, Boolean>()

    suspend fun getRaceExist(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceId eq raceId }.count() > 0
    }

    suspend fun getDirection(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceId eq raceId }.firstOrNull()?.get(RaceList.reverse) == true
    }

    suspend fun getInsideRaceExist(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        CircuitPoint.select { (CircuitPoint.raceId eq raceId) and (CircuitPoint.inside eq true) }.count() > 0
    }

    fun displayLap(currentLap: Int?, beforeLap: Int?, player: Player, lap: Int) {
        if (currentLap == null || beforeLap == null) {
            return
        }
        if (currentLap > beforeLap) {
            if (currentLap == lap - 1) {
                player.showTitle(title((Lang.getComponent("last-lap", player.locale(), TextColor.color(GREEN))),
                    Lang.getComponent("one-step-forward-lap", player.locale(), TextColor.color(BLUE))))
            } else {
                player.showTitle(title(Lang.getComponent("now-lap", player.locale(), currentLap, lap),
                    Lang.getComponent("one-step-forward-lap", player.locale(), TextColor.color(BLUE))))
            }
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.clearTitle()
            }, 40)
        } else if (currentLap < beforeLap) {
            player.showTitle(title(Lang.getComponent("now-lap", player.locale(), currentLap, lap),
                Lang.getComponent("one-step-backwards-lap", player.locale())))
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.clearTitle()
            }, 40)
        }
    }

    suspend fun returnRaceSetting(raceId: String, player: Player) = withContext(Dispatchers.IO) {
        if (!raceExist(raceId)) {
            player.sendMessage(Lang.getComponent("no-exist-this-raceid-race", player.locale()))
            return@withContext true
        }
        if (!existStaff(raceId, player.uniqueId)) {
            player.sendMessage(Lang.getComponent("only-race-creator-can-setting", player.locale()))
            return@withContext true
        }
        return@withContext false
    }

    private suspend fun raceExist(raceId: String): Boolean {
        var exist = false
        newSuspendedTransaction(Dispatchers.IO) {
            exist = BetSetting.select { BetSetting.raceId eq raceId }.count() > 0
        }
        return exist
    }

    fun judgeLap(goalDegree: Int, beforeDegree: Int?, currentDegree: Int?, threshold: Int): Int {
        if (currentDegree == null) return 0
        when (goalDegree) {
            0 -> {
                if ((beforeDegree in 360 - threshold until 360) && (currentDegree in 0 until threshold)) {
                    return 1
                }
                if ((beforeDegree in 0 until threshold) && (currentDegree in 360 - threshold until 360)) {
                    return -1
                }
            }
            90 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return 1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return -1
                }
            }
            180 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return 1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return -1
                }
            }
            270 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return 1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return -1
                }
            }
        }
        return 0
    }

    fun getRaceDegree(Y: Double, X: Double): Int {
        val degree = Math.toDegrees(atan2(Y, X)).toInt()
        return if (degree < 0) {
            360 + degree
        } else {
            degree
        }
    }

    fun displayScoreboard(nowRankings: List<UUID>,
        currentLap: HashMap<UUID, Int>,
        currentDegree: HashMap<UUID, Int>,
        raceAudience: TreeSet<UUID>,
        innerCircumference: Int,
        startDegree: Int,
        lap: Int) {

        raceAudience.forEach {

            if (Bukkit.getOfflinePlayer(it).isOnline) {
                val player = Bukkit.getPlayer(it)!!
                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective(Lang.getText("scoreboard-ranking", player.locale()),
                    "dummy",
                    Lang.getComponent("scoreboard-now-ranking", player.locale()))
                objective.displaySlot = DisplaySlot.SIDEBAR

                for (i in nowRankings.indices) {
                    val playerName = Bukkit.getPlayer(nowRankings[i])?.name
                    val score = objective.getScore(LegacyComponentSerializer.legacySection()
                        .serialize(Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)))
                    score.score = nowRankings.size * 2 - 2 * i - 1
                    val degree: Component = if (currentLap[Bukkit.getPlayer(nowRankings[i])?.uniqueId] == null) {
                        Lang.getComponent("finished-the-race", player.locale())
                    } else {
                        Lang.getComponent("now-lap-and-now-length",
                            player.locale(),
                            currentLap[Bukkit.getPlayer(nowRankings[i])?.uniqueId]?.toInt(),
                            lap,
                            (currentDegree[Bukkit.getPlayer(nowRankings[i])?.uniqueId]?.minus(startDegree))?.times(innerCircumference)?.div(360))
                    }
                    val displayDegree = objective.getScore(LegacyComponentSerializer.legacySection().serialize(degree))
                    displayDegree.score = nowRankings.size * 2 - 2 * i - 2
                }
                player.scoreboard = scoreboard
            }
        }
    }

    fun decideRanking(totalDegree: HashMap<UUID, Int>): ArrayList<UUID> {
        val ranking = ArrayList<UUID>()
        val sorted = totalDegree.toList().sortedBy { (_, value) -> value }
        sorted.forEach {
            ranking.add(it.first)
        }
        ranking.reverse()
        return ranking
    }

    suspend fun getLapCount(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceId eq raceId }.first()[RaceList.lap]
    }

    suspend fun getAllJockeys(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        val jockeys: ArrayList<OfflinePlayer> = ArrayList()
        PlayerList.select { PlayerList.raceId eq raceId }.forEach {
            jockeys.add(Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])))
        }
        jockeys
    }

    suspend fun getGoalDegree(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceId eq raceId }.firstOrNull()?.get(RaceList.goalDegree)
    }

    suspend fun getCircuitExist(raceId: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        CircuitPoint.select { (CircuitPoint.raceId eq raceId) and (CircuitPoint.inside eq inside) }.count() > 0
    }

    suspend fun getPolygon(raceId: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        val polygon = Polygon()
        CircuitPoint.select { (CircuitPoint.raceId eq raceId) and (CircuitPoint.inside eq inside) }.forEach {
            polygon.addPoint(it[CircuitPoint.XPoint], it[CircuitPoint.YPoint])
        }
        polygon
    }

    suspend fun getOwner(raceId: String): UUID? {
        var raceCreator: String? = null
        newSuspendedTransaction(Dispatchers.IO) {
            raceCreator = RaceList.select { RaceList.raceId eq raceId }.firstOrNull()?.get(RaceList.creator)
        }
        raceCreator ?: return null
        return UUID.fromString(raceCreator)
    }

    suspend fun getCentralPoint(raceId: String, xPoint: Boolean): Int? = newSuspendedTransaction(Dispatchers.IO) {
        var point: Int? = null
        RaceList.select { RaceList.raceId eq raceId }.forEach {
            point = if (xPoint) {
                it.getOrNull(RaceList.centralXPoint)
            } else {
                it.getOrNull(RaceList.centralYPoint)
            }
        }
        point
    }

    suspend fun getReverse(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceId eq raceId }.firstOrNull()?.get(RaceList.reverse)
    }

    suspend fun getRacePlayerAmount(raceId: String): Long = newSuspendedTransaction {
        PlayerList.select {
            PlayerList.raceId eq raceId
        }.count()
    }

    suspend fun getRacePlayerExist(RaceId: String, playerUUID: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        PlayerList.select { (PlayerList.raceId eq RaceId) and (PlayerList.playerUUID eq playerUUID.toString()) }.count() > 0
    }
}