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

package dev.nikomaru.raceassist.horse.commands

import cloud.commandframework.annotations.*
import cloud.commandframework.annotations.specifier.Range
import org.bukkit.OfflinePlayer
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

@CommandMethod("ra|RaceAssist horse")
class BuckupCommand {

    @CommandPermission("RaceAssist.commands.horse.buckup")
    @CommandMethod("buckup <radius>")
    fun buckup(sender: CommandSender, @Argument(value = "radius") @Range(min = "1", max = "256") radius: Int) {
        if (sender !is Player) {
            sender.sendMessage("Only the player can do this.")
            return
        }
        val world = sender.location.world
        val list = world.getNearbyEntitiesByType(LivingEntity::class.java, sender.location, radius.toDouble()).stream().filter { entity ->
            entity.type == EntityType.HORSE
        }.filter { entity ->
            (entity as Horse).isTamed
        }
        list.forEach {
            val horse = it as Horse
            val owner = horse.owner
            if (owner is OfflinePlayer) {
                val uuid = horse.uniqueId
                val style = horse.style.name
                val jump = round(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.value.pow(1.7) * 5.293, 2)
                val maxhealth = round(horse.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value, 2)
                val speed = round(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!.value * 42.162962963, 2)

            }
        }
    }

    private fun round(value: Double, places: Int): Double {
        require(places >= 0)
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).toDouble()
    }
}

