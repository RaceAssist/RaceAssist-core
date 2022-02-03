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

package dev.nikomaru.raceassist.race.commands.audience

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import com.github.shynixn.mccoroutine.launch
import dev.nikomaru.raceassist.RaceAssist
import dev.nikomaru.raceassist.database.PlayerList
import dev.nikomaru.raceassist.race.commands.CommandUtils.getRaceCreator
import dev.nikomaru.raceassist.utils.Lang
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

@CommandMethod("ra|RaceAssist audience")
class AudienceListCommand {

    @CommandMethod("list <raceId>")
    private fun list(sender: Player, @Argument(value = "raceId", suggestions = "raceId") raceID: String) {
        RaceAssist.plugin.launch {
            if (getRaceCreator(raceID) != sender.uniqueId) {
                sender.sendMessage(
                    Component.text(
                        Lang.getText("only-race-creator-can-display", sender.locale()),
                        TextColor.color(NamedTextColor.RED)
                    )
                )
                return@launch
            }
            sender.sendMessage(Component.text(Lang.getText("participants-list", sender.locale()), TextColor.color(NamedTextColor.GREEN)))
            newSuspendedTransaction(Dispatchers.IO) {
                PlayerList.select { PlayerList.raceID eq raceID }.forEach {
                    sender.sendMessage(
                        Component.text(
                            Bukkit.getOfflinePlayer(UUID.fromString(it[PlayerList.playerUUID])).name!!, TextColor.color(
                                NamedTextColor.GREEN
                            )
                        )
                    )
                }
            }
        }
    }
}