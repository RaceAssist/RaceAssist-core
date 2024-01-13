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

package dev.nikomaru.raceassist.race.commands

import be.seeseemelk.mockbukkit.ServerMock
import dev.nikomaru.raceassist.RaceAssistTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.test.KoinTest
import org.koin.test.inject

@ExtendWith(RaceAssistTest::class)
class HelpCommandTest : KoinTest {
    private val server: ServerMock by inject()

    @Test
    @DisplayName("Testing the help command")
    fun helpCommandTest() {
        val player = server.addPlayer()
        player.isOp = true
        player.performCommand("ra help")
        Thread.sleep(100)
        val res = player.nextMessage()
        println(res)
        assertNotNull(res)
    }

}