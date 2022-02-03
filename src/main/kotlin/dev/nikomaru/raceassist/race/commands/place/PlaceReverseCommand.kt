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

package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.RaceList
import dev.nikomaru.raceassist.race.commands.CommandUtils.getDirection
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@CommandMethod("ra|RaceAssist place")
class PlaceReverseCommand {
    @CommandMethod("reverse <raceId>")
    fun reverse(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {

        RaceAssist.plugin.launch {
            if (getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(
                    Component.text(
                        Lang.getText("only-race-creator-can-setting", sender.locale()),
                        TextColor.color(NamedTextColor.RED)
                    )
                )
                return@launch
            }
            val nowDirection = getDirection(raceID)

            newSuspendedTransaction(Dispatchers.IO) {
                RaceList.update({ RaceList.raceID eq raceID }) {
                    it[reverse] = !nowDirection
                }
            }
            sender.sendMessage(Component.text(Lang.getText("to-change-race-orientation", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
        }
    }

}