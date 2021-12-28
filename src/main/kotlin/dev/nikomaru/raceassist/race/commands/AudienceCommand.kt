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
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.Subcommand
import dev.nikomaru.raceassist.database.RaceList
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

@CommandAlias("ra|RaceAssist")
@Subcommand("Audience")
class AudienceCommand : BaseCommand() {

    @Subcommand("join")
    @CommandCompletion("@RaceID")
    private fun join(sender: CommandSender, raceID: String) {
        if (!getRaceExist(raceID)) {
            sender.sendMessage(text("そのレースは見つかりません", TextColor.color(RED)))
            return
        }
        if (!audience.containsKey(raceID)) {
            audience[raceID] = ArrayList()
        }
        audience[raceID]?.add((sender as Player).uniqueId)
        sender.sendMessage(text("参加しました", TextColor.color(GREEN)))
    }

    @Subcommand("leave")
    @CommandCompletion("@RaceID")
    private fun leave(sender: CommandSender, raceID: String) {
        if (audience[raceID]?.contains((sender as Player).uniqueId) == false) {
            sender.sendMessage(text("参加していません", TextColor.color(RED)))
            return
        }
        audience[raceID]?.remove((sender as Player).uniqueId)
        sender.sendMessage(text("退出しました", TextColor.color(GREEN)))
    }

    private fun getRaceExist(raceID: String): Boolean {
        var raceExist = false
        transaction {
            addLogger(StdOutSqlLogger)
            raceExist = RaceList.select { RaceList.raceID eq raceID }.count() > 0
        }
        return raceExist
    }

    companion object {
        val audience: HashMap<String, ArrayList<UUID>> = HashMap()
    }
}