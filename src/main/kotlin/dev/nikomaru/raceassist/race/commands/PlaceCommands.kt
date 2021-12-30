/*
 * Copyright © 2021 Nikomaru <nikomaru@nikomaru.dev>
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
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.RaceCommand.Companion.getCentralPoint
import dev.nikomaru.raceassist.race.commands.RaceCommand.Companion.getReverse
import dev.nikomaru.raceassist.race.utils.InsideCircuit
import dev.nikomaru.raceassist.race.utils.OutsideCircuit
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*
import kotlin.math.atan2

@CommandAlias("ra|RaceAssist")
@Subcommand("place")
class PlaceCommands : BaseCommand() {

    @CommandPermission("RaceAssist.commands.place")
    @Subcommand("reverse")
    @CommandCompletion("@raceID")
    fun reverse(sender: CommandSender, @Single raceID: String) {
        val player = sender as Player
        if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        val nowDirection = getDirection(raceID)

        transaction {
            RaceList.update({ RaceList.raceID eq raceID }) {
                it[reverse] = !nowDirection
            }
        }
        sender.sendMessage(text("レースの向きを変更しました", TextColor.color(GREEN)))
    }

    @CommandPermission("RaceAssist.commands.place")
    @Subcommand("central")
    @CommandCompletion("@raceID")
    fun central(sender: CommandSender, @Single raceID: String) {
        val player = sender as Player
        if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        canSetCentral[player.uniqueId] = true
        centralRaceID[player.uniqueId] = raceID
        player.sendMessage(text("中心点を設定してください", TextColor.color(GREEN)))
    }

    @CommandPermission("RaceAssist.commands.place")
    @Subcommand("degree")
    @CommandCompletion("@raceID")
    fun degree(sender: CommandSender, @Single raceID: String) {
        val player = sender as Player
        if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        val centralXPoint = getCentralPoint(raceID, true) ?: return sender.sendMessage(text("中心点が設定されていません", TextColor.color(RED)))
        val centralYPoint = getCentralPoint(raceID, false) ?: return sender.sendMessage(text("中心点が設定されていません", TextColor.color(RED)))
        val reverse = getReverse(raceID) ?: return sender.sendMessage(text("reverseが設定されていません", TextColor.color(RED)))
        var nowX = player.location.blockX - centralXPoint
        val nowY = player.location.blockZ - centralYPoint
        if (reverse) {
            nowX = -nowX
        }
        val currentDegree = if (Math.toDegrees(atan2(nowY.toDouble(), nowX.toDouble())).toInt() < 0) {
            360 + Math.toDegrees(atan2(nowY.toDouble(), nowX.toDouble())).toInt()
        } else {
            Math.toDegrees(atan2(nowY.toDouble(), nowX.toDouble())).toInt()
        }
        var degree = 0
        when (currentDegree) {
            in 0..45    -> {
                player.sendMessage(text("0度にしました", TextColor.color(GREEN)))
                degree = 0
            }
            in 46..135  -> {
                player.sendMessage(text("90度にしました", TextColor.color(GREEN)))
                degree = 90
            }
            in 136..225 -> {
                player.sendMessage(text("180度にしました", TextColor.color(GREEN)))
                degree = 180
            }
            in 226..315 -> {
                player.sendMessage(text("270度にしました", TextColor.color(GREEN)))
                degree = 270
            }
            in 316..360 -> {
                player.sendMessage(text("0度にしました", TextColor.color(GREEN)))
                degree = 0
            }
        }
        transaction {
            RaceList.update({ RaceList.raceID eq raceID }) {
                it[goalDegree] = degree
            }
        }
    }

    @CommandPermission("RaceAssist.commands.place")
    @Subcommand("lap")
    @CommandCompletion("@raceID @lap")
    @Syntax("[RaceID] <lap>")
    fun setLap(sender: CommandSender, @Single raceID: String, @Single lap: Int) {
        val player = sender as Player
        if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        if (lap < 1) {
            player.sendMessage(text("1以上の数字を入力してください", TextColor.color(RED)))
            return
        }
        transaction {
            RaceList.update({ RaceList.raceID eq raceID }) {
                it[this.lap] = lap
            }
        }
        player.sendMessage(text("ラップ数を設定しました", TextColor.color(GREEN)))
    }

    @CommandPermission("RaceAssist.commands.place")
    @Subcommand("set")
    @CommandCompletion("@RaceID in|out")
    fun set(sender: CommandSender, @Single raceID: String, @Single type: String) {
        val player = sender as Player

        if (RaceCommand.getRaceCreator(raceID) == null) {
            player.sendMessage(text("レースが存在しません", TextColor.color(RED)))
            return
        } else if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
            player.sendMessage(text("他人のレースは設定できません", TextColor.color(RED)))
            return
        }
        if (canSetOutsideCircuit[player.uniqueId] != null || canSetInsideCircuit[player.uniqueId] != null) {
            player.sendMessage("すでに設定モードになっています")
            return
        }
        if (type == "in") {
            canSetInsideCircuit[player.uniqueId] = true
            player.sendMessage(text("内側のコース設定モードになりました", TextColor.color(GREEN)))
        } else if (type == "out") {
            val insideCircuitExist: Boolean = getInsideRaceExist(raceID)
            if (!insideCircuitExist) {
                player.sendMessage("内側のコースが設定されていません")
                return
            }
            canSetOutsideCircuit[player.uniqueId] = true
            player.sendMessage(text("外側のコース設定モードになりました", TextColor.color(GREEN)))
        }
        circuitRaceID[player.uniqueId] = raceID
        player.sendMessage(text("左クリックで設定を開始し,右クリックで中断します", TextColor.color(GREEN)))
        player.sendMessage("設定を終了する場合は/KeibaAssist race circuit finish と入力してください")
    }

    @CommandPermission("RaceAssist.commands.place")
    @Subcommand("finish")
    fun finish(sender: CommandSender) {
        val player = sender as Player
        if (Objects.isNull(canSetOutsideCircuit[player.uniqueId]) && Objects.isNull(canSetInsideCircuit[player.uniqueId])) {
            player.sendMessage("設定にあなたは現在なっていません")
            return
        }
        if (Objects.nonNull(canSetInsideCircuit[player.uniqueId])) {
            canSetInsideCircuit.remove(player.uniqueId)
            InsideCircuit.finish(player)
            player.sendMessage("内側のコース設定を終了しました")
        }
        if (Objects.nonNull(canSetOutsideCircuit[player.uniqueId])) {
            canSetOutsideCircuit.remove(player.uniqueId)
            OutsideCircuit.finish(player)
            player.sendMessage("外側のコース設定を終了しました")
        }
    }

    private fun getDirection(raceID: String): Boolean {
        var direction = false
        transaction {
            direction = RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.reverse) == true
        }
        return direction
    }

    private fun getInsideRaceExist(raceID: String): Boolean {
        var existRaceInside = false
        transaction {
            existRaceInside = CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq true) }.count() > 0
        }
        return existRaceInside
    }

    companion object {
        private var canSetInsideCircuit = HashMap<UUID, Boolean>()
        private var canSetOutsideCircuit = HashMap<UUID, Boolean>()
        private var circuitRaceID = HashMap<UUID, String>()
        private var canSetCentral = HashMap<UUID, Boolean>()
        private var centralRaceID = HashMap<UUID, String>()

        fun getCanSetInsideCircuit(): HashMap<UUID, Boolean> {
            return canSetInsideCircuit
        }

        fun getCanSetOutsideCircuit(): HashMap<UUID, Boolean> {
            return canSetOutsideCircuit
        }

        fun getCircuitRaceID(): HashMap<UUID, String> {
            return circuitRaceID
        }

        fun putCanSetInsideCircuit(uniqueId: UUID, b: Boolean) {
            canSetInsideCircuit[uniqueId] = b
        }

        fun putCanSetOutsideCircuit(uniqueId: UUID, b: Boolean) {
            canSetOutsideCircuit[uniqueId] = b
        }

        fun removeCanSetInsideCircuit(uniqueId: UUID) {
            canSetInsideCircuit.remove(uniqueId)
        }

        fun removeCanSetOutsideCircuit(uniqueId: UUID) {
            canSetOutsideCircuit.remove(uniqueId)
        }

        fun getCanSetCentral(): HashMap<UUID, Boolean> {
            return canSetCentral
        }

        fun removeCanSetCentral(uniqueId: UUID) {
            canSetCentral.remove(uniqueId)
        }

        fun getCentralRaceID(): HashMap<UUID, String> {
            return centralRaceID
        }
    }
}