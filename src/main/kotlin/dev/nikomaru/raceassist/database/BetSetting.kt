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

package dev.nikomaru.raceassist.database

import org.jetbrains.exposed.sql.Table

object BetSetting : Table() {
    val raceId = varchar("raceId", 30)
    val creator = varchar("creator", 40)
    val canBet = bool("canBet")
    val returnPercent = integer("returnPercent")
    val spreadsheetId = varchar("spreadsheetId", 50).nullable()
}