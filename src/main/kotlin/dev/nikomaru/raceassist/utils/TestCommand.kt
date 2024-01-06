package dev.nikomaru.raceassist.utils

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandMethod
import dev.nikomaru.raceassist.utils.display.LuminescenceShulker
import org.bukkit.Location
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