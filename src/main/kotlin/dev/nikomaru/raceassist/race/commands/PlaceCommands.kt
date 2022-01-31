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
import dev.nikomaru.raceassist.database.CircuitPoint
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.RaceCommand.Companion.getCentralPoint
import dev.nikomaru.raceassist.race.commands.RaceCommand.Companion.getReverse
import dev.nikomaru.raceassist.race.utils.InsideCircuit
import dev.nikomaru.raceassist.race.utils.OutsideCircuit
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.*
import kotlin.math.atan2

@CommandAlias("ra|RaceAssist")
@Subcommand("place")
@CommandPermission("RaceAssist.commands.place")
class PlaceCommands : BaseCommand() {

    @Subcommand("reverse")
    @CommandCompletion("@RaceID")
    fun reverse(sender: Player, @Single raceID: String) {
        plugin.launch {
            if (RaceCommand.getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(text(Lang.getText("only-race-creator-can-setting", sender.locale()), TextColor.color(RED)))
                return@launch
            }
            val nowDirection = getDirection(raceID)

            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.update({ RaceList.raceID eq raceID }) {
                    it[reverse] = !nowDirection
                }
            }
            sender.sendMessage(text(Lang.getText("to-change-race-orientation", sender.locale()), TextColor.color(GREEN)))
        }
    }

    @Subcommand("central")
    @CommandCompletion("@RaceID")
    fun central(sender: CommandSender, @Single raceID: String) {
        val player = sender as Player
        plugin.launch {
            if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(text(Lang.getText("only-race-creator-can-setting", sender.locale()), TextColor.color(RED)))
                return@launch
            }
        }
        canSetCentral[player.uniqueId] = true
        centralRaceID[player.uniqueId] = raceID
        player.sendMessage(text(Lang.getText("to-set-central-point", sender.locale()), TextColor.color(GREEN)))
    }

    @Subcommand("degree")
    @CommandCompletion("@RaceID")
    fun degree(player: Player, @Single raceID: String) {
        plugin.launch {
            if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(text(Lang.getText("only-race-creator-can-setting", player.locale()), TextColor.color(RED)))
                return@launch
            }
            val centralXPoint = getCentralPoint(raceID, true) ?: return@launch player.sendMessage(
                text(
                    Lang.getText("no-exist-central-point", player.locale()), TextColor.color
                        (RED)
                )
            )
            val centralYPoint = getCentralPoint(raceID, false) ?: return@launch player.sendMessage(
                text(
                    Lang.getText("no-exist-central-point", player.locale()), TextColor.color
                        (RED)
                )
            )
            val reverse =
                getReverse(raceID) ?: return@launch player.sendMessage(
                    text(
                        Lang.getText("orientation-is-not-set", player.locale()),
                        TextColor.color(RED)
                    )
                )
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
                in 0..45 -> {
                    player.sendMessage(text(Lang.getText("to-set-0-degree", player.locale()), TextColor.color(GREEN)))
                    degree = 0
                }
                in 46..135 -> {
                    player.sendMessage(text(Lang.getText("to-set-90-degree", player.locale()), TextColor.color(GREEN)))
                    degree = 90
                }
                in 136..225 -> {
                    player.sendMessage(text(Lang.getText("to-set-180-degree", player.locale()), TextColor.color(GREEN)))
                    degree = 180
                }
                in 226..315 -> {
                    player.sendMessage(text(Lang.getText("to-set-270-degree", player.locale()), TextColor.color(GREEN)))
                    degree = 270
                }
                in 316..360 -> {
                    player.sendMessage(text(Lang.getText("to-set-0-degree", player.locale()), TextColor.color(GREEN)))
                    degree = 0
                }
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.update({ RaceList.raceID eq raceID }) {
                    it[goalDegree] = degree
                }
            }
        }
    }

    @Subcommand("lap")
    @CommandCompletion("@RaceID @lap")
    @Syntax("[RaceID] <lap>")
    fun setLap(sender: CommandSender, @Single raceID: String, @Single lap: Int) {
        val player = sender as Player
        plugin.launch {
            if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(text(Lang.getText("only-race-creator-can-setting", sender.locale()), TextColor.color(RED)))
                return@launch
            }

            if (lap < 1) {
                player.sendMessage(text(Lang.getText("to-need-enter-over-1", sender.locale()), TextColor.color(RED)))
                return@launch
            }
            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.update({ RaceList.raceID eq raceID }) {
                    it[this.lap] = lap
                }
            }
            player.sendMessage(text(Lang.getText("to-set-lap", sender.locale()), TextColor.color(GREEN)))
        }
    }

    @Subcommand("set")
    @CommandCompletion("@RaceID in|out")
    fun set(player: Player, @Single raceID: String, @Single type: String) {
        plugin.launch {

            if (RaceCommand.getRaceCreator(raceID) == null) {
                player.sendMessage(text(Lang.getText("no-exist-race", player.locale()), TextColor.color(RED)))
                return@launch
            } else if (RaceCommand.getRaceCreator(raceID) != player.uniqueId) {
                player.sendMessage(text(Lang.getText("only-race-creator-can-setting", player.locale()), TextColor.color(RED)))
                return@launch
            }

            if (canSetOutsideCircuit[player.uniqueId] != null || canSetInsideCircuit[player.uniqueId] != null) {
                player.sendMessage(Lang.getText("already-setting-mode", player.locale()))
                return@launch
            }
            if (type == "in") {
                canSetInsideCircuit[player.uniqueId] = true
                player.sendMessage(text(Lang.getText("to-be-inside-set-mode", player.locale()), TextColor.color(GREEN)))
            } else if (type == "out") {

                if (!getInsideRaceExist(raceID)) {
                    player.sendMessage(Lang.getText("no-inside-course-set", player.locale()))
                    return@launch
                }
                canSetOutsideCircuit[player.uniqueId] = true
                player.sendMessage(text(Lang.getText("to-be-outside-set-mode", player.locale()), TextColor.color(GREEN)))
            }
            circuitRaceID[player.uniqueId] = raceID
            player.sendMessage(text(Lang.getText("to-click-left-start-right-finish", player.locale()), TextColor.color(GREEN)))
            player.sendMessage(Lang.getText("to-enter-finish-message", player.locale()))
        }
    }

    @Subcommand("finish")
    fun finish(sender: CommandSender) {
        plugin.launch {
            val player = sender as Player
            if (Objects.isNull(canSetOutsideCircuit[player.uniqueId]) && Objects.isNull(canSetInsideCircuit[player.uniqueId])) {
                player.sendMessage(Lang.getText("now-you-not-setting-mode", sender.locale()))
                return@launch
            }
            if (Objects.nonNull(canSetInsideCircuit[player.uniqueId])) {
                canSetInsideCircuit.remove(player.uniqueId)
                InsideCircuit.finish(player)
                player.sendMessage(Lang.getText("to-finish-inside-course-setting", sender.locale()))
            }
            if (Objects.nonNull(canSetOutsideCircuit[player.uniqueId])) {
                canSetOutsideCircuit.remove(player.uniqueId)
                OutsideCircuit.finish(player)
                player.sendMessage(Lang.getText("to-finish-outside-course-setting", sender.locale()))
            }
        }
    }

    private suspend fun getDirection(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceList.select { RaceList.raceID eq raceID }.firstOrNull()?.get(RaceList.reverse) == true
    }

    private suspend fun getInsideRaceExist(raceID: String) = newSuspendedTransaction(Dispatchers.IO) {
        CircuitPoint.select { (CircuitPoint.raceID eq raceID) and (CircuitPoint.inside eq true) }.count() > 0
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