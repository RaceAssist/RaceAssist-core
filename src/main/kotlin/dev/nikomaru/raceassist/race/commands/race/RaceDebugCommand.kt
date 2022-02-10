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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.race.commands.CommandUtils.displayLap
import dev.nikomaru.raceassist.race.commands.CommandUtils.getCentralPoint
import dev.nikomaru.raceassist.race.commands.CommandUtils.getCircuitExist
import dev.nikomaru.raceassist.race.commands.CommandUtils.getGoalDegree
import dev.nikomaru.raceassist.race.commands.CommandUtils.getLapCount
import dev.nikomaru.raceassist.race.commands.CommandUtils.getPolygon
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceDegree
import dev.nikomaru.raceassist.race.commands.CommandUtils.getReverse
import dev.nikomaru.raceassist.race.commands.CommandUtils.judgeLap
import dev.nikomaru.raceassist.race.commands.CommandUtils.starting
import dev.nikomaru.raceassist.race.commands.CommandUtils.stop
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.ScoreboardManager
import java.awt.Polygon
import java.text.MessageFormat
import kotlin.math.hypot
import kotlin.math.roundToInt

@CommandMethod("ra|RaceAssist race")
class RaceDebugCommand {

    @CommandPermission("RaceAssist.commands.race.debug")
    @CommandMethod("debug <raceId>")
    fun debug(player: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (starting) {
                player.sendMessage(Component.text(Lang.getText("now-starting-other-race", player.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            starting = true
            if (getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(Component.text(Lang.getText("only-race-creator-can-start", player.locale()), TextColor.color(NamedTextColor.RED)))
            }
            if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
                player.sendMessage(Component.text(Lang.getText("no-exist-race", player.locale()), TextColor.color(NamedTextColor.YELLOW)))
                return@launch
            }

            val insidePolygon = getPolygon(raceID, true)
            val outsidePolygon = getPolygon(raceID, false)
            if (insidePolygon.npoints < 3 || outsidePolygon.npoints < 3) {
                player.sendMessage(Component.text(Lang.getText("no-exist-race", player.locale()), TextColor.color(NamedTextColor.YELLOW)))
                return@launch
            }
            val reverse = getReverse(raceID) ?: false
            val lap: Int = getLapCount(raceID)
            val threshold = 40
            val centralXPoint: Int =
                getCentralPoint(raceID, true) ?: return@launch player.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                    player.locale()), TextColor.color(NamedTextColor.YELLOW)))
            val centralYPoint: Int =
                getCentralPoint(raceID, false) ?: return@launch player.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                    player.locale()), TextColor.color(NamedTextColor.YELLOW)))
            val goalDegree: Int =
                getGoalDegree(raceID) ?: return@launch player.sendMessage(Component.text(Lang.getText("no-exist-goal-degree", player.locale()),
                    TextColor.color(NamedTextColor.YELLOW)))
            var beforeDegree = 0
            var currentLap = 0
            var counter = 0
            var passBorders = 0
            var totalDegree = 0
            val lengthCircle = calcurateLength(insidePolygon)




            for (timer in 0..4) {
                val showTimer = async(Dispatchers.minecraft) {
                    player.showTitle(Title.title(Component.text("${5 - timer}", TextColor.color(NamedTextColor.GREEN)), Component.text(" ")))
                }
                delay(1000)
                showTimer.await()
            }

            player.showTitle(Title.title(Component.text(Lang.getText("to-notice-start-message", player.locale()),
                TextColor.color(NamedTextColor.GREEN)), Component.text(" ")))


            while (counter < 180 && stop[raceID] != true) {

                val nowX = player.location.blockX
                val nowY = player.location.blockZ
                val relativeNowX = if (!reverse) nowX - centralXPoint else -1 * (nowX - centralXPoint)
                val relativeNowY = nowY - centralYPoint
                val currentDegree = getRaceDegree(relativeNowY.toDouble(), relativeNowX.toDouble())

                val beforeLap = currentLap
                val calculateLap = async(Dispatchers.Default) {
                    currentLap += judgeLap(goalDegree, beforeDegree, currentDegree, threshold)
                    passBorders += judgeLap(0, beforeDegree, currentDegree, threshold)
                    displayLap(currentLap, beforeLap, player, lap)
                    beforeDegree = currentDegree
                    totalDegree = currentDegree + (passBorders * 360)
                }

                if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                    player.sendActionBar(Component.text(Lang.getText("outside-the-racetrack", player.locale()), TextColor.color(NamedTextColor.RED)))
                }

                calculateLap.await()

                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective(Lang.getText("scoreboard-ranking", player.locale()),
                    "dummy",
                    Component.text(Lang.getText("scoreboard-context", player.locale()), TextColor.color(NamedTextColor.YELLOW)))
                objective.displaySlot = DisplaySlot.SIDEBAR

                val score = objective.getScore("raceId = $raceID    goalDegree = $goalDegree°")
                score.score = 7
                val data1 = objective.getScore("relativeNowX = $relativeNowX m relativeNowY = $relativeNowY m")
                data1.score = 6
                val data2 = objective.getScore("passBorders = $passBorders times currentLap = $currentLap times")
                data2.score = 5
                val data3 = objective.getScore("totalDegree = $totalDegree° currentDegree = $currentDegree°")
                data3.score = 4
                val data4 =
                    objective.getScore("lengthCircle = ${lengthCircle.roundToInt()} m nowLength = ${(lengthCircle / 360 * totalDegree).roundToInt()} m")
                data4.score = 3
                val degree = MessageFormat.format(Lang.getText("scoreboard-now-lap-and-now-degree", player.locale()), currentLap, totalDegree)
                val displayDegree = objective.getScore(degree)
                displayDegree.score = 2
                val residue = objective.getScore(MessageFormat.format(Lang.getText("time-remaining", player.locale()), 180 - counter))
                residue.score = 1
                player.scoreboard = scoreboard
                counter++
                delay(1000)
            }
            delay(2000)

            player.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
            starting = false

        }
    }

    private fun calcurateLength(insidePolygon: Polygon): Double {
        var total = 0.0
        val insideX = insidePolygon.xpoints
        val insideY = insidePolygon.ypoints
        for (i in 0 until insidePolygon.npoints) {
            total += if (i <= insidePolygon.npoints - 2) {
                hypot((insideX[i] - insideX[i + 1]).toDouble(), (insideY[i] - insideY[i + 1]).toDouble())
            } else {
                hypot((insideX[i] - insideX[0]).toDouble(), (insideY[i] - insideY[0]).toDouble())
            }
        }
        return total
    }

}