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

package dev.nikomaru.raceassist.utils.event

enum class LogDataType {
    BET, PLACE, RACE, WEB, HORSE,
}
// プレイヤーに対してもメッセージを送る場合targetを使用しplayerについても記録したい場合明示的に分け同じものを格納
// 時刻はZonedDateTimeを使用する
