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

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.github.shynixn.mccoroutine.launch
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.RaceAssist.Companion.setRaceID
import dev.nikomaru.raceassist.api.sheet.SheetsServiceUtil
import dev.nikomaru.raceassist.database.*
import dev.nikomaru.raceassist.dispatch.discord.DiscordWebhook
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.RaceAudience
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.ScoreboardManager
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.awt.Polygon
import java.text.MessageFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

@CommandAlias("ra|RaceAssist")
@Subcommand("race")
class RaceCommand : BaseCommand() {

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("start")
    @CommandCompletion("@RaceID")
    fun start(sender: CommandSender, @Single raceID: String) {
        plugin!!.launch {

            if (starting) {
                sender.sendMessage(text(Lang.getText("now-starting-other-race", (sender as Player).locale()), TextColor.color(RED)))
                return@launch
            }
            if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
                sender.sendMessage(text(Lang.getText("only-race-creator-can-start", sender.locale()), TextColor.color(RED)))
                return@launch
            }
            if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
                sender.sendMessage(text(Lang.getText("no-exist-race", sender.locale()), TextColor.color(YELLOW)))
                return@launch
            }
            val jockeys: ArrayList<Player> = ArrayList()
            getAllJockeys(raceID).forEach { jockey ->
                if (jockey.isOnline) {
                    jockeys.add(jockey as Player)
                    sender.sendMessage(text(MessageFormat.format(Lang.getText("player-join", sender.locale()), jockey.name), TextColor.color(GREEN)))
                } else {
                    sender.sendMessage(
                        text(
                            MessageFormat.format(Lang.getText("player-is-offline", sender.locale()), jockey.name), TextColor.color
                                (YELLOW)
                        )
                    )
                }
            }
            if (jockeys.size < 2) {
                sender.sendMessage(text(Lang.getText("over-two-users-need", sender.locale()), TextColor.color(YELLOW)))
                return@launch
            }

            val centralXPoint: Int = getCentralPoint(raceID, true) ?: return@launch sender.sendMessage(
                text(Lang.getText("no-exist-central-point", sender.locale()), TextColor.color(YELLOW))
            )
            val centralYPoint: Int = getCentralPoint(raceID, false) ?: return@launch sender.sendMessage(
                text(
                    Lang.getText("no-exist-central-point", sender.locale()),
                    TextColor.color(YELLOW)
                )
            )
            val goalDegree: Int =
                getGoalDegree(raceID) ?: return@launch sender.sendMessage(
                    text(
                        Lang.getText("no-exist-goal-degree", sender.locale()),
                        TextColor.color(YELLOW)
                    )
                )

            if (goalDegree % 90 != 0) {
                sender.sendMessage(text(Lang.getText("goal-degree-must-multiple-90", sender.locale()), TextColor.color(YELLOW)))
                return@launch
            }

            starting = true
            val jockeyCount = jockeys.size
            val finishJockey: ArrayList<UUID> = ArrayList<UUID>()
            val totalDegree: HashMap<UUID, Int> = HashMap()
            val beforeDegree: HashMap<UUID, Int> = HashMap()
            val currentLap: HashMap<UUID, Int> = HashMap()
            val threshold = Config.threshold!!
            val raceAudience: TreeSet<UUID> = TreeSet()

            val passBorders: HashMap<UUID, Int> = HashMap()
            val time: HashMap<UUID, Int> = HashMap()

            var insidePolygon = Polygon()
            val setInsidePolygon = async(Dispatchers.IO) {
                insidePolygon = getPolygon(raceID, true)

            }

            val getLap = async(Dispatchers.IO) {
                getLapCount(raceID)
            }
            val setOutsidePolygon = async(Dispatchers.IO) {
                getPolygon(raceID, false)
            }
            val setReverse = async(Dispatchers.IO) {
                getReverse(raceID) ?: false
            }
            val calculateCircumference = async(Dispatchers.async) {
                //内周の距離のカウント
                var total = 0.0
                val insideX = insidePolygon.xpoints
                val insideY = insidePolygon.ypoints
                setInsidePolygon.await()
                for (i in 0 until insidePolygon.npoints) {
                    total += if (i <= insidePolygon.npoints - 2) {
                        hypot((insideX[i] - insideX[i + 1]).toDouble(), (insideY[i] - insideY[i + 1]).toDouble())
                    } else {
                        hypot((insideX[i] - insideX[0]).toDouble(), (insideY[i] - insideY[0]).toDouble())
                    }
                }
                total
            }

            //観客(スコアボードを表示する人)の設定

            val audiences = RaceAudience()


            AudiencesCommand.audience[raceID]?.forEach {
                audiences.add(Bukkit.getOfflinePlayer(it))
            }

            jockeys.forEach {
                audiences.add(it)
            }
            audiences.add(sender)

            audiences.getUUID().forEach {
                raceAudience.add(it)
            }

            //5.4.3...1 のカウント
            var timer1 = 0
            while (timer1 <= 4) {
                val sendTimer = async(Dispatchers.minecraft) {
                    audiences.getUUID().forEach {
                        if (Bukkit.getOfflinePlayer(it).isOnline) {
                            Bukkit.getPlayer(it)?.showTitle(title(text("${5 - timer1}", TextColor.color(GREEN)), text(" ")))
                        }
                    }

                }
                delay(1000)
                sendTimer.await()
                timer1++
            }

            val lap = getLap.await()
            val outsidePolygon = setOutsidePolygon.await()
            val reverse = setReverse.await()
            val innerCircumference = calculateCircumference.await()

            jockeys.forEach {
                beforeDegree[it.uniqueId] = getRaceDegree(
                    if (!reverse) it.location.blockX - centralXPoint
                    else -(it.location.blockX - centralXPoint), it.location.blockZ - centralYPoint
                )
                currentLap[it.uniqueId] = 0
                passBorders[it.uniqueId] = 0
            }

            val beforeTime = System.currentTimeMillis()
            audiences.showTitleI18n("race-start")

            val randomJockey = jockeys.random()
            val startDegree = getRaceDegree(
                randomJockey.location.blockZ - centralYPoint, if (reverse) {
                    -(randomJockey.location.blockX - centralXPoint)
                } else {
                    randomJockey.location.blockX - centralXPoint
                }
            )

            //レースの処理
            while (jockeys.size >= 1 && stop[raceID] != true) {

                val iterator = jockeys.iterator()
                while (iterator.hasNext()) {
                    val player: Player = iterator.next()
                    if (!player.isOnline) {
                        iterator.remove()
                        continue
                    }

                    val nowX: Int = player.location.blockX
                    val nowY = player.location.blockZ
                    val relativeNowX = if (!reverse) nowX - centralXPoint else -(nowX - centralXPoint)
                    val relativeNowY = nowY - centralYPoint
                    val currentDegree = getRaceDegree(relativeNowY, relativeNowX)
                    val uuid = player.uniqueId

                    val beforeLap = currentLap[uuid]
                    val calculateLap = async(Dispatchers.Default) {
                        currentLap[uuid] = currentLap[uuid]!! + judgeLap(goalDegree, reverse, beforeDegree[uuid], currentDegree, threshold)
                        passBorders[uuid] = passBorders[uuid]!! + judgeLap(0, reverse, beforeDegree[uuid], currentDegree, threshold)
                        displayLap(currentLap[uuid], beforeLap, player, lap)
                        beforeDegree[uuid] = currentDegree
                        totalDegree[uuid] = currentDegree + (passBorders[uuid]!! * 360)
                    }

                    if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                        player.sendActionBar(text(Lang.getText("outside-the-racetrack", player.locale()), TextColor.color(RED)))
                    }

                    calculateLap.await()

                    if (currentLap[uuid]!! >= lap) {
                        iterator.remove()
                        finishJockey.add(uuid)
                        totalDegree.remove(uuid)
                        currentLap.remove(uuid)
                        player.showTitle(
                            title(
                                text(
                                    MessageFormat.format(Lang.getText("player-ranking", player.locale()), jockeyCount - jockeys.size, jockeyCount),
                                    TextColor.color(GREEN)
                                ), text("")
                            )
                        )
                        time[uuid] = ((System.currentTimeMillis() - beforeTime) / 1000).toInt()
                        continue
                    }
                }
                val displayRanking = async(Dispatchers.minecraft) {

                    displayScoreboard(
                        finishJockey.plus(decideRanking(totalDegree)),
                        currentLap,
                        totalDegree,
                        raceAudience,
                        innerCircumference.roundToInt(),
                        startDegree,
                        lap
                    )
                }
                delay(100)
                displayRanking.await()
            }
            audiences.showTitleI18n("finish-race")
            delay(2000)

            for (i in 0 until finishJockey.size) {
                audiences.getUUID().forEach {
                    if (Bukkit.getOfflinePlayer(it).isOnline) {
                        Bukkit.getPlayer(it)?.sendMessage(
                            MessageFormat.format(
                                Lang.getText("to-notice-ranking-message", Bukkit.getPlayer(it)!!.locale()),
                                i + 1, Bukkit.getPlayer(finishJockey[i])?.name!!
                            )
                        )
                    }
                }
            }

            withContext(Dispatchers.IO) {
                if (Config.discordWebHook != null) {
                    sendWebHook(finishJockey, time, sender.uniqueId, raceID)
                }
            }
            Bukkit.getOnlinePlayers().forEach {
                it.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
            }
            starting = false

        }
    }

    private fun sendWebHook(finishJockey: ArrayList<UUID>, time: HashMap<UUID, Int>, starter: UUID, raceID: String) {
        val json = JSONObject()
        json["username"] = "RaceAssist"
        json["avatar_url"] = "https://3.bp.blogspot.com/-Y3AVYVjLcPs/UYiNxIliDxI/AAAAAAAARSg/nZLIqBRUta8/s800/animal_uma.png"
        val result = JSONArray()
        val embeds = JSONArray()
        val author = JSONObject()
        val embedsObject = JSONObject()
        embedsObject["title"] = Lang.getText("discord-webhook-race-result", Locale.getDefault())
        author["name"] =
            MessageFormat.format(Lang.getText("discord-webhook-name", Locale.getDefault()), Bukkit.getOfflinePlayer(starter).name, raceID)
        author["icon_url"] = "https://crafthead.net/avatar/$starter"
        embedsObject["author"] = author
        for (i in 0 until finishJockey.size) {
            val playerResult = JSONObject()
            playerResult["name"] = MessageFormat.format(Lang.getText("discord-webhook-ranking", Locale.getDefault()), i + 1)
            playerResult["value"] = String.format(
                "%s %2d:%02d",
                Bukkit.getPlayer(finishJockey[i])?.name,
                floor((time[finishJockey[i]]!!.toDouble() / 60)).toInt(),
                time[finishJockey[i]]!! % 60
            )
            playerResult["inline"] = true
            result.add(playerResult)
        }
        embedsObject["fields"] = result
        embeds.add(embedsObject)
        json["embeds"] = embeds

        plugin!!.logger.info(json.toString())
        val discordWebHook = DiscordWebhook()
        discordWebHook.sendWebHook(json.toString())
    }

    private fun getRaceDegree(Y: Int, X: Int): Int {
        return if (Math.toDegrees(atan2((Y).toDouble(), (X).toDouble())).toInt() < 0) {
            360 + Math.toDegrees(atan2((Y).toDouble(), (X).toDouble())).toInt()
        } else {
            Math.toDegrees(atan2((Y).toDouble(), (X).toDouble())).toInt()
        }
    }

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("debug")
    @CommandCompletion("@RaceID")
    fun debug(sender: CommandSender, @Single raceID: String) {
        plugin?.launch {
            if (starting) {
                sender.sendMessage(text(Lang.getText("now-starting-other-race", (sender as Player).locale()), TextColor.color(RED)))
                return@launch
            }
            starting = true
            if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
                sender.sendMessage(text(Lang.getText("only-race-creator-can-start", sender.locale()), TextColor.color(RED)))
            }
            if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
                sender.sendMessage(text(Lang.getText("no-exist-race", sender.locale()), TextColor.color(YELLOW)))
                return@launch
            }

            val insidePolygon = getPolygon(raceID, true)
            val outsidePolygon = getPolygon(raceID, false)
            if (insidePolygon.npoints < 3 || outsidePolygon.npoints < 3) {
                sender.sendMessage(text(Lang.getText("no-exist-race", sender.locale()), TextColor.color(YELLOW)))
                return@launch
            }
            val reverse = getReverse(raceID) ?: false
            val lap: Int = getLapCount(raceID)
            val threshold = 40
            val centralXPoint: Int =
                getCentralPoint(raceID, true) ?: return@launch sender.sendMessage(
                    text(
                        Lang.getText("no-exist-central-point", sender.locale()), TextColor.color
                            (YELLOW)
                    )
                )
            val centralYPoint: Int =
                getCentralPoint(raceID, false) ?: return@launch sender.sendMessage(
                    text(
                        Lang.getText("no-exist-central-point", sender.locale()),
                        TextColor.color(YELLOW)
                    )
                )
            val goalDegree: Int =
                getGoalDegree(raceID) ?: return@launch sender.sendMessage(
                    text(
                        Lang.getText("no-exist-goal-degree", sender.locale()),
                        TextColor.color(YELLOW)
                    )
                )
            var beforeDegree = 0
            var currentLap = 0
            var counter = 0



            withContext(Dispatchers.minecraft) {
                for (timer in 0..4) {
                    sender.showTitle(title(text("${5 - timer}", TextColor.color(GREEN)), text(" ")))
                    delay(1000)
                }
            }

            sender.showTitle(title(text(Lang.getText("to-notice-start-message", sender.locale()), TextColor.color(GREEN)), text(" ")))
            val it: Player = sender


            while (counter < 180 && stop[raceID] != true) {

                val nowX: Int = it.location.blockX
                val nowY: Int = it.location.blockZ
                var relativeNowX = nowX - centralXPoint
                val relativeNowY = nowY - centralYPoint
                if (reverse) {
                    relativeNowX = -relativeNowX
                }
                val currentDegree = getRaceDegree(relativeNowY, relativeNowX)
                val beforeLap = currentLap

                currentLap += judgeLap(goalDegree, reverse, currentDegree, beforeDegree, threshold)

                displayLap(currentLap, beforeLap, it, lap)

                if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                    it.sendActionBar(text(Lang.getText("outside-the-racetrack", sender.locale()), TextColor.color(RED)))
                }
                beforeDegree = currentDegree

                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective(
                    Lang.getText("scoreboard-ranking", sender.locale()),
                    "dummy",
                    text(Lang.getText("scoreboard-context", sender.locale()), TextColor.color(YELLOW))
                )
                objective.displaySlot = DisplaySlot.SIDEBAR

                val score = objective.getScore(Lang.getText("first-ranking", sender.locale()) + "   " + "§b${sender.name}")
                score.score = 4
                val degree = MessageFormat.format(Lang.getText("scoreboard-now-lap-and-now-degree", sender.locale()), currentLap, currentDegree)

                val displayDegree = objective.getScore(degree)
                displayDegree.score = 2
                val residue = objective.getScore(MessageFormat.format(Lang.getText("time-remaining", sender.locale()), 180 - counter))
                residue.score = 1
                sender.scoreboard = scoreboard
                counter++
                delay(1000)
            }
            delay(2000)

            sender.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
            starting = false

        }
    }

    private fun judgeLap(goalDegree: Int, reverse: Boolean, beforeDegree: Int?, currentDegree: Int?, threshold: Int): Int {
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

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("stop")
    @CommandCompletion("@RaceID")
    fun stop(sender: CommandSender, @Single raceID: String) {
        plugin?.launch {
            if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
                sender.sendMessage(text(Lang.getText("only-race-creator-can-stop", sender.locale()), TextColor.color(RED)))
            }
            stop[raceID] = true


            delay(1000)
            stop[raceID] = false
        }
    }

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("create")
    fun create(sender: CommandSender, @Single raceID: String) {
        plugin!!.launch {
            if (getRaceCreator(raceID) != null) {
                sender.sendMessage(Lang.getText("already-used-the-name-race", (sender as Player).locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.insert {
                    it[this.raceID] = raceID
                    it[this.creator] = (sender as Player).uniqueId.toString()
                    it[this.reverse] = false
                    it[this.lap] = 1
                    it[this.centralXPoint] = null
                    it[this.centralYPoint] = null
                    it[this.goalDegree] = null
                }
                BetSetting.insert {
                    it[this.raceID] = raceID
                    it[this.canBet] = false
                    it[this.returnPercent] = 75
                    it[this.creator] = (sender as Player).uniqueId.toString()
                    it[this.spreadsheetId] = null
                }
            }
            setRaceID()
            sender.sendMessage(Lang.getText("to-create-race", (sender as Player).locale()))
        }
    }

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    fun delete(sender: CommandSender, @Single raceID: String) {
        plugin!!.launch {
            if (getRaceCreator(raceID) == null) {
                sender.sendMessage(Lang.getText("no-racetrack-is-set", (sender as Player).locale()))
                return@launch
            }
            if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
                sender.sendMessage(Lang.getText("only-race-creator-can-setting", sender.locale()))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.deleteWhere { RaceList.raceID eq raceID }
                CircuitPoint.deleteWhere { CircuitPoint.raceID eq raceID }
                PlayerList.deleteWhere { PlayerList.raceID eq raceID }
                BetList.deleteWhere { BetList.raceID eq raceID }
                BetSetting.deleteWhere { BetSetting.raceID eq raceID }
                TempBetData.deleteWhere { TempBetData.raceID eq raceID }
                val spreadsheetId = getSheetID(raceID)
                if (spreadsheetId != null) {
                    val sheetsService = SheetsServiceUtil.getSheetsService(spreadsheetId)

                    val range = "${raceID}_RaceAssist!A1:E"
                    val requestBody = ClearValuesRequest()
                    val request = sheetsService!!.spreadsheets().values().clear("RaceAssist", range, requestBody)
                    request.execute()

                }
                setRaceID()
                sender.sendMessage(Lang.getText("to-delete-race-and-so-on", sender.locale()))
            }
        }
    }

    private suspend fun getSheetID(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        BetSetting.select { BetSetting.raceID eq raceID }.firstOrNull()?.get(BetSetting.spreadsheetId)
    }

    private fun displayLap(currentLap: Int?, beforeLap: Int?, player: Player, lap: Int) {
        if (currentLap == null || beforeLap == null) {
            return
        }
        if (currentLap > beforeLap) {
            if (currentLap == lap - 1) {
                player.showTitle(
                    title(
                        (text(Lang.getText("last-lap", player.locale()), TextColor.color(GREEN))), text(
                            Lang.getText("one-step-forward-lap", player.locale()),
                            TextColor.color(BLUE)
                        )
                    )
                )
            } else {
                player.showTitle(
                    title(
                        (text(MessageFormat.format(Lang.getText("now-lap", player.locale()), currentLap, lap), TextColor.color(GREEN))), text
                            (Lang.getText("one-step-forward-lap", player.locale()), TextColor.color(BLUE))
                    )
                )
            }
            Bukkit.getScheduler().runTaskLater(plugin!!, Runnable {
                player.clearTitle()
            }, 40)
        } else if (currentLap < beforeLap) {
            player.showTitle(
                title(
                    text(MessageFormat.format(Lang.getText("now-lap", player.locale()), currentLap, lap), TextColor.color(GREEN)),
                    text(Lang.getText("one-step-backwards-lap", player.locale()), TextColor.color(RED))
                )
            )
            Bukkit.getScheduler().runTaskLater(plugin!!, Runnable {
                player.clearTitle()
            }, 40)
        }
    }

    private fun displayScoreboard(
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

    private fun decideRanking(totalDegree: HashMap<UUID, Int>): ArrayList<UUID> {
        val ranking = ArrayList<UUID>()
        val sorted = totalDegree.toList().sortedBy { (_, value) -> value }
        sorted.forEach {
            ranking.add(it.first)
        }
        ranking.reverse()
        return ranking
    }

    private suspend fun getLapCount(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.first()[RaceList.lap]
    }

    private suspend fun getAllJockeys(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        val jockeys: ArrayList<OfflinePlayer> = ArrayList()
        PlayerList.select { PlayerList.raceID eq raceID }.forEach {
            jockeys.add(Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])))
        }
        jockeys
    }

    private suspend fun getGoalDegree(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.goalDegree)
    }

    private suspend fun getCircuitExist(raceID: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq inside) }.count() > 0
    }

    private suspend fun getPolygon(raceID: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        val polygon = Polygon()
        CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq inside) }.forEach {
            polygon.addPoint(it[CircuitPoint.XPoint], it[CircuitPoint.YPoint])
        }
        polygon
    }

    companion object {

        var starting = false
        var stop = HashMap<String, Boolean>()

        suspend fun getRaceCreator(raceID: String): UUID? = newSuspendedTransaction(Dispatchers.IO) {
            UUID.fromString(RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.creator))
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

    }
}

