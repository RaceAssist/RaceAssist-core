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

package dev.nikomaru.raceassist.utils

import dev.nikomaru.raceassist.data.files.PlaceSettingData
import dev.nikomaru.raceassist.data.files.RaceSettingData
import dev.nikomaru.raceassist.data.files.StaffSettingData.existStaff
import dev.nikomaru.raceassist.utils.coroutines.async
import kotlinx.coroutines.*
import net.kyori.adventure.title.Title.title
import org.bukkit.*
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.awt.image.BufferedImage
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.*

object Utils {

    val audience: HashMap<String, ArrayList<UUID>> = HashMap()
    val canSetInsideCircuit = HashMap<UUID, Boolean>()
    val canSetOutsideCircuit = HashMap<UUID, Boolean>()
    val circuitRaceId = HashMap<UUID, String>()
    val canSetCentral = HashMap<UUID, Boolean>()
    val centralRaceId = HashMap<UUID, String>()
    var stop = HashMap<String, Boolean>()
    lateinit var mapColor: Properties

    suspend fun getInsideRaceExist(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        PlaceSettingData.getInsidePolygon(raceId).npoints > 0
    }

    suspend fun displayLap(currentLap: Int?, beforeLap: Int?, player: Player, lap: Int) {
        if (currentLap == null || beforeLap == null) {
            return
        }
        if (currentLap == lap) {
            return
        }
        val count: Long = 2000
        withContext(Dispatchers.async) {
            if (currentLap > beforeLap) {
                if (currentLap == lap - 1) {
                    player.showTitle(title((Lang.getComponent("last-lap", player.locale())),
                        Lang.getComponent("one-step-forward-lap", player.locale())))
                } else {
                    player.showTitle(title(Lang.getComponent("now-lap", player.locale(), currentLap, lap),
                        Lang.getComponent("one-step-forward-lap", player.locale())))
                }
                delay(count)
                player.clearTitle()
            } else if (currentLap < beforeLap) {
                player.showTitle(title(Lang.getComponent("now-lap", player.locale(), currentLap, lap),
                    Lang.getComponent("one-step-backwards-lap", player.locale())))
                delay(count)
                player.clearTitle()
            }
        }
    }

    private fun getBlockColor(x: Int, z: Int, world: World): String {
        val block = world.getHighestBlockAt(x, z, HeightMap.WORLD_SURFACE)
        val key = block.blockData.material.key.asString()
        val color = mapColor.getProperty(key) ?: "#000000"
        if (color == "#000000") {
            println(key)
        }
        return color
    }

    suspend fun createImage(x1: Int, x2: Int, y1: Int, y2: Int): String = withContext(Dispatchers.IO) {
        val image = BufferedImage(abs(x1 - x2) + 9, abs(y1 - y2) + 9, BufferedImage.TYPE_3BYTE_BGR)

        val sizeX = abs(x1 - x2) + 8
        val sizeY = abs(y1 - y2) + 8
        for (x in min(x1, x2) - 4..max(x1, x2) + 4) {
            for (y in min(y1, y2) - 4..max(y1, y2) + 4) {
                val relectiveX = sizeX - (x - (min(x1, x2) - 4))
                val relectiveY = sizeY - (y - (min(y1, y2) - 4))
                val hex = getBlockColor(x, y, Bukkit.getWorld("world")!!).replace("#", "")
                val rgb = hex.toInt(16)
                image.setRGB(relectiveX, relectiveY, rgb)
            }
        }

        val baos = ByteArrayOutputStream()
        val bos = BufferedOutputStream(baos)
        withContext(Dispatchers.IO) {
            ImageIO.write(image, "png", bos)
            bos.flush()
            bos.close()
        }
        return@withContext Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    suspend fun returnCanRaceSetting(raceId: String, player: CommandSender) = withContext(Dispatchers.IO) {
        if (player is ConsoleCommandSender) {
            return@withContext true
        }
        (player as Player)
        if (!RaceSettingData.existsRace(raceId)) {
            player.sendMessage(Lang.getComponent("no-exist-this-raceid-race", player.locale(), raceId))
            return@withContext true
        }
        if (!existStaff(raceId, player)) {
            player.sendMessage(Lang.getComponent("only-race-creator-can-setting", player.locale()))
            return@withContext true
        }
        return@withContext false
    }

    fun judgeLap(goalDegree: Int, beforeDegree: Int?, currentDegree: Int?, threshold: Int): Int {
        if (currentDegree == null) return 0
        when (goalDegree) {
            0 -> {
                if ((beforeDegree in 360 - threshold until 360) && (currentDegree in 0 until threshold)) {
                    return 1
                }
                if ((beforeDegree in 0 until threshold) && (currentDegree in 360 - threshold until 360)) {
                    return -1
                }
            }

            90 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return 1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return -1
                }
            }

            180 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return 1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return -1
                }
            }

            270 -> {
                if ((beforeDegree in goalDegree - threshold until goalDegree) && (currentDegree in goalDegree until goalDegree + threshold)) {
                    return 1
                }
                if ((beforeDegree in goalDegree until goalDegree + threshold) && (currentDegree in goalDegree - threshold until goalDegree)) {
                    return -1
                }
            }
        }
        return 0
    }

    fun getRaceDegree(Y: Double, X: Double): Int {
        val degree = Math.toDegrees(atan2(Y, X)).toInt()
        return if (degree < 0) {
            360 + degree
        } else {
            degree
        }
    }

    suspend fun getPolygon(raceId: String, inside: Boolean) = newSuspendedTransaction(Dispatchers.IO) {
        if (inside) {
            PlaceSettingData.getInsidePolygon(raceId)
        } else {
            PlaceSettingData.getOutsidePolygon(raceId)
        }
    }

    fun CommandSender.locale(): Locale {
        return if (this is Player) this.locale() else Locale.getDefault()
    }

    suspend fun getCentralPoint(raceId: String, xPoint: Boolean): Int? = newSuspendedTransaction(Dispatchers.IO) {
        if (xPoint) {
            PlaceSettingData.getCentralXPoint(raceId)
        } else {
            PlaceSettingData.getCentralYPoint(raceId)
        }
    }

    fun String.toUUID(): UUID {
        return UUID.fromString(this)
    }

    fun UUID.toOfflinePlayer(): OfflinePlayer {
        return Bukkit.getOfflinePlayer(this)
    }

    fun passwordHash(string: String): String {
        val sha3 = MessageDigest.getInstance("SHA3-256")
        val sha3Result = sha3.digest(string.toByteArray())
        return String.format("%040x", BigInteger(1, sha3Result))
    }

}