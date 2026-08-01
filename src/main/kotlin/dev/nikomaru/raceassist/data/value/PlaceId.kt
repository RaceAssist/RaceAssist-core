package dev.nikomaru.raceassist.data.value


interface IdentifiableRaceId {
    val raceId: String
}

@JvmInline
value class RaceId(override val raceId: String) : IdentifiableRaceId

@JvmInline
value class OperatorId(override val raceId: String) : IdentifiableRaceId