/*
 *     Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 *
 *     This program is free software: you can redistribute it and/or modify
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

package dev.nikomaru.raceassist.race

import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.api.core.manager.PlaceManager
import dev.nikomaru.raceassist.api.core.manager.RaceManager
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.data.files.json
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.RaceAudience
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.Utils.toLivingHorse
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import dev.nikomaru.raceassist.utils.event.Lang
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.ScoreboardManager
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.awt.Polygon
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

class RaceJudgement(private val raceId: String, private val executor: CommandSender) {

    private lateinit var placeId: String

    private lateinit var raceManager: RaceManager
    private lateinit var placeManager: PlaceManager

    private val locale = executor.locale()
    private lateinit var replacement: HashMap<UUID, String>

    var finished = false

    private val jockeys: ArrayList<Player> = ArrayList()
    private var jockeyCount = 0

    private val finishJockeys = arrayListOf<UUID>()

    private var threshold = 0

    private var centralXPoint = 0
    private var centralYPoint = 0
    private var goalDegree = 0
    private var lap: Int = 0
    private var reverse: Boolean = false

    private var innerCircumference: Double = 0.0

    private lateinit var insidePolygon: Polygon
    private lateinit var outsidePolygon: Polygon

    private var startDegree = 0

    private val totalDegree = hashMapOf<UUID, Int>()
    private val beforeDegree = hashMapOf<UUID, Int>()
    private val currentLap = hashMapOf<UUID, Int>()
    private val passBorders = hashMapOf<UUID, Int>()

    private val time = hashMapOf<UUID, Long>()

    private val audiences = RaceAudience()

    var suspend = false

    private var limit = 0L

    private lateinit var raceResultData: RaceResultData

    private var beforeTime = 0L

    suspend fun setting(): Boolean {
        raceManager = RaceAssist.api.getRaceManager(raceId) ?: return false
        placeId = raceManager.getPlaceId()
        placeManager = RaceAssist.api.getPlaceManager(placeId) ?: return false


        if (!placeManager.getTrackExist()) {
            executor.sendMessage(Lang.getComponent("no-exist-race", locale))
            return false
        }

        raceManager.getJockeys().forEach {
            if (it.isOnline) {
                jockeys.add(it as Player)
                executor.sendMessage(Lang.getComponent("player-join", locale, it.name))
            } else {
                executor.sendMessage(Lang.getComponent("player-is-offline", locale, it.name))
            }
        }
        if (jockeys.size < 2) {
            executor.sendMessage(Lang.getComponent("over-two-users-need", locale))
            return false
        }

        centralXPoint = placeManager.getCentralPointX() ?: run {
            executor.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            return false
        }

        centralYPoint = placeManager.getCentralPointY() ?: run {
            executor.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            return false
        }

        goalDegree = placeManager.getGoalDegree()

        jockeyCount = jockeys.size
        threshold = Config.config.threshold
        lap = raceManager.getLap()
        reverse = placeManager.getReverse()
        innerCircumference = getInnerCircumference(insidePolygon)


        insidePolygon = placeManager.getInside()
        outsidePolygon = placeManager.getOutside()

        limit = Config.config.raceLimitMilliSecond

        //観客(スコアボードを表示する人)の設定
        Utils.audience[raceId]?.forEach {
            audiences.add(Bukkit.getOfflinePlayer(it))
        }
        jockeys.forEach {
            audiences.add(it)
        }
        if (executor is Player) {
            audiences.add(executor)
        }

        replacement = raceManager.getReplacement()
        raceManager.getHorse().forEach { (t, u) ->
            val name = u.toLivingHorse()?.customName()?.toPlainText()
            if (name != null) {
                replacement[t] = name
            }
        }

        return true
    }

    suspend fun start() {

        var timer = 0
        while (timer <= 4) {
            audiences.showTitle(Title.title(Component.text("${5 - timer}"), Component.text("")))
            delay(1000)
            timer++
        }

        jockeys.forEach {
            beforeDegree[it.uniqueId] = Utils.getRaceDegree(
                if (!reverse) (it.location.blockX - centralXPoint).toDouble()
                else (-1 * (it.location.blockX - centralXPoint)).toDouble(),
                (it.location.blockZ - centralYPoint).toDouble()
            )
            currentLap[it.uniqueId] = 0
            passBorders[it.uniqueId] = 0
        }

        beforeTime = System.currentTimeMillis()
        audiences.showTitleI18n("race-start")

        //最初の位置をランダムに選んだ人から決める
        val randomJockey = jockeys.random()
        val startDegree = getStartPoint(randomJockey, centralYPoint, reverse, centralXPoint)

        // 結果の保存のため
        val goalDistance = getGoalDistance(lap, goalDegree, startDegree, innerCircumference)

        val senderName = if (executor is Player) executor.name else "Console"

        val uuidToName = jockeys.associate { it.uniqueId to it.name } as HashMap<UUID, String>

        val rectangleData = RectangleData(
            outsidePolygon.bounds2D.minX.roundToInt() - 4,
            outsidePolygon.bounds2D.minY.roundToInt() - 4,
            outsidePolygon.bounds2D.maxX.roundToInt() + 4,
            outsidePolygon.bounds2D.maxY.roundToInt() + 4
        )
        val horses: HashMap<UUID, UUID> = HashMap()
        jockeys.forEach {
            val vehicle = it.vehicle
            if (vehicle != null) {
                if (vehicle is Horse) {
                    horses[it.uniqueId] = vehicle.uniqueId
                }
            }
        }

        raceResultData = RaceResultData(
            "1.0",
            raceId,
            senderName,
            horses,
            ZonedDateTime.now(),
            ZonedDateTime.now(),
            false,
            hashMapOf(),
            lap,
            goalDistance,
            uuidToName,
            replacement,
            rectangleData,
            insidePolygon,
            outsidePolygon,
            arrayListOf(),
            null
        )

    }

    private fun getStartPoint(randomJockey: Player, centralYPoint: Int, reverse: Boolean, centralXPoint: Int) =
        Utils.getRaceDegree(
            (randomJockey.location.blockZ - centralYPoint).toDouble(), if (reverse) {
                (-1 * (randomJockey.location.blockX - centralXPoint)).toDouble()
            } else {
                (randomJockey.location.blockX - centralXPoint).toDouble()
            }
        )

    private suspend fun putRaceResult(raceResultData: RaceResultData) {
        withContext(Dispatchers.IO) {
            val resultFolder = RaceAssist.plugin.dataFolder.resolve("result")
            resultFolder.mkdirs()
            val resultFile = resultFolder.resolve("${raceResultData.raceId}.json")
            resultFile.writeText(json.encodeToString(raceResultData))
            sendResultWebHook(raceResultData)
        }
    }

    private suspend fun sendResultWebHook(raceResultData: RaceResultData) {
        Config.config.resultWebhook.forEach {
            var editUrl = it.url
            if (editUrl.last() != '/') {
                editUrl += "/"
            }
            editUrl += "v1/result/push/${raceResultData.raceId}"

            Utils.client.post(editUrl) {
                contentType(ContentType.Application.Json)
                setBody(raceResultData)
                headers {
                    val token = Base64.getEncoder().encodeToString("${it.name}:${it.password}".toByteArray())
                    append("Authorization", "Basic $token")
                }
            }
        }
    }

    private suspend fun getInnerCircumference(insidePolygon: Polygon) = withContext(Dispatchers.Default) {
        //内周の距離のカウント
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
        total
    }

    private suspend fun sendWebHook(
        finishJockey: ArrayList<UUID>,
        time: HashMap<UUID, Long>,
        starter: OfflinePlayer,
        raceId: String,
        suspend: Boolean
    ) {
        val json = JSONObject()
        json["username"] = "RaceAssist"
        json["avatar_url"] =
            "https://3.bp.blogspot.com/-Y3AVYVjLcPs/UYiNxIliDxI/AAAAAAAARSg/nZLIqBRUta8/s800/animal_uma.png"
        val result = JSONArray()
        val embeds = JSONArray()
        val author = JSONObject()
        val embedsObject = JSONObject()
        embedsObject["title"] = if (suspend) "RaceAssist_suspend" else "RaceAssist"
        author["name"] = Lang.getText("discord-webhook-name", Locale.getDefault(), starter.name, raceId)
        author["icon_url"] = "https://crafthead.net/avatar/$starter"
        embedsObject["author"] = author
        for (i in 0 until finishJockey.size) {
            val playerResult = JSONObject()
            playerResult["name"] = Lang.getText("discord-webhook-ranking", Locale.getDefault(), i + 1)
            playerResult["value"] = String.format(
                "%s %2d:%02d",
                Bukkit.getPlayer(finishJockey[i])?.name,
                floor((time[finishJockey[i]]!!.toDouble() / 60000)).toInt(),
                time[finishJockey[i]]!! % 60000
            )
            playerResult["inline"] = true
            result.add(playerResult)
        }
        embedsObject["fields"] = result
        embeds.add(embedsObject)
        json["embeds"] = embeds

        sendDiscordResultWebHook(json.toString())
    }

    private suspend fun sendDiscordResultWebHook(json: String) = withContext(Dispatchers.IO) {

        Config.config.discordWebHook.race.forEach {
            try {
                val webHookUrl = URL(it)
                val con: HttpsURLConnection = (webHookUrl.openConnection() as HttpsURLConnection)

                con.addRequestProperty("Content-Type", "application/JSON; charset=utf-8")
                con.addRequestProperty("User-Agent", "DiscordBot")
                con.doOutput = true
                con.requestMethod = "POST"

                con.setRequestProperty("Content-Length", json.length.toString())

                val stream: OutputStream = con.outputStream
                stream.write(json.toByteArray(Charsets.UTF_8))
                stream.flush()
                stream.close()

                val status: Int = con.responseCode
                if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_NO_CONTENT) {
                    RaceAssist.plugin.logger.warning("error:$status")
                }
                con.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayScoreboard(
        nowRankings: List<UUID>,
        currentDegree: HashMap<UUID, Int>,
        raceAudience: Collection<UUID>,
        innerCircumference: Int,
        startDegree: Int,
        goalDegree: Int,
        lap: Int
    ) {

        raceAudience.forEach {

            if (Bukkit.getOfflinePlayer(it).isOnline) {
                val player = Bukkit.getPlayer(it)!!
                val manager: ScoreboardManager = Bukkit.getScoreboardManager()
                val scoreboard = manager.newScoreboard
                val objective: Objective = scoreboard.registerNewObjective(
                    Lang.getText("scoreboard-ranking", player.locale()),
                    "dummy",
                    Lang.getComponent("scoreboard-now-ranking", player.locale())
                )
                objective.displaySlot = DisplaySlot.SIDEBAR

                val goalDistance = getGoalDistance(lap, goalDegree, startDegree, innerCircumference.toDouble())

                for (i in nowRankings.indices) {

                    val playerName = replacement[nowRankings[i]] ?: Bukkit.getPlayer(nowRankings[i])?.name

                    val component = if (currentDegree[Bukkit.getPlayer(nowRankings[i])!!.uniqueId] == null) {
                        Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)
                            .append(Lang.getComponent("finished-the-race", player.locale()))
                    } else {
                        val currentDistance =
                            ((currentDegree[Bukkit.getPlayer(nowRankings[i])!!.uniqueId]!!.toDouble() - startDegree.toDouble()) / 360.0 * innerCircumference.toDouble()).toInt()

                        Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)
                            .append(Lang.mm.deserialize("${currentDistance}m/${goalDistance}m "))
                    }

                    val displayDegree =
                        objective.getScore(LegacyComponentSerializer.legacySection().serialize(component))
                    displayDegree.score = nowRankings.size - i
                }
                player.scoreboard = scoreboard
            }
        }
    }

    private fun getGoalDistance(lap: Int, goalDegree: Int, startDegree: Int, innerCircumference: Double) =
        (((lap - 1).toDouble() + if (goalDegree > startDegree) {
            ((goalDegree.toDouble() - startDegree.toDouble()) / 360.0)
        } else {
            ((goalDegree.toDouble() + 360.0 - startDegree.toDouble()) / 360.0)
        }) * innerCircumference).toInt()

    private fun decideRanking(totalDegree: HashMap<UUID, Int>): ArrayList<UUID> {
        val ranking = ArrayList<UUID>()
        val sorted = totalDegree.toList().sortedBy { (_, value) -> value }
        sorted.forEach {
            ranking.add(it.first)
        }
        ranking.reverse()
        return ranking
    }

    suspend fun calculate() {
        val currentRaceData =
            CurrentRaceData(((System.currentTimeMillis() - beforeTime).toDouble() / 1000), arrayListOf())

        //正常時の終了
        if (jockeys.size < 1) {
            finished = true
            return
        }
        //stopコマンドによる終了
        if (Utils.stop[raceId] == true) {
            suspend = true
            raceResultData.suspend = true
            audiences.sendMessageI18n("suspended-race-by-operator")
            finished = true
            return
        }
        //制限による終了
        if ((System.currentTimeMillis() - beforeTime) > limit) {
            suspend = true
            raceResultData.suspend = true
            audiences.sendMessageI18n("suspended-race-by-limit")
            finished = true
            return
        }

        val iterator = jockeys.iterator()

        while (iterator.hasNext()) {
            val player: Player = iterator.next()

            if (!player.isOnline) {
                iterator.remove()
                continue
            }
            //各騎手の位置の取得
            val nowX = withContext(Dispatchers.minecraft) { player.location.blockX }
            val nowY = withContext(Dispatchers.minecraft) { player.location.blockZ }
            val relativeNowX = if (!reverse) nowX - centralXPoint else -1 * (nowX - centralXPoint)
            val relativeNowY = nowY - centralYPoint
            val currentDegree = Utils.getRaceDegree(relativeNowY.toDouble(), relativeNowX.toDouble())
            val uuid = player.uniqueId
            val beforeLap = currentLap[uuid]

            //ラップの計算
            currentLap[uuid] =
                currentLap[uuid]!! + Utils.judgeLap(goalDegree, beforeDegree[uuid], currentDegree, threshold)
            passBorders[uuid] = passBorders[uuid]!! + Utils.judgeLap(0, beforeDegree[uuid], currentDegree, threshold)
            Utils.displayLap(currentLap[uuid], beforeLap, player, lap)
            beforeDegree[uuid] = currentDegree
            totalDegree[uuid] = currentDegree + (passBorders[uuid]!! * 360)

            val currentDistance =
                (((totalDegree[uuid]!!.toDouble() - startDegree.toDouble()) / 360.0) * innerCircumference).toInt()

            val currentResultData = PlayerRaceData(uuid, false, currentDistance, nowX, nowY)
            currentRaceData.playerRaceData.add(currentResultData)

            //コース内にいるか判断
            if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                player.sendActionBar(Lang.getComponent("outside-the-racetrack", player.locale()))
            }

            //ゴールした時の処理
            if (currentLap[uuid]!! >= lap) {
                withContext(Dispatchers.async) {
                    iterator.remove()
                    finishJockeys.add(uuid)
                    totalDegree.remove(uuid)
                    currentLap.remove(uuid)
                    player.showTitle(
                        Title.title(
                            Lang.getComponent(
                                "player-ranking",
                                player.locale(),
                                jockeyCount - jockeys.size,
                                jockeyCount
                            ),
                            Component.text("")
                        )
                    )
                }
                time[uuid] = (System.currentTimeMillis() - beforeTime)
                continue
            }

        }

        finishJockeys.forEach { finishJockey ->
            val player = Bukkit.getPlayer(finishJockey) ?: return@forEach
            val uuid = player.uniqueId
            val none = currentRaceData.playerRaceData.none { it.uuid == uuid }
            if (none) {
                currentRaceData.playerRaceData.add(PlayerRaceData(uuid, true, null, null, null))
            }
        }
        //現在のレースの状況保存のため
        raceResultData.currentRaceData.add(currentRaceData)

    }

    suspend fun display() {
        //順位の表示
        RaceAssist.plugin.launch {
            val displayRanking = async(Dispatchers.minecraft) {
                displayScoreboard(
                    finishJockeys.plus(decideRanking(totalDegree)),
                    totalDegree,
                    audiences.getUUID(),
                    innerCircumference.roundToInt(),
                    startDegree,
                    goalDegree,
                    lap
                )
            }
            delay(Config.config.delay)
            displayRanking.await()
        }.join()
    }

    suspend fun last() {
        //終了時の処理
        raceResultData.finish = ZonedDateTime.now()
        audiences.showTitleI18n("finish-race")
        delay(1000)

        //後始末
        Bukkit.getOnlinePlayers().forEach {
            it.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
        }

        for (i in 0 until finishJockeys.size) {
            audiences.sendMessageI18n("to-notice-ranking-message", i + 1, Bukkit.getPlayer(finishJockeys[i])?.name!!)
        }


        finishJockeys.forEachIndexed { index, element ->
            raceResultData.result[index + 1] = element
        }
        raceResultData.image = Utils.createImage(
            raceResultData.rectangleData.x1,
            raceResultData.rectangleData.x2,
            raceResultData.rectangleData.y1,
            raceResultData.rectangleData.y2
        )

        //結果の保存
        putRaceResult(raceResultData)
        sendWebHook(finishJockeys, time, raceManager.getOwner(), raceId, suspend)
    }

    suspend fun payDividend() {
        BetUtils.payDividend(finishJockeys[0].toOfflinePlayer(), raceId, executor, executor.locale())
    }

}