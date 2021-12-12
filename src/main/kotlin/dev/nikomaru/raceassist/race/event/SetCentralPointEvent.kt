package dev.nikomaru.raceassist.race.event

import dev.nikomaru.raceassist.database.Database
import dev.nikomaru.raceassist.race.commands.PlaceCommands
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class SetCentralPointEvent : Listener {
    @EventHandler
    fun setCentralPoint(event: PlayerInteractEvent) {
        if (PlaceCommands.getCanSetCentral()[event.player.uniqueId] == null || PlaceCommands.getCanSetCentral()[event.player.uniqueId] != true) {
            return
        }
        if (event.action != Action.LEFT_CLICK_BLOCK) {
            return
        }
        try {
            val connection = Database.connection ?: return
            val statement =
                connection.prepareStatement("UPDATE RaceList SET CentralXPoint= ? , CentralYPoint = ? WHERE RaceID = ?")
            statement.setInt(1, event.clickedBlock?.location?.blockX ?: 0)
            statement.setInt(2, event.clickedBlock?.location?.blockZ ?: 0)
            statement.setString(3, PlaceCommands.getCentralRaceID()[event.player.uniqueId])
            statement.execute()
            statement.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        event.player.sendMessage("§a中心を設定しました")
        PlaceCommands.removeCanSetCentral(event.player.uniqueId)
    }
}