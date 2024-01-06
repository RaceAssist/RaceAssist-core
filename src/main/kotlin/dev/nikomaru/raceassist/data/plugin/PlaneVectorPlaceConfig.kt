package dev.nikomaru.raceassist.data.plugin

import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.utils.OfflinePlayerSerializer
import dev.nikomaru.raceassist.data.utils.PolygonSerializer
import kotlinx.serialization.Serializable
import org.bukkit.OfflinePlayer
import java.awt.Polygon

@Serializable
data class PlaneVectorPlaceConfig(
    override val placeType: PlaceType = PlaceType.PLANE_VECTOR,
    override val placeId: String,
    override val placeName: String?,
    override val placeImageUrl: String?,
    val inside: @Serializable(with = PolygonSerializer::class) Polygon,
    val outside: @Serializable(with = PolygonSerializer::class) Polygon,
    val calculatePolygonList: ArrayList<CalculatePolygon>,
    val image: String?,
    override val owner: @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer,
    override val staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
) : PlaceConfig()

@Serializable
data class CalculatePolygon(
    val num: Int,
    val start: Point,
    val end: Point,
    val polygon: @Serializable(with = PolygonSerializer::class) Polygon,
)

@Serializable
data class Point(
    val x: Int,
    val y: Int
)
