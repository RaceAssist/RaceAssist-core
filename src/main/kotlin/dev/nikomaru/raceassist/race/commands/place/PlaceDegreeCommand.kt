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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.data.files.PlaceSettingData
import dev.nikomaru.raceassist.data.files.PlaceSettingData.getReverse
import dev.nikomaru.raceassist.data.files.RaceUtils
import dev.nikomaru.raceassist.utils.Utils.getCentralPoint
import dev.nikomaru.raceassist.utils.Utils.getRaceDegree
import dev.nikomaru.raceassist.utils.i18n.Lang
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandMethod("ra|RaceAssist place")
class PlaceDegreeCommand {
    @CommandPermission("raceassist.commands.place.degree")
    @CommandMethod("degree <operatePlaceId>")
    suspend fun degree(sender: CommandSender, @Argument(value = "operatePlaceId", suggestions = "operatePlaceId") placeId: String) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }

        if (!RaceUtils.hasRaceControlPermission(placeId, sender)) return
        val centralXPoint = getCentralPoint(placeId, true) ?: return sender.sendMessage(Lang.getComponent("no-exist-central-point", sender.locale()))
        val centralYPoint = getCentralPoint(placeId, false) ?: return sender.sendMessage(Lang.getComponent("no-exist-central-point", sender.locale()))
        val reverse = getReverse(placeId)
        val nowX = sender.location.blockX
        val nowY = sender.location.blockZ
        val relativeNowX = if (!reverse) nowX - centralXPoint else -1 * (nowX - centralXPoint)
        val relativeNowY = nowY - centralYPoint

        val degree = when (getRaceDegree(relativeNowY.toDouble(), relativeNowX.toDouble())) {
            in 0..45 -> {
                0
            }

            in 46..135 -> {
                90
            }

            in 136..225 -> {
                180
            }

            in 226..315 -> {
                270
            }

            in 316..360 -> {
                0
            }

            else -> {
                0
            }
        }
        sender.sendMessage(Lang.getComponent("to-set-degree", sender.locale(), degree))
        PlaceSettingData.setGoalDegree(placeId, degree)

    }
}