package dev.nikomaru.raceassist.horse.events

import dev.nikomaru.raceassist.horse.utlis.HorseUtils
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTameEvent

class HorseTamedEvent : Listener {
    @EventHandler
    suspend fun horseTamedEvent(event: EntityTameEvent) {
        val entity = event.entity
        if (entity.type != EntityType.HORSE) {
            return
        }
        val horse = entity as Horse
        HorseUtils.updateKilledHorse(horse)
        return
    }

}