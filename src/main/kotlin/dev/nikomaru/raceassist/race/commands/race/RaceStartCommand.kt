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
import dev.nikomaru.raceassist.bet.commands.BetReturnCommand
import dev.nikomaru.raceassist.bet.commands.BetReturnCommand.Companion.canReturn
import dev.nikomaru.raceassist.dispatch.discord.DiscordWebhook
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.CommandUtils
import dev.nikomaru.raceassist.utils.CommandUtils.decideRanking
import dev.nikomaru.raceassist.utils.CommandUtils.displayLap
import dev.nikomaru.raceassist.utils.CommandUtils.displayScoreboard
import dev.nikomaru.raceassist.utils.CommandUtils.getAllJockeys
import dev.nikomaru.raceassist.utils.CommandUtils.getCentralPoint
import dev.nikomaru.raceassist.utils.CommandUtils.getCircuitExist
import dev.nikomaru.raceassist.utils.CommandUtils.getGoalDegree
import dev.nikomaru.raceassist.utils.CommandUtils.getLapCount
import dev.nikomaru.raceassist.utils.CommandUtils.getOwner
import dev.nikomaru.raceassist.utils.CommandUtils.getPolygon
import dev.nikomaru.raceassist.utils.CommandUtils.getRaceDegree
import dev.nikomaru.raceassist.utils.CommandUtils.getReverse
import dev.nikomaru.raceassist.utils.CommandUtils.judgeLap
import dev.nikomaru.raceassist.utils.CommandUtils.stop
import dev.nikomaru.raceassist.utils.Lang
import dev.nikomaru.raceassist.utils.RaceAudience
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import java.awt.Polygon
import java.util.*
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

@CommandMethod("ra|RaceAssist race")
class RaceStartCommand {

    @CommandPermission("RaceAssist.commands.race.start")
    @CommandMethod("start <raceId>")
    fun start(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceId: String) {
        RaceAssist.plugin.launch {
            val locale = if (sender is Player) sender.locale() else Locale.getDefault()

            if (!getCircuitExist(raceId, true) || !getCircuitExist(raceId, false)) {
                sender.sendMessage(Lang.getComponent("no-exist-race", locale))
                return@launch
            }
            val jockeys: ArrayList<Player> = ArrayList()
            getAllJockeys(raceId).forEach { jockey ->
                if (jockey.isOnline) {
                    jockeys.add(jockey as Player)
                    sender.sendMessage(Lang.getComponent("player-join", locale, jockey.name))
                } else {
                    sender.sendMessage(Lang.getComponent("player-is-offline", locale, jockey.name))
                }
            }
            if (jockeys.size < 2) {
                sender.sendMessage(Lang.getComponent("over-two-users-need", locale))
                return@launch
            }

            val centralXPoint: Int =
                getCentralPoint(raceId, true) ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            val centralYPoint: Int =
                getCentralPoint(raceId, false) ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-central-point", locale))
            val goalDegree: Int = getGoalDegree(raceId) ?: return@launch sender.sendMessage(Lang.getComponent("no-exist-goal-degree",
                locale,
                TextColor.color(NamedTextColor.YELLOW)))

            if (goalDegree % 90 != 0) {
                sender.sendMessage(Lang.getComponent("goal-degree-must-multiple-90", locale))
                return@launch
            }

            val jockeyCount = jockeys.size
            val finishJockey: ArrayList<UUID> = ArrayList<UUID>()
            val totalDegree: HashMap<UUID, Int> = HashMap()
            val beforeDegree: HashMap<UUID, Int> = HashMap()
            val currentLap: HashMap<UUID, Int> = HashMap()
            val threshold = Config.threshold!!
            val raceAudience: TreeSet<UUID> = TreeSet()
            var suspend = false

            val passBorders: HashMap<UUID, Int> = HashMap()
            val time: HashMap<UUID, Int> = HashMap()

            var insidePolygon = Polygon()
            val setInsidePolygon = async(Dispatchers.IO) {
                insidePolygon = getPolygon(raceId, true)

            }

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

            val lap = withContext(Dispatchers.IO) {
                getLapCount(raceId)
            }

            val outsidePolygon = withContext(Dispatchers.IO) {
                getPolygon(raceId, false)
            }

            val reverse = withContext(Dispatchers.IO) {
                getReverse(raceId) ?: false
            }

            val innerCircumference = withContext(Dispatchers.Default) {
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
                if (jockeys.size < 1) {
                    canReturn[raceId] = true
                    val betReturnCommand = BetReturnCommand()
                    betReturnCommand.returnBet(sender, raceId, Bukkit.getOfflinePlayer(finishJockey[0]).name.toString())
                    canReturn[raceId] = false
                    break
                }
                if (stop[raceId] == true) {
                    suspend = true
                    audiences.sendMessageI18n("suspended-race")
                    break
                }
                val iterator = jockeys.iterator()
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
                    val calculateLap = async(Dispatchers.Default) {
                        currentLap[uuid] = currentLap[uuid]!! + judgeLap(goalDegree, beforeDegree[uuid], currentDegree, threshold)
                        passBorders[uuid] = passBorders[uuid]!! + judgeLap(0, beforeDegree[uuid], currentDegree, threshold)
                        displayLap(currentLap[uuid], beforeLap, player, lap)
                        beforeDegree[uuid] = currentDegree
                        totalDegree[uuid] = currentDegree + (passBorders[uuid]!! * 360)
                    }

                    if (insidePolygon.contains(nowX, nowY) || !outsidePolygon.contains(nowX, nowY)) {
                        player.sendActionBar(Lang.getComponent("outside-the-racetrack", player.locale()))
                    }

                    calculateLap.await()

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
                val displayRanking = async(minecraft) {

                    displayScoreboard(finishJockey.plus(decideRanking(totalDegree)),
                        currentLap,
                        totalDegree,
                        raceAudience,
                        innerCircumference.roundToInt(),
                        startDegree,
                        lap)
                }
                delay(200)
                displayRanking.await()
            }
            audiences.showTitleI18n("finish-race")
            delay(2000)

            for (i in 0 until finishJockey.size) {
                audiences.sendMessageI18n("to-notice-ranking-message", i + 1, Bukkit.getPlayer(finishJockey[i])?.name!!)
            }


            withContext(Dispatchers.IO) {
                if (Config.discordWebHook != null) {
                    sendWebHook(finishJockey, time, getOwner(raceId)!!, raceId, suspend)
                }
            }
            Bukkit.getOnlinePlayers().forEach {
                it.scoreboard.clearSlot(DisplaySlot.SIDEBAR)
            }

        }
    }

    private fun sendWebHook(finishJockey: ArrayList<UUID>, time: HashMap<UUID, Int>, starter: UUID, raceId: String, suspend: Boolean) {
        val json = JSONObject()
        json["username"] = "RaceAssist"
        json["avatar_url"] = "https://3.bp.blogspot.com/-Y3AVYVjLcPs/UYiNxIliDxI/AAAAAAAARSg/nZLIqBRUta8/s800/animal_uma.png"
        val result = JSONArray()
        val embeds = JSONArray()
        val author = JSONObject()
        val embedsObject = JSONObject()
        embedsObject["title"] = if (suspend) "RaceAssist_suspend" else "RaceAssist"
        author["name"] = Lang.getText("discord-webhook-name", Locale.getDefault(), Bukkit.getOfflinePlayer(starter).name, raceId)
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

        val discordWebHook = DiscordWebhook()
        discordWebHook.sendWebHook(json.toString())
    }

}
