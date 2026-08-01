package dev.nikomaru.raceassist.data.value


interface IdentifiableRaceId {
    val raceId: String
}

@JvmInline
value class RaceId(override val raceId: String) : IdentifiableRaceId{
    override fun toString(): String {
        return raceId
    }
}

@JvmInline
value class OperateRaceId(override val raceId: String) : IdentifiableRaceId{
    override fun toString(): String{
        return raceId
    }
}