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

import cloud.commandframework.CommandManager
import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.specifier.Greedy
import cloud.commandframework.annotations.suggestions.Suggestions
import cloud.commandframework.context.CommandContext
import cloud.commandframework.minecraft.extras.MinecraftHelp
import dev.nikomaru.raceassist.RaceAssist
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender


class HelpCommand {
    private lateinit var manager: CommandManager<CommandSender>
    private lateinit var minecraftHelp: MinecraftHelp<CommandSender>
    private lateinit var audience: Audience


    fun registerFeature(
        plugin: RaceAssist,
        annotationParser: AnnotationParser<CommandSender>
    ) {
        this.manager = annotationParser.manager()
        this.audience = Bukkit.getServer()
        // Set up the help instance.
        this.setupHelp()

        // This will scan for `@Command` and `@Suggestions`.
        annotationParser.parse(this)
    }

    private fun setupHelp() {
        this.minecraftHelp =
            MinecraftHelp.createNative(
                // The help command. This gets prefixed onto all the clickable queries.
                "/ra help",  // The command manager instance that is used to look up the commands.
                this.manager,  // Tells the help manager how to map command senders to adventure audiences.
            )
    }

    @Suggestions("help_queries")
    fun suggestHelpQueries(
        ctx: CommandContext<CommandSender?>,
        input: String
    ): List<String> {
        return manager.createCommandHelpHandler()
            .queryRootIndex(ctx.sender)
            .entries
            .stream()
            .map { it.syntaxString }
            .toList()
    }

    @CommandMethod("ra help [query]")
    @CommandDescription("Help menu")
    fun commandHelp(
        sender: CommandSender,
        @Argument(value = "query", suggestions = "help_queries") @Greedy query: String?
    ) {
        // MinecraftHelp looks up the relevant help pages and displays them to the sender.
        minecraftHelp.queryCommands(query ?: "", sender)
    }
}