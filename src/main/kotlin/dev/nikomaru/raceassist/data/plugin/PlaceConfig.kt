package dev.nikomaru.raceassist.data.plugin

import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.utils.OfflinePlayerSerializer
import kotlinx.serialization.Serializable
import org.bukkit.OfflinePlayer

@Serializable
abstract class PlaceConfig {
    abstract val placeType: PlaceType
    abstract val placeId: String
    abstract val placeName: String?
    abstract val placeImageUrl: String?
    abstract val owner: OfflinePlayer
    abstract val staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>
}