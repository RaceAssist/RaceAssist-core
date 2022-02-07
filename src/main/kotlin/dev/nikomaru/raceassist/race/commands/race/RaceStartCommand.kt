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
import dev.nikomaru.raceassist.dispatch.discord.DiscordWebhook
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.race.commands.CommandUtils
import dev.nikomaru.raceassist.race.commands.CommandUtils.decideRanking
import dev.nikomaru.raceassist.race.commands.CommandUtils.displayLap
import dev.nikomaru.raceassist.race.commands.CommandUtils.displayScoreboard
import dev.nikomaru.raceassist.race.commands.CommandUtils.getAllJockeys
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
import dev.nikomaru.raceassist.utils.RaceAudience
import dev.nikomaru.raceassist.utils.coroutines.async
import dev.nikomaru.raceassist.utils.coroutines.minecraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
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
import java.text.MessageFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

@CommandMethod("ra|RaceAssist race")
class RaceStartCommand {

    @CommandPermission("RaceAssist.commands.race.start")
    @CommandMethod("start <raceId>")
    fun start(sender: CommandSender, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {

            if (starting) {
                sender.sendMessage(Component.text(Lang.getText("now-starting-other-race", (sender as Player).locale()),
                    TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            if (getRaceCreator(raceID) != (sender as Player).uniqueId) {
                sender.sendMessage(Component.text(Lang.getText("only-race-creator-can-start", sender.locale()), TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            if (!getCircuitExist(raceID, true) || !getCircuitExist(raceID, false)) {
                sender.sendMessage(Component.text(Lang.getText("no-exist-race", sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
                return@launch
            }
            val jockeys: ArrayList<Player> = ArrayList()
            getAllJockeys(raceID).forEach { jockey ->
                if (jockey.isOnline) {
                    jockeys.add(jockey as Player)
                    sender.sendMessage(Component.text(MessageFormat.format(Lang.getText("player-join", sender.locale()), jockey.name),
                        TextColor.color(NamedTextColor.GREEN)))
                } else {
                    sender.sendMessage(Component.text(MessageFormat.format(Lang.getText("player-is-offline", sender.locale()), jockey.name),
                        TextColor.color(NamedTextColor.YELLOW)))
                }
            }
            if (jockeys.size < 2) {
                sender.sendMessage(Component.text(Lang.getText("over-two-users-need", sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
                return@launch
            }

            val centralXPoint: Int =
                getCentralPoint(raceID, true) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                    sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
            val centralYPoint: Int =
                getCentralPoint(raceID, false) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                    sender.locale()), TextColor.color(NamedTextColor.YELLOW)))
            val goalDegree: Int =
                getGoalDegree(raceID) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-goal-degree", sender.locale()),
                    TextColor.color(NamedTextColor.YELLOW)))

            if (goalDegree % 90 != 0) {
                sender.sendMessage(Component.text(Lang.getText("goal-degree-must-multiple-90", sender.locale()),
                    TextColor.color(NamedTextColor.YELLOW)))
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


            CommandUtils.audience[raceID]?.forEach {
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
                audiences.showTitle(Title.title(Component.text("${5 - timer1}", TextColor.color(NamedTextColor.GREEN)), Component.text(" ")))
                delay(1000)
                timer1++
            }

            val lap = getLap.await()
            val outsidePolygon = setOutsidePolygon.await()
            val reverse = setReverse.await()
            val innerCircumference = calculateCircumference.await()

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
                        player.sendActionBar(Component.text(Lang.getText("outside-the-racetrack", player.locale()),
                            TextColor.color(NamedTextColor.RED)))
                    }

                    calculateLap.await()

                    if (currentLap[uuid]!! >= lap) {
                        iterator.remove()
                        finishJockey.add(uuid)
                        totalDegree.remove(uuid)
                        currentLap.remove(uuid)
                        player.showTitle(Title.title(Component.text(MessageFormat.format(Lang.getText("player-ranking", player.locale()),
                            jockeyCount - jockeys.size,
                            jockeyCount), TextColor.color(NamedTextColor.GREEN)), Component.text("")))
                        time[uuid] = ((System.currentTimeMillis() - beforeTime) / 1000).toInt()
                        continue
                    }
                }
                val displayRanking = async(Dispatchers.minecraft) {

                    displayScoreboard(finishJockey.plus(decideRanking(totalDegree)),
                        currentLap,
                        totalDegree,
                        raceAudience,
                        innerCircumference.roundToInt(),
                        startDegree,
                        lap)
                }
                delay(100)
                displayRanking.await()
            }
            audiences.showTitleI18n("finish-race")
            delay(2000)

            for (i in 0 until finishJockey.size) {
                audiences.sendMessageI18n("to-notice-ranking-message", i + 1, Bukkit.getPlayer(finishJockey[i])?.name!!)
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