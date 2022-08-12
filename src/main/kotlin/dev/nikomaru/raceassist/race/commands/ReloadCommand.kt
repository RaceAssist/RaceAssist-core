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

package dev.nikomaru.raceassist.race.commands

import cloud.commandframework.annotations.*
import dev.nikomaru.raceassist.files.Config
import kotlinx.serialization.ExperimentalSerializationApi
import org.bukkit.command.CommandSender

@CommandMethod("ra|raceassist")
class ReloadCommand {

    @OptIn(ExperimentalSerializationApi::class)
    @CommandMethod("reload")
    @CommandPermission("raceassist.command.reload")
    @CommandDescription("help command")
    fun reload(sender: CommandSender) {
        Config.load()
        sender.sendMessage("Reloaded config")
    }

}