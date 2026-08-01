package dev.nikomaru.raceassist.data.value


interface IdentifiablePlaceId{
    val placeId: String
}


@JvmInline
value class PlaceId(override val placeId: String): IdentifiablePlaceId{
    override fun toString(): String {
        return placeId
    }
}

@JvmInline
value class OperatePlaceId(override val placeId: String): IdentifiablePlaceId{
    override fun toString(): String {
        return placeId
    }
}

