/*
 *     Copyright © 2021-2022 Nikomaru <nikomaru@nikomaru.dev>
 *
 *     This program is free software: you can redistribute it and/or modify
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

package dev.nikomaru.raceassist.api.core

import dev.nikomaru.raceassist.api.core.manager.*

/**
 * 各種マネージャーを取得します。
 */
interface RaceAssistAPI {

    /**
     * レースIDを指定して、ベットマネージャーを取得します。
     * @param raceId レースID
     */

    fun getBetManager(raceId: String): BetManager?

    /**
     * Horseマネージャーを取得します。
     */

    fun getHorseManager(): HorseManager

    /**
     * PlaceIdを指定して、Placeマネージャーを取得します。
     * @param placeId PlaceId
     */

    fun getPlaceManager(placeId: String): PlaceManager?

    /**
     * raceIdを指定して、Raceマネージャーを取得します。
     * @param raceId raceId
     */

    fun getRaceManager(raceId: String): RaceManager?

    /**
     * Webマネージャーを取得します。
     */

    fun getWebManager(): WebManager?

    /**
     * Dataマネージャーを取得します。
     */

    fun getDataManager(): DataManager


}