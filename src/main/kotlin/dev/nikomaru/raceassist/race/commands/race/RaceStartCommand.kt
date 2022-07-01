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

package dev.nikomaru.raceassist.race.commands.race

import cloud.commandframework.annotations.*
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.bet.commands.BetReturnCommand
import dev.nikomaru.raceassist.data.files.PlaceSettingData
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.dispatch.discord.DiscordWebhook
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.*
import dev.nikomaru.raceassist.utils.CommandUtils.displayLap
import dev.nikomaru.raceassist.utils.CommandUtils.getRaceDegree
import dev.nikomaru.raceassist.utils.CommandUtils.judgeLap
import dev.nikomaru.raceassist.utils.CommandUtils.stop
import dev.nikomaru.raceassist.utils.Lang.mm
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scoreboard.*
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.awt.Polygon
import java.util.*
import kotlin.math.*

@CommandMethod("ra|RaceAssist race")
class RaceStartCommand {

    @CommandPermission("RaceAssist.commands.race.start")
    @CommandMethod("start <raceId>")
    suspend fun start(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        val locale = if (sender is Player) sender.locale() else Locale.getDefault()

        if (PlaceSettingData.getInsidePolygon(raceId).npoints == 0 || PlaceSettingData.getOutsidePolygon(raceId).npoints == 0) {
            sender.sendMessage(Lang.getComponent("no-exist-race", locale))
            return
        }
        val jockeys: ArrayList<Player> = ArrayList()
        RaceSettingData.getJockeys(raceId).forEach {
            if (it.isOnline) {
                jockeys.add(it as Player)
                sender.sendMessage(Lang.getComponent("player-join", locale, it.name))
            } else {
                sender.sendMessage(Lang.getComponent("player-is-offline", locale, it.name))
            }
        }
        if (jockeys.size < 2) {
            sender.sendMessage(Lang.getComponent("over-two-users-need", locale))
            return
        }

        val centralXPoint: Int =
            PlaceSettingData.getCentralXPoint(raceId) ?: return sender.sendMessage(Lang.getComponent("no-exist-central-point", locale))
        val centralYPoint: Int =
            PlaceSettingData.getCentralYPoint(raceId) ?: return sender.sendMessage(Lang.getComponent("no-exist-central-point", locale))
        val goalDegree: Int = PlaceSettingData.getGoalDegree(raceId)

        val jockeyCount = jockeys.size
        val finishJockey: ArrayList<UUID> = ArrayList<UUID>()
        val totalDegree: HashMap<UUID, Int> = HashMap()
        val beforeDegree: HashMap<UUID, Int> = HashMap()
        val currentLap: HashMap<UUID, Int> = HashMap()
        val threshold = Config.config.threshold
        val raceAudience: TreeSet<UUID> = TreeSet()
        var suspend = false

        val passBorders: HashMap<UUID, Int> = HashMap()
        val time: HashMap<UUID, Int> = HashMap()

        val insidePolygon = PlaceSettingData.getInsidePolygon(raceId)

        //観客(スコアボードを表示する人)の設定

        val audiences = RaceAudience()


        CommandUtils.audience[raceId]?.forEach {
            audiences.add(Bukkit.getOfflinePlayer(it))
        }
        jockeys.forEach {
            audiences.add(it)
        }
        if (sender is Player) {
            audiences.add(sender)
        }
        audiences.getUUID().forEach {
            raceAudience.add(it)
        }

        //5.4.3...1 のカウント
        var timer1 = 0
        while (timer1 <= 4) {
            audiences.showTitle(Title.title(text("${5 - timer1}"), text("")))
            delay(1000)
            timer1++
        }

        val lap = PlaceSettingData.getLap(raceId)

        val outsidePolygon = PlaceSettingData.getOutsidePolygon(raceId)

        val reverse = PlaceSettingData.getReverse(raceId)

        val innerCircumference = getInnerCircumference(insidePolygon)

        jockeys.forEach {
            beforeDegree[it.uniqueId] = getRaceDegree(if (!reverse) (it.location.blockX - centralXPoint).toDouble()
            else (-1 * (it.location.blockX - centralXPoint)).toDouble(), (it.location.blockZ - centralYPoint).toDouble())
            currentLap[it.uniqueId] = 0
            passBorders[it.uniqueId] = 0
        }

        val beforeTime = System.currentTimeMillis()
        audiences.showTitleI18n("race-start")

        val randomJockey = jockeys.random()
        val startDegree = getRaceDegree((randomJockey.location.blockZ - centralYPoint).toDouble(), if (reverse) {
            (-1 * (randomJockey.location.blockX - centralXPoint)).toDouble()
        } else {
            (randomJockey.location.blockX - centralXPoint).toDouble()
        })

        //レースの処理
        while (true) {
            //正常時の終了
            if (jockeys.size < 1) {
                BetReturnCommand.canReturn[raceId] = true
                BetReturnCommand.payRefund(Bukkit.getOfflinePlayer(finishJockey[0]), raceId, sender, locale)
                break
            }
            //stopコマンドによる終了
            if (stop[raceId] == true) {
                suspend = true
                audiences.sendMessageI18n("suspended-race")
                break
            }
            val iterator = jockeys.iterator()
            //各騎手の位置の取得
            while (iterator.hasNext()) {
                val player: Player = iterator.next()
                if (!player.isOnline) {
                    iterator.remove()
                    continue
                }

                val nowX: Int = player.location.blockX
                val nowY = player.location.blockZ
                val relativeNowX = if (!reverse) nowX - centralXPoint else -1 * (nowX - centralXPoint)
                val relativeNowY = nowY - centralYPoint
                val currentDegree = getRaceDegree(relativeNowY.toDouble(), relativeNowX.toDouble())
                val uuid = player.uniqueId

                val beforeLap = currentLap[uuid]
                //ラップの計算
                withContext(Dispatchers.Default) {
                    currentLap[uuid] = currentLap[uuid]!! + judgeLap(goalDegree, beforeDegree[uuid], currentDegree, threshold)
                    passBorders[uuid] = passBorders[uuid]!! + judgeLap(0, beforeDegree[uuid], currentDegree, threshold)
                    displayLap(currentLap[uuid], beforeLap, player, lap)
                    beforeDegree[uuid] = currentDegree
                    totalDegree[uuid] = currentDegree + (passBorders[uuid]!! * 360)
                }

                //コース内にいるか判断
                if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                    player.sendActionBar(Lang.getComponent("outside-the-racetrack", player.locale()))
                }

                //ゴールした時の処理
                if (currentLap[uuid]!! >= lap) {
                    iterator.remove()
                    finishJockey.add(uuid)
                    totalDegree.remove(uuid)
                    currentLap.remove(uuid)
                    player.showTitle(Title.title(Lang.getComponent("player-ranking", player.locale(), jockeyCount - jockeys.size, jockeyCount),
                        text("")))
                    time[uuid] = ((System.currentTimeMillis() - beforeTime) / 1000).toInt()
                    continue
                }
            }
            //順位の表示
            plugin.launch {
                val displayRanking = async(minecraft) {

                    displayScoreboard(finishJockey.plus(decideRanking(totalDegree)),
                        totalDegree,
                        raceAudience,
                        innerCircumference.roundToInt(),
                        startDegree,
                        goalDegree,
                        lap,
                        raceId)
                }
                delay(Config.config.delay)
                displayRanking.await()
            }.join()
        }
        //終了時の処理
        audiences.showTitleI18n("finish-race")
        delay(2000)

        for (i in 0 until finishJockey.size) {
            audiences.sendMessageI18n("to-notice-ranking-message", i + 1, Bukkit.getPlayer(finishJockey[i])?.name!!)
        }

        withContext(Dispatchers.IO) {

            if (Config.config.discordWebHook.result.isNotEmpty()) {
                sendWebHook(finishJockey, time, RaceSettingData.getOwner(raceId), raceId, suspend)
            }
        }
        Bukkit.getOnlinePlayers().forEach {
            it.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
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

    private fun sendWebHook(finishJockey: ArrayList<UUID>, time: HashMap<UUID, Int>, starter: OfflinePlayer, raceId: String, suspend: Boolean) {
        val json = JSONObject()
        json["username"] = "RaceAssist"
        json["avatar_url"] = "https://3.bp.blogspot.com/-Y3AVYVjLcPs/UYiNxIliDxI/AAAAAAAARSg/nZLIqBRUta8/s800/animal_uma.png"
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
            playerResult["value"] = String.format("%s %2d:%02d",
                Bukkit.getPlayer(finishJockey[i])?.name,
                floor((time[finishJockey[i]]!!.toDouble() / 60)).toInt(),
                time[finishJockey[i]]!! % 60)
            playerResult["inline"] = true
            result.add(playerResult)
        }
        embedsObject["fields"] = result
        embeds.add(embedsObject)
        json["embeds"] = embeds


        DiscordWebhook.sendWebHook(json.toString())
    }

    private suspend fun displayScoreboard(nowRankings: List<UUID>,
        currentDegree: HashMap<UUID, Int>,
        raceAudience: TreeSet<UUID>,
        innerCircumference: Int,
        startDegree: Int,
        goalDegree: Int,
        lap: Int,
        raceId: String) {

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

                    val playerName = RaceSettingData.getReplacement(raceId)[nowRankings[i]] ?: Bukkit.getPlayer(nowRankings[i])?.name
                    val goalDistance = (((lap - 1).toDouble() + if (goalDegree > startDegree) {
                        ((goalDegree.toDouble() - startDegree.toDouble()) / 360.0)
                    } else {
                        ((goalDegree.toDouble() + 360.0 - startDegree.toDouble()) / 360.0)
                    }) * innerCircumference.toDouble()).toInt()

                    val component = if (currentDegree[Bukkit.getPlayer(nowRankings[i])!!.uniqueId] == null) {
                        Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)
                            .append(Lang.getComponent("finished-the-race", player.locale()))
                    } else {
                        val currentDistance =
                            ((currentDegree[Bukkit.getPlayer(nowRankings[i])!!.uniqueId]!!.toDouble() - startDegree.toDouble()) / 360.0 * innerCircumference.toDouble()).toInt()

                        Lang.getComponent("scoreboard-now-ranking-and-name", player.locale(), i + 1, playerName)
                            .append(mm.deserialize("${currentDistance}m/${goalDistance}m "))
                    }

                    val displayDegree = objective.getScore(LegacyComponentSerializer.legacySection().serialize(component))
                    displayDegree.score = nowRankings.size - i
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
}

