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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.manager.PlaceManager
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.SuggestionId
import dev.nikomaru.raceassist.utils.Utils.displayLap
import dev.nikomaru.raceassist.utils.Utils.getRaceDegree
import dev.nikomaru.raceassist.utils.Utils.judgeLap
import dev.nikomaru.raceassist.utils.Utils.stop
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.ScoreboardManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt

@CommandMethod("ra|RaceAssist race")
class RaceDebugCommand : KoinComponent {
    val plugin: RaceAssist by inject()

    @CommandPermission("raceassist.commands.race.debug")
    @CommandMethod("debug <operateRaceId>")
    suspend fun debug(
        sender: CommandSender,
        @Argument(value = "operateRaceId", suggestions = SuggestionId.OPERATE_RACE_ID) raceId: String
    ) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val locale = sender.locale()
        plugin.launch {
            val raceManager = RaceAssist.api.getRaceManager(raceId)
            if (raceManager?.senderHasControlPermission(sender) != true) return@launch
            val placeId = raceManager.getPlaceId()
            val placeManager =
                RaceAssist.api.getPlaceManager(placeId) as PlaceManager.PlainPlaceManager? ?: return@launch
            if (!placeManager.getTrackExist()) {
                sender.sendMessage(Lang.getComponent("no-exist-race", locale))
                return@launch
            }

            val insidePolygon = placeManager.getInside()
            val outsidePolygon = placeManager.getOutside()
            if (insidePolygon.npoints < 3 || outsidePolygon.npoints < 3) {
                sender.sendMessage(Lang.getComponent("no-exist-race", locale))
                return@launch
            }
            val reverse = placeManager.getReverse()
            val lap: Int = raceManager.getLap()
            val threshold = 40
            val centralXPoint: Int =
                placeManager.getCentralPointX()
                    ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            val centralYPoint: Int =
                placeManager.getCentralPointY()
                    ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            val goalDegree: Int = placeManager.getGoalDegree()
            var beforeDegree = 0.0
            var currentLap = 0
            var counter = 0
            var passBorders = 0
            var totalDegree = 0.0
            val lengthCircle = placeManager.calculateLength()

            for (timer in 0..4) {
                val showTimer = async(Dispatchers.minecraft) {
                    sender.showTitle(
                        Title.title(
                            Lang.getComponent("${5 - timer}", locale),
                            Lang.getComponent("", locale)
                        )
                    )
                }
                delay(1000)
                showTimer.await()
            }

            sender.showTitle(
                Title.title(
                    Lang.getComponent("to-notice-start-message", locale),
                    Lang.getComponent(" ", locale)
                )
            )

            while (counter < 180 && stop[raceId] != true) {

                val nowX = sender.location.blockX
                val nowY = sender.location.blockZ
                val relativeNowX = if (!reverse) nowX - centralXPoint else -1 * (nowX - centralXPoint)
                val relativeNowY = nowY - centralYPoint
                val currentDegree = getRaceDegree(relativeNowY.toDouble(), relativeNowX.toDouble())

                val beforeLap = currentLap
                val calculateLap = async(Dispatchers.Default) {
                    currentLap += judgeLap(goalDegree, beforeDegree.toInt(), currentDegree.toInt(), threshold)
                    passBorders += judgeLap(0, beforeDegree.toInt(), currentDegree.toInt(), threshold)
                    plugin.launch {
                        async(Dispatchers.async) {
                            displayLap(currentLap, beforeLap, sender, lap)
                        }
                    }
                    beforeDegree = currentDegree
                    totalDegree = currentDegree + (passBorders * 360)
                }

                if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                    sender.sendActionBar(Lang.getComponent("outside-the-racetrack", locale))
                }

                calculateLap.await()

                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective(
                    Lang.getText("scoreboard-ranking", locale),
                    "dummy",
                    Lang.getComponent("scoreboard-context", locale)
                )

                objective.displaySlot = DisplaySlot.SIDEBAR

                val score = objective.getScore("raceId = $raceId    goalDegree = $goalDegree°")
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
                val degree = Lang.getComponent(
                    "scoreboard-now-lap-and-now-degree",
                    locale,
                    currentLap.toString(),
                    totalDegree.toString()
                )
                val displayDegree = objective.getScore(LegacyComponentSerializer.legacySection().serialize(degree))
                displayDegree.score = 2
                val residue = objective.getScore(Lang.getText("time-remaining", locale, (180 - counter).toString()))
                residue.score = 1
                sender.scoreboard = scoreboard
                counter++
                delay(1000)
            }
            delay(2000)

            sender.scoreboard.clearSlot(DisplaySlot.SIDEBAR)

        }.join()
    }


}