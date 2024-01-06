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

package dev.nikomaru.raceassist.data.plugin

import dev.nikomaru.raceassist.api.core.PlaceType
import dev.nikomaru.raceassist.data.utils.OfflinePlayerSerializer
import dev.nikomaru.raceassist.data.utils.PolygonSerializer
import kotlinx.serialization.Serializable
import org.bukkit.OfflinePlayer
import java.awt.Polygon

@Serializable
data class PlainPlaceConfig(
    override val placeType: PlaceType = PlaceType.PLAIN,
    override val placeId: String,
    override val placeName: String?,
    override val placeImageUrl: String?,
    val centralX: Int?,
    val centralY: Int?,
    val goalDegree: Int,
    val reverse: Boolean,
    val inside: @Serializable(with = PolygonSerializer::class) Polygon,
    val outside: @Serializable(with = PolygonSerializer::class) Polygon,
    val image: String?,
    override val owner: @Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer,
    override val staff: ArrayList<@Serializable(with = OfflinePlayerSerializer::class) OfflinePlayer>,
) : PlaceConfig()