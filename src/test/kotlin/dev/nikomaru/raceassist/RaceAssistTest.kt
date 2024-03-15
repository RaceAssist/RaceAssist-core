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

package dev.nikomaru.raceassist

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class RaceAssistTest : BeforeEachCallback, AfterEachCallback {
    private lateinit var server: ServerMock
    private lateinit var plugin: RaceAssist

    override fun beforeEach(context: ExtensionContext) {
        println("beforeEach() executed before " + context.displayName + ".")
        server = MockBukkit.mock()
        setupKoin()
    }

    override fun afterEach(context: ExtensionContext) {
        MockBukkit.unmock()
    }


    private fun setupKoin() {
        plugin = MockBukkit.load(RaceAssist::class.java)
        val appModule = module {
            single<RaceAssist> { plugin }
            single<ServerMock> { server }
        }
        loadKoinModules(appModule)
    }
}