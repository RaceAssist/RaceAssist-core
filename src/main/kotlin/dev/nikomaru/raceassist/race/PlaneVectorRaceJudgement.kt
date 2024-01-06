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