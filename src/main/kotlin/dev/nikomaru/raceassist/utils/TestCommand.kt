/*
 * Copyright © 2021-2024 Nikomaru <nikomaru@nikomaru.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.nikomaru.raceassist.utils

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import org.bukkit.command.CommandSender

@CommandMethod("ra-test")
class TestCommand {

    @CommandMethod("display <x> <y> <z> <mills>")
    suspend fun display(
        sender: CommandSender,
        @Argument(value = "x") x: Double,
        @Argument(value = "y") y: Double,
        @Argument(value = "z") z: Double,
        @Argument(value = "mills") mills: Long
    ) {
        if (sender !is org.bukkit.entity.Player) return
//        LuminescenceShulker.display(Location(sender.world, x, y, z), mills, arrayListOf(sender))

    }
}