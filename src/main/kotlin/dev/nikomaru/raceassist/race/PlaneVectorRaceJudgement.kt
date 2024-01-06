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

package dev.nikomaru.raceassist.race

import com.github.michaelbull.result.Result
import dev.nikomaru.raceassist.race.error.PlaceSettingError
import dev.nikomaru.raceassist.race.error.RaceSettingError
import org.bukkit.command.CommandSender

class PlaneVectorRaceJudgement(override val raceId: String, override val executor: CommandSender) :
    RaceJudgement(raceId, executor) {
    override suspend fun raceSetting(): Result<Unit, RaceSettingError> {
        TODO("Not yet implemented")
    }

    override suspend fun placeSetting(): Result<Unit, PlaceSettingError> {
        TODO("Not yet implemented")
    }

    override suspend fun start() {
        TODO("Not yet implemented")
    }

    override suspend fun calculate() {
        TODO("Not yet implemented")
    }

    override suspend fun display() {
        TODO("Not yet implemented")
    }

    override suspend fun finish() {
        TODO("Not yet implemented")
    }

    override suspend fun payDividend() {
        TODO("Not yet implemented")
    }
}