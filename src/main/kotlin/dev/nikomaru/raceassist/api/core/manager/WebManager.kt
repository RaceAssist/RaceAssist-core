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

package dev.nikomaru.raceassist.api.core.manager

import dev.nikomaru.raceassist.data.database.UserAuthData
import dev.nikomaru.raceassist.files.Config
import dev.nikomaru.raceassist.utils.Utils.toUUID
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class WebManager {
    fun getAvailable(): Boolean {
        return Config.config.webAPI != null
    }

    suspend fun getRegisteredPlayers(): List<UUID> {
        return newSuspendedTransaction {
            UserAuthData.selectAll().map {
                it[UserAuthData.uuid].toUUID()
            }
        }
    }

}