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




package dev.nikomaru.raceassist.race

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.bet.BetUtils
import dev.nikomaru.raceassist.data.utils.json
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.race.error.PlaceSettingError
import dev.nikomaru.raceassist.race.error.RaceSettingError
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.Utils
import dev.nikomaru.raceassist.utils.Utils.locale
import dev.nikomaru.raceassist.utils.Utils.toLivingHorse
import dev.nikomaru.raceassist.utils.Utils.toOfflinePlayer
import dev.nikomaru.raceassist.utils.Utils.toPlainText
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Horse
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.ScoreboardManager
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

class PlainRaceJudgement(override val raceId: String, override val executor: CommandSender) :
    RaceJudgement(raceId, executor), KoinComponent {
    val plugin: RaceAssist by inject()

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

    private val totalDegree = hashMapOf<UUID, Double>()
    private val beforeDegree = hashMapOf<UUID, Double>()
    private val passBorders = hashMapOf<UUID, Int>()

    private lateinit var raceResultData: RaceResultData

    override suspend fun raceSetting(): Result<Unit, RaceSettingError> {
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
            return Err(RaceSettingError.PLAYER_IS_NOT_ENOUGH)
        }
        jockeyCount = jockeys.size
        lap = raceManager.getLap()
        limit = Config.config.raceLimitMilliSecond

        replacement = raceManager.getReplacement()
        raceManager.getHorse().forEach { (t, u) ->
            val name = u.toLivingHorse()?.customName()?.toPlainText()
            if (name != null) {
                replacement[t] = name
            }
        }

        return Ok(Unit)
    }

    override suspend fun placeSetting(): Result<Unit, PlaceSettingError> {
        if (!plainPlaceManager.getTrackExist()) {
            executor.sendMessage(Lang.getComponent("no-exist-race", locale))
            return Err(PlaceSettingError.TRACK_IS_NOT_FOUND)
        }

        centralXPoint = plainPlaceManager.getCentralPointX() ?: run {
            executor.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            return Err(PlaceSettingError.CENTRAL_POINT_IS_NOT_FOUND)
        }

        centralYPoint = plainPlaceManager.getCentralPointY() ?: run {
            executor.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            return Err(PlaceSettingError.CENTRAL_POINT_IS_NOT_FOUND)
        }

        goalDegree = plainPlaceManager.getGoalDegree()
        threshold = Config.config.threshold
        reverse = plainPlaceManager.getReverse()
        insidePolygon = plainPlaceManager.getInside()
        outsidePolygon = plainPlaceManager.getOutside()
        innerCircumference = getInnerCircumference(insidePolygon)
        return Ok(Unit)
    }


    override suspend fun start() {

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
            outsidePolygon.bounds2D.minX.roundToInt() - 10,
            outsidePolygon.bounds2D.minY.roundToInt() - 10,
            outsidePolygon.bounds2D.maxX.roundToInt() + 10,
            outsidePolygon.bounds2D.maxY.roundToInt() + 10
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
            val resultFolder = plugin.dataFolder.resolve("result")
            resultFolder.mkdirs()
            val resultFile = resultFolder.resolve("${raceResultData.raceId}.json")
            resultFile.writeText(json.encodeToString(raceResultData))
            sendResultWebHook(raceResultData)
        }
    }

    private suspend fun sendResultWebHook(raceResultData: RaceResultData) {
        Config.config.webAPI?.recordUrl?.forEach {
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
                    plugin.logger.warning("error:$status")
                }
                con.disconnect()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayScoreboard(
        nowRankings: List<UUID>,
        currentDegree: HashMap<UUID, Double>,
        raceAudience: Collection<UUID>,
        innerCircumference: Int,
        startDegree: Double,
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
                    Criteria.DUMMY,
                    Lang.getComponent("scoreboard-now-ranking", player.locale())
                )
                objective.displaySlot = DisplaySlot.SIDEBAR

                val goalDistance = getGoalDistance(lap, goalDegree, startDegree, innerCircumference.toDouble())

                for (i in nowRankings.indices) {

                    val playerName = replacement[nowRankings[i]] ?: Bukkit.getPlayer(nowRankings[i])?.name

                    val mm = MiniMessage.miniMessage()
                    if (currentDegree[Bukkit.getPlayer(nowRankings[i])!!.uniqueId] == null) {
                        val component =
                            Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)
                                .append(mm.deserialize(" "))
                                .append(Lang.getComponent("finished-the-race", player.locale()))

                        val displayDegree =
                            objective.getScore(LegacyComponentSerializer.legacySection().serialize(component))
                        displayDegree.score = nowRankings.size - i
                    } else {
                        val currentLap = currentLap[Bukkit.getPlayer(nowRankings[i])!!.uniqueId]
                        val lapMessage = "lap : ${currentLap!!} / $lap"
                        val currentDistance =
                            (((currentDegree[Bukkit.getPlayer(nowRankings[i])!!.uniqueId]!!.toDouble() - startDegree) / 360.0) * innerCircumference).toInt()
                        val distanceMessage =
                            "distance : ${currentDistance}m / ${goalDistance}m"
                        val detailMessage = "$lapMessage , $distanceMessage"
                        val component1 =
                            Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)
                                .append(mm.deserialize(" "))
                                .append(mm.deserialize(distanceMessage))
//                        val component2 = mm.deserialize(detailMessage)

                        val displayDegree =
                            objective.getScore(LegacyComponentSerializer.legacySection().serialize(component1))
                        displayDegree.score = nowRankings.size - i
//                        val displayDetail =
//                            objective.getScore(LegacyComponentSerializer.legacySection().serialize(component2))
                    }
                }
                player.scoreboard = scoreboard
            }
        }
    }

    private fun getGoalDistance(lap: Int, goalDegree: Int, startDegree: Double, innerCircumference: Double) =
        (((lap - 1).toDouble() + if (goalDegree > startDegree) {
            ((goalDegree.toDouble() - startDegree) / 360.0)
        } else {
            ((goalDegree.toDouble() + 360.0 - startDegree) / 360.0)
        }) * innerCircumference).toInt()

    private fun decideRanking(totalDegree: HashMap<UUID, Double>): ArrayList<UUID> {
        val ranking = ArrayList<UUID>()
        val sorted = totalDegree.toList().sortedBy { (_, value) -> value }
        sorted.forEach {
            ranking.add(it.first)
        }
        ranking.reverse()
        return ranking
    }

    override suspend fun calculate() {
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
            currentLap[uuid] = currentLap[uuid]!! + Utils.judgeLap(
                goalDegree, beforeDegree[uuid]?.toInt(), currentDegree.toInt(), threshold
            )
            passBorders[uuid] = passBorders[uuid]!! + Utils.judgeLap(
                0, beforeDegree[uuid]?.toInt(), currentDegree.toInt(), threshold
            )
            plugin.launch {
                async(Dispatchers.async) {
                    Utils.displayLap(currentLap[uuid], beforeLap, player, lap)
                }
            }
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
                                "player-ranking", player.locale(), jockeyCount - jockeys.size, jockeyCount
                            ), Component.text("")
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

    override suspend fun display() {
        //順位の表示
        plugin.launch {
            val displayRanking = async(Dispatchers.minecraft) {
                displayScoreboard(
                    finishJockeys.plus(decideRanking(totalDegree)),
                    totalDegree,
                    audiences.getUUID(),
                    innerCircumference.roundToInt(),
                    startDegree.toDouble(),
                    goalDegree,
                    lap
                )
            }
            delay(Config.config.delay)
            displayRanking.await()
        }.join()
    }


    override suspend fun finish() {
        //終了時の処理
        raceResultData.finish = ZonedDateTime.now()
        audiences.showTitleI18n("finish-race")
        delay(1000)

        //後始末
        Bukkit.getOnlinePlayers().forEach {
            it.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
        }

        for (i in 0 until finishJockeys.size) {
            audiences.sendMessageI18n(
                "to-notice-ranking-message", i + 1, Bukkit.getPlayer(finishJockeys[i])?.name!!
            )
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

    override suspend fun payDividend() {
        BetUtils.payDividend(finishJockeys[0].toOfflinePlayer(), raceId, executor, executor.locale())
    }

}