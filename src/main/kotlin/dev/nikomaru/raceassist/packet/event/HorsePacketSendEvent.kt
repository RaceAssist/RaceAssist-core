package dev.nikomaru.raceassist.packet.event

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import dev.nikomaru.raceassist.RaceAssist.Companion.plugin
import dev.nikomaru.raceassist.RaceAssist.Companion.protocolManager


class HorsePacketSendEvent {
    init {
        protocolManager.addPacketListener(object : PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK
        ) {
            override fun onPacketSending(event: PacketEvent) {

            }
        })


    }
}