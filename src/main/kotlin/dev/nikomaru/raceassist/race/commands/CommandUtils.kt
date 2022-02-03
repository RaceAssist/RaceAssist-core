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

package dev.nikomaru.raceassist.race.commands

import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextColor
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
import java.text.MessageFormat
import java.util.*
import kotlin.math.atan2

object CommandUtils {

    val audience: HashMap<String, ArrayList<UUID>> = HashMap()
    val canSetInsideCircuit = HashMap<UUID, Boolean>()
    val canSetOutsideCircuit = HashMap<UUID, Boolean>()
    val circuitRaceID = HashMap<UUID, String>()
    val canSetCentral = HashMap<UUID, Boolean>()
    val centralRaceID = HashMap<UUID, String>()
    var starting = false
    var stop = HashMap<String, Boolean>()

    suspend fun getRaceExist(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.count() > 0
    }

    suspend fun getDirection(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.reverse) == true
    }

    suspend fun getInsideRaceExist(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq true) }.count() > 0
    }

    suspend fun getSheetID(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetSetting.select { BetSetting.raceID eq raceID }.firstOrNull()?.get(
            BetSetting.spreadsheetId
        )
    }

    fun displayLap(currentLap: Int?, beforeLap: Int?, player: Player, lap: Int) {
        if (currentLap == null || beforeLap == null) {
            return
        }
        if (currentLap > beforeLap) {
            if (currentLap == lap - 1) {
                player.showTitle(
                    title(
                        (text(
                            Lang.getText("last-lap", player.locale()),
                            TextColor.color(GREEN)
                        )), text(
                            Lang.getText("one-step-forward-lap", player.locale()),
                            TextColor.color(BLUE)
                        )
                    )
                )
            } else {
                player.showTitle(
                    title(
                        (text(
                            MessageFormat.format(
                                Lang.getText("now-lap", player.locale()),
                                currentLap,
                                lap
                            ), TextColor.color(GREEN)
                        )), text
                            (Lang.getText("one-step-forward-lap", player.locale()), TextColor.color(BLUE))
                    )
                )
            }
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.clearTitle()
            }, 40)
        } else if (currentLap < beforeLap) {
            player.showTitle(
                title(
                    text(MessageFormat.format(Lang.getText("now-lap", player.locale()), currentLap, lap), TextColor.color(GREEN)),
                    text(Lang.getText("one-step-backwards-lap", player.locale()), TextColor.color(RED))
                )
            )
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                player.clearTitle()
            }, 40)
        }
    }

    fun judgeLap(goalDegree: Int, reverse: Boolean, beforeDegree: Int?, currentDegree: Int?, threshold: Int): Int {
        if (currentDegree == null) return 0
        when (goalDegree) {
            0 -> {
                if ((beforeDegree in 360 - threshold until 360) && (currentDegree in 0 until threshold)) {
                    return if (!reverse) 1 else -1
                }
                if ((beforeDegree in 0 until threshold) && (currentDegree in 360 - threshold until 360)) {
                    return if (!reverse) -1 else 1
                }
            }
            90 -> {

                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return if (!reverse) 1 else -1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return if (!reverse) -1 else 1
                }
            }
            180 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return if (!reverse) 1 else -1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return if (!reverse) -1 else 1
                }
            }
            270 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return if (!reverse) 1 else -1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return if (!reverse) -1 else 1
                }
            }
        }
        return 0
    }

    fun displayScoreboard(
        nowRankings: List<UUID>,
        currentLap: HashMap<UUID, Int>,
        currentDegree: HashMap<UUID, Int>,
        raceAudience: TreeSet<UUID>,
        innerCircumference: Int,
        startDegree: Int,
        lap: Int
    ) {

        raceAudience.forEach {

            if (Bukkit.getOfflinePlayer(it).isOnline) {
                val player = Bukkit.getPlayer(it)!!
                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective(
                    Lang.getText("scoreboard-ranking", player.locale()), "dummy", text(
                        Lang.getText("scoreboard-now-ranking", player.locale()), TextColor.color
                            (YELLOW)
                    )
                )
                objective.displaySlot = DisplaySlot.SIDEBAR

                for (i in nowRankings.indices) {
                    val playerName = Bukkit.getPlayer(nowRankings[i])?.name
                    val score = objective.getScore(
                        MessageFormat.format(
                            Lang.getText(
                                "scoreboard-now-ranking-and-name", player.locale()
                            ), i + 1, playerName
                        )
                    )
                    score.score = nowRankings.size * 2 - 2 * i - 1
                    val degree: String = if (currentLap[Bukkit.getPlayer(nowRankings[i])?.uniqueId] == null) {
                        Lang.getText("finished-the-race", player.locale())
                    } else {
                        MessageFormat.format(
                            Lang.getText("now-lap-and-now-length", player.locale()),
                            currentLap[Bukkit.getPlayer(nowRankings[i])?.uniqueId]?.toInt(),
                            lap,
                            (currentDegree[Bukkit.getPlayer(nowRankings[i])?.uniqueId]?.minus(startDegree))?.times(innerCircumference)?.div(360)
                        )
                    }
                    val displayDegree = objective.getScore(degree)
                    displayDegree.score = nowRankings.size * 2 - 2 * i - 2
                }
                player.scoreboard = scoreboard
            }
        }
    }

    fun getRaceDegree(Y: Int, X: Int): Int {
        return if (Math.toDegrees(atan2((Y).toDouble(), (X).toDouble())).toInt() < 0) {
            360 + Math.toDegrees(atan2((Y).toDouble(), (X).toDouble())).toInt()
        } else {
            Math.toDegrees(atan2((Y).toDouble(), (X).toDouble())).toInt()
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

    suspend fun getLapCount(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.first()[RaceList.lap]
    }

    suspend fun getAllJockeys(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        val jockeys: ArrayList<OfflinePlayer> = ArrayList()
        PlayerList.select { PlayerList.raceID eq raceID }.forEach {
            jockeys.add(Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])))
        }
        jockeys
    }

    suspend fun getGoalDegree(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.goalDegree)
    }

    suspend fun getCircuitExist(raceID: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq inside) }.count() > 0
    }

    suspend fun getPolygon(raceID: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        val polygon = Polygon()
        CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq inside) }.forEach {
            polygon.addPoint(it[CircuitPoint.XPoint], it[CircuitPoint.YPoint])
        }
        polygon
    }

    suspend fun getRaceCreator(raceID: String): UUID? {
        var raceCreator: String? = null;
        newSuspendedTransaction(Dispatchers.IO) {
            raceCreator = RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.creator)
        }
        raceCreator ?: return null
        return UUID.fromString(raceCreator)
    }

    suspend fun getCentralPoint(raceID: String, xPoint: Boolean): Int? = newSuspendedTransaction(Dispatchers.IO) {
        var point: Int? = null
        RaceList.select { RaceList.raceID eq raceID }.forEach {
            point = if (xPoint) {
                it.getOrNull(RaceList.centralXPoint)
            } else {
                it.getOrNull(RaceList.centralYPoint)
            }
        }
        point
    }

    suspend fun getReverse(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.reverse)
    }

    suspend fun getRacePlayerAmount(): Long = newSuspendedTransaction {
        PlayerList.select {
            PlayerList.raceID eq "raceID"
        }.count()
    }

    suspend fun getRacePlayerExist(RaceID: String, playerUUID: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        PlayerList.select { (PlayerList.raceID eq RaceID) and (PlayerList.playerUUID eq playerUUID.toString()) }.count() > 0
    }
}