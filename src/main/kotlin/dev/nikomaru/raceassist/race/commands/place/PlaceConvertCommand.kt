package dev.nikomaru.raceassist.race.commands.place

import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission

@CommandMethod("ra place")
class PlaceConvertCommand {
    @CommandPermission("raceassist.commands.place.convert")
    @CommandMethod("convert <placeId> <afterPlaceId>")
    fun convert() {
        TODO("aaa")

    }
}