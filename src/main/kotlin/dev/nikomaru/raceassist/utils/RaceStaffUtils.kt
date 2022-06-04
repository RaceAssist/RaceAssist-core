/*
 * Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 * This program is free software: you can redistribute it and/or modify
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

package dev.nikomaru.raceassist.utils

import dev.nikomaru.raceassist.database.RaceStaff
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

object RaceStaffUtils {
    suspend fun addStaff(raceId: String, staff: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        if (existStaff(raceId, staff)) return@newSuspendedTransaction false

        RaceStaff.insert {
            it[this.raceId] = raceId
            it[this.uuid] = staff.toString()
        }
        return@newSuspendedTransaction true
    }

    suspend fun existStaff(raceId: String, staff: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        RaceStaff.select {
            RaceStaff.raceId eq raceId and (RaceStaff.uuid eq staff.toString())
        }.count() > 0
    }

    suspend fun removeStaff(raceId: String, staff: UUID) = newSuspendedTransaction(Dispatchers.IO) {
        if (!existStaff(raceId, staff)) return@newSuspendedTransaction false

        RaceStaff.deleteWhere {
            RaceStaff.raceId eq raceId and (RaceStaff.uuid eq staff.toString())
        }
        return@newSuspendedTransaction true
    }

    suspend fun getStaff(raceId: String) = newSuspendedTransaction(Dispatchers.IO) {
        RaceStaff.select {
            RaceStaff.raceId eq raceId
        }.map {
            UUID.fromString(it[RaceStaff.uuid])
        }
    }
}