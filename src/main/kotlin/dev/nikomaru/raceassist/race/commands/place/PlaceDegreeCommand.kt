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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.CommandUtils.getCentralPoint
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceDegree
import dev.nikomaru.raceassist.race.commands.CommandUtils.getReverse
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@CommandMethod("ra|RaceAssist place")
class PlaceDegreeCommand {
    @CommandPermission("RaceAssist.commands.place.degree")
    @CommandMethod("degree <raceId>")
    fun degree(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(Component.text(Lang.getText("only-race-creator-can-setting", sender.locale()),
                    TextColor.color(NamedTextColor.RED)))
                return@launch
            }
            val centralXPoint =
                getCentralPoint(raceID, true) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                    sender.locale()), TextColor.color(NamedTextColor.RED)))
            val centralYPoint =
                getCentralPoint(raceID, false) ?: return@launch sender.sendMessage(Component.text(Lang.getText("no-exist-central-point",
                    sender.locale()), TextColor.color(NamedTextColor.RED)))
            val reverse =
                getReverse(raceID) ?: return@launch sender.sendMessage(Component.text(Lang.getText("orientation-is-not-set", sender.locale()),
                    TextColor.color(NamedTextColor.RED)))
            val nowX = sender.location.blockX
            val nowY = sender.location.blockZ
            val relativeNowX = if (!reverse) nowX - centralXPoint else -1 * (nowX - centralXPoint)
            val relativeNowY = nowY - centralYPoint
            val currentDegree = getRaceDegree(relativeNowY.toDouble(), relativeNowX.toDouble())

            sender.sendMessage("Degree: $currentDegree")
            var degree = 0
            when (currentDegree) {
                in 0..45 -> {
                    sender.sendMessage(Component.text(Lang.getText("to-set-0-degree", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
                    degree = 0
                }
                in 46..135 -> {
                    sender.sendMessage(Component.text(Lang.getText("to-set-90-degree", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
                    degree = 90
                }
                in 136..225 -> {
                    sender.sendMessage(Component.text(Lang.getText("to-set-180-degree", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
                    degree = 180
                }
                in 226..315 -> {
                    sender.sendMessage(Component.text(Lang.getText("to-set-270-degree", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
                    degree = 270
                }
                in 316..360 -> {
                    sender.sendMessage(Component.text(Lang.getText("to-set-0-degree", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
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
}