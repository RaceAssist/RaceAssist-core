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
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.RaceAssist.Companion.setRaceID
import dev.nikomaru.raceassist.database.BetSetting
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.dispatch.discord.DiscordWebhook
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.race.commands.AudiencesCommand.Companion.audience
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
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.awt.Polygon
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

        if (starting) {
            sender.sendMessage(text("他のレース開始中です", TextColor.color(RED)))
            return
        }
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(text("レース作成者しか開始することはできません", TextColor.color(RED)))
            return
        }
        if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
            sender.sendMessage(text("レースが存在しません", TextColor.color(YELLOW)))
            return
        }
        val jockeys: ArrayList<Player> = ArrayList()
        getAllJockeys(raceID).forEach { jockey ->
            if (jockey.isOnline) {
                jockeys.add(jockey as Player)
                sender.sendMessage(text("${jockey.name}が参加しました", TextColor.color(GREEN)))
            } else {
                sender.sendMessage(text("${jockey.name}はオフラインです", TextColor.color(YELLOW)))
            }
        }
        if (jockeys.size < 2) {
            sender.sendMessage(text("開催には2人以上のユーザーが必要です", TextColor.color(YELLOW)))
            return
        }

        val centralXPoint: Int = getCentralPoint(raceID, true) ?: return sender.sendMessage(text("中心点が存在しません", TextColor.color(YELLOW)))
        val centralYPoint: Int = getCentralPoint(raceID, false) ?: return sender.sendMessage(text("中心点が存在しません", TextColor.color(YELLOW)))
        val goalDegree: Int = getGoalDegree(raceID) ?: return sender.sendMessage(text("ゴール角度が存在しません", TextColor.color(YELLOW)))

        if (goalDegree % 90 != 0) {
            sender.sendMessage(text("ゴール角度は90の倍数である必要があります", TextColor.color(YELLOW)))
            return
        }
        plugin!!.launch {
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
            var outsidePolygon = Polygon()
            val setInsidePolygon = async(Dispatchers.IO) {
                insidePolygon = getPolygon(raceID, true)

            }
            var reverse = false
            var innerCircumference = 0.0

            var lap = 1
            val getLap = async(Dispatchers.IO) {
                lap = getLapCount(raceID)
            }
            val setOutsidePolygon = async(Dispatchers.IO) {
                outsidePolygon = getPolygon(raceID, false)
            }
            val setReverse = async(Dispatchers.IO) {
                reverse = getReverse(raceID) ?: false
            }
            val calculateCircumference = async(Dispatchers.async) {
                //内周の距離のカウント
                val insideX = insidePolygon.xpoints
                val insideY = insidePolygon.ypoints
                setInsidePolygon.await()
                for (i in 0 until insidePolygon.npoints) {
                    innerCircumference += if (i <= insidePolygon.npoints - 2) {
                        hypot((insideX[i] - insideX[i + 1]).toDouble(), (insideY[i] - insideY[i + 1]).toDouble())
                    } else {
                        hypot((insideX[i] - insideX[0]).toDouble(), (insideY[i] - insideY[0]).toDouble())
                    }
                }
            }

            //観客(スコアボードを表示する人)の設定
            audience[raceID]?.forEach {
                if (Bukkit.getPlayer(it) != null && Bukkit.getPlayer(it)?.isOnline == true) {
                    raceAudience.add(it)
                }
            }
            jockeys.forEach {
                raceAudience.add(it.uniqueId)
            }
            raceAudience.add(sender.uniqueId)

            //5.4.3...1 のカウント
            var timer1 = 0
            while (timer1 <= 4) {
                val sendTimer = async(Dispatchers.minecraft) {
                    raceAudience.forEach {
                        Bukkit.getPlayer(it)?.showTitle(title(text("${5 - timer1}", TextColor.color(GREEN)), text(" ")))
                    }
                }
                delay(1000)
                sendTimer.await()
                timer1++
            }

            getLap.await()
            setOutsidePolygon.await()
            setReverse.await()
            calculateCircumference.await()

            jockeys.forEach {
                beforeDegree[it.uniqueId] = getRaceDegree(
                    if (!reverse) it.location.blockX - centralXPoint
                    else -(it.location.blockX - centralXPoint), it.location.blockZ - centralYPoint
                )
                currentLap[it.uniqueId] = 0
                passBorders[it.uniqueId] = 0
            }
            val beforeTime = System.currentTimeMillis()
            raceAudience.forEach {
                Bukkit.getPlayer(it)?.showTitle(title(text("レースが開始しました", TextColor.color(GREEN)), text(" ")))
            }

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
                    val calculateLap = async(Dispatchers.async) {
                        currentLap[uuid] = currentLap[uuid]!! + judgeLap(goalDegree, reverse, beforeDegree[uuid], currentDegree, threshold)
                        passBorders[uuid] = passBorders[uuid]!! + judgeLap(0, reverse, beforeDegree[uuid], currentDegree, threshold)
                        displayLap(currentLap[uuid], beforeLap, player, lap)
                        beforeDegree[uuid] = currentDegree
                        totalDegree[uuid] = currentDegree + (passBorders[uuid]!! * 360)
                    }

                    if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                        player.sendActionBar(text("レース場外に出てる可能性があります", TextColor.color(RED)))
                    }

                    calculateLap.await()

                    if (currentLap[uuid]!! >= lap) {
                        iterator.remove()
                        finishJockey.add(uuid)
                        totalDegree.remove(uuid)
                        currentLap.remove(uuid)
                        player.showTitle(title(text("${jockeyCount - jockeys.size}/$jockeyCount 位です", TextColor.color(GREEN)), text("")))
                        time[uuid] = ((System.currentTimeMillis() - beforeTime) / 1000).toInt()
                        continue
                    }
                }
                val displayRanking = async(Dispatchers.minecraft) {
                    val raceAudienceIterator = raceAudience.iterator()
                    while (raceAudienceIterator.hasNext()) {
                        val it = raceAudienceIterator.next()
                        if (!Bukkit.getOfflinePlayer(it).isOnline) {
                            raceAudienceIterator.remove()
                        }
                    }
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

            delay(2000)
            raceAudience.forEach {
                Bukkit.getPlayer(it)?.showTitle(title(text("レースが終了しました", TextColor.color(GREEN)), text("")))
                for (i in 0 until finishJockey.size) {
                    Bukkit.getPlayer(it)?.sendMessage("${i + 1}位  ${Bukkit.getPlayer(finishJockey[i])?.name}")
                }
            }

            if (Config.discordWebHook != null) {
                sendWebHook(finishJockey, time, sender.uniqueId, raceID)
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
        embedsObject["title"] = "レース結果"
        author["name"] = "開始した人:${Bukkit.getOfflinePlayer(starter).name}   レースID:$raceID"
        author["icon_url"] = "https://crafthead.net/avatar/$starter"
        embedsObject["author"] = author
        for (i in 0 until finishJockey.size) {
            val playerResult = JSONObject()
            playerResult["name"] = "${i + 1}位"
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
        if (starting) {
            sender.sendMessage(text("他のレース開始中です", TextColor.color(RED)))
            return
        }
        starting = true
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(text("レース作成者しか開始することはできません", TextColor.color(RED)))
        }
        if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
            sender.sendMessage(text("レースが存在しません", TextColor.color(YELLOW)))
            return
        }

        val insidePolygon = getPolygon(raceID, true)
        val outsidePolygon = getPolygon(raceID, false)
        if (insidePolygon.npoints < 3 || outsidePolygon.npoints < 3) {
            sender.sendMessage(text("レースが存在しません", TextColor.color(YELLOW)))
            return
        }
        val reverse = getReverse(raceID) ?: false
        val lap: Int = getLapCount(raceID)
        val threshold = 40
        val centralXPoint: Int = getCentralPoint(raceID, true) ?: return sender.sendMessage(text("中心点が存在しません", TextColor.color(YELLOW)))
        val centralYPoint: Int = getCentralPoint(raceID, false) ?: return sender.sendMessage(text("中心点が存在しません", TextColor.color(YELLOW)))
        val goalDegree: Int = getGoalDegree(raceID) ?: return sender.sendMessage(text("ゴール角度が存在しません", TextColor.color(YELLOW)))
        var beforeDegree = 0
        var currentLap = 0
        var counter = 0

        plugin?.launch {

            withContext(Dispatchers.minecraft) {
                for (timer in 0..4) {
                    sender.showTitle(title(text("${5 - timer}", TextColor.color(GREEN)), text(" ")))
                    delay(1000)
                }
            }

            sender.showTitle(title(text("スタートです", TextColor.color(GREEN)), text(" ")))
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
                    it.sendActionBar(text("レース場外に出てる可能性があります", TextColor.color(RED)))
                }
                beforeDegree = currentDegree

                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective("ranking", "dummy", text("デバッグモード", TextColor.color(YELLOW)))
                objective.displaySlot = DisplaySlot.SIDEBAR

                val score = objective.getScore("§61位" + "   " + "§b${sender.name}")
                score.score = 4
                val degree = "現在のラップ$currentLap Lap 現在の角度$currentDegree°"

                val displayDegree = objective.getScore(degree)
                displayDegree.score = 2
                val residue = objective.getScore("残り時間     ${180 - counter} 秒")
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
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage(text("レース作成者しか停止することはできません", TextColor.color(RED)))
        }
        stop[raceID] = true

        plugin?.launch {
            delay(1000)
            stop[raceID] = false
        }
    }

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("create")
    fun create(sender: CommandSender, @Single raceID: String) {
        if (getRaceCreator(raceID) != null) {
            sender.sendMessage("その名前のレース場は既に設定されています")
            return
        }
        transaction {
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
            }
        }
        setRaceID()
        sender.sendMessage("レース場を作成しました")
    }

    @CommandPermission("RaceAssist.commands.race")
    @Subcommand("delete")
    @CommandCompletion("@RaceID")
    fun delete(sender: CommandSender, @Single raceID: String) {

        if (getRaceCreator(raceID) == null) {
            sender.sendMessage("レース場が設定されていません")
            return
        }
        if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
            sender.sendMessage("レース場作成者が設定してください")
            return
        }


        transaction {
            RaceList.deleteWhere { RaceList.raceID eq raceID }
            CircuitPoint.deleteWhere { CircuitPoint.raceID eq raceID }
            PlayerList.deleteWhere { PlayerList.raceID eq raceID }
        }
        setRaceID()
        sender.sendMessage("レース場、及びプレイヤーなどのデータを削除しました")
    }

    private fun displayLap(currentLap: Int?, beforeLap: Int?, player: Player, lap: Int) {
        if (currentLap == null || beforeLap == null) {
            return
        }
        if (currentLap > beforeLap) {
            if (currentLap == lap - 1) {
                player.showTitle(title((text("最終ラップです ", TextColor.color(GREEN))), text("ラップが一つ進みました", TextColor.color(BLUE))))
            } else {
                player.showTitle(title((text("現在${currentLap} / ${lap}ラップです ", TextColor.color(GREEN))), text("ラップが一つ進みました", TextColor.color(BLUE))))
            }
            Bukkit.getScheduler().runTaskLater(plugin!!, Runnable {
                player.clearTitle()
            }, 40)
        } else if (currentLap < beforeLap) {
            player.showTitle(title(text("現在${currentLap} / ${lap}ラップです ", TextColor.color(GREEN)), text("ラップが一つ戻りました", TextColor.color(RED))))
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
        val manager: ScoreboardManager = Bukkit.getScoreboardManager()
        val scoreboard = manager.newScoreboard
        val objective: Objective = scoreboard.registerNewObjective("ranking", "dummy", text("現在のランキング", TextColor.color(YELLOW)))
        objective.displaySlot = DisplaySlot.SIDEBAR

        for (i in nowRankings.indices) {
            val playerName = Bukkit.getPlayer(nowRankings[i])?.name
            val score = objective.getScore("§6${i + 1}位" + "   " + "§b$playerName")
            score.score = nowRankings.size * 2 - 2 * i - 1
            val degree: String = if (currentLap[Bukkit.getPlayer(nowRankings[i])?.uniqueId] == null) {
                "完走しました"
            } else {
                "現在のラップ${
                    currentLap[Bukkit.getPlayer(nowRankings[i])?.uniqueId]?.toInt()
                }/ $lap Lap 現在の距離${
                    (currentDegree[Bukkit.getPlayer(nowRankings[i])?.uniqueId]?.minus(startDegree))?.times(innerCircumference)?.div(360)
                }m"
            }
            val displayDegree = objective.getScore(degree)
            displayDegree.score = nowRankings.size * 2 - 2 * i - 2
        }
        raceAudience.forEach {
            Bukkit.getPlayer(it)?.scoreboard = scoreboard
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

    private fun getRaceCreator(raceID: String): UUID? {
        var creatorUUID: UUID? = null
        transaction {

            RaceList.select { RaceList.raceID eq raceID }.forEach {
                creatorUUID = UUID.fromString(it[RaceList.creator])
            }
        }
        return creatorUUID
    }

    private fun getLapCount(raceID: String): Int {

        var lapCount = 1
        transaction {
            lapCount = RaceList.select { RaceList.raceID eq raceID }.first()[RaceList.lap]
        }
        return lapCount
    }

    private fun getAllJockeys(raceID: String): ArrayList<OfflinePlayer> {
        val jockeys: ArrayList<OfflinePlayer> = ArrayList()
        transaction {
            PlayerList.select { PlayerList.raceID eq raceID }.forEach {
                jockeys.add(Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])))
            }
        }
        return jockeys
    }

    private fun getGoalDegree(raceID: String): Int? {
        var goalDegree: Int? = null
        transaction {
            goalDegree = RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.goalDegree)
        }
        return goalDegree
    }

    private fun getCircuitExist(raceID: String, inside: Boolean): Boolean {
        var raceExist = false

        transaction {
            raceExist = CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq inside) }.count() > 0

        }
        return raceExist
    }


    private fun getPolygon(raceID: String, inside: Boolean): Polygon {
        val polygon = Polygon()
        transaction {
            CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq inside) }.forEach {
                polygon.addPoint(it[CircuitPoint.XPoint], it[CircuitPoint.YPoint])
            }
        }
        return polygon
    }

    companion object {

        var starting = false
        var stop = HashMap<String, Boolean>()

        fun getRaceCreator(raceID: String): UUID? {
            var uuid: UUID? = null
            transaction {
                RaceList.select { RaceList.raceID eq raceID }.forEach {
                    uuid = UUID.fromString(it[RaceList.creator])
                }
            }
            return uuid
        }

        fun getCentralPoint(raceID: String, xPoint: Boolean): Int? {
            var centralPoint: Int? = null
            transaction {
                RaceList.select { RaceList.raceID eq raceID }.forEach {
                    centralPoint = if (xPoint) {
                        it.getOrNull(RaceList.centralXPoint)
                    } else {
                        it.getOrNull(RaceList.centralYPoint)
                    }
                }
            }
            return centralPoint
        }

        fun getReverse(raceID: String): Boolean? {
            var reverse: Boolean? = null

            transaction {
                reverse = RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.reverse)

            }
            return reverse
        }
    }
}

