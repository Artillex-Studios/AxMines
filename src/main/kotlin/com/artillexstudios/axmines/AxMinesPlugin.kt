package com.artillexstudios.axmines

import com.artillexstudios.axapi.AxPlugin
import com.artillexstudios.axapi.dependencies.DependencyManagerWrapper
import com.artillexstudios.axapi.metrics.AxMetrics
import com.artillexstudios.axapi.updatechecker.UpdateCheck
import com.artillexstudios.axapi.updatechecker.UpdateCheckResult
import com.artillexstudios.axapi.updatechecker.UpdateChecker
import com.artillexstudios.axapi.updatechecker.sources.ModrinthUpdateCheckSource
import com.artillexstudios.axapi.utils.StringUtils
import com.artillexstudios.axapi.utils.Version
import com.artillexstudios.axmines.commands.AxMinesCommand
import com.artillexstudios.axmines.config.impl.Config
import com.artillexstudios.axmines.config.impl.Messages
import com.artillexstudios.axmines.integrations.PlaceholderAPIIntegration
import com.artillexstudios.axmines.listener.BlockListener
import com.artillexstudios.axmines.mines.Mine
import com.artillexstudios.axmines.mines.MineTicker
import com.artillexstudios.axmines.mines.Mines
import java.time.Duration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bstats.bukkit.Metrics
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import revxrsal.commands.bukkit.BukkitCommandHandler
import revxrsal.zapper.repository.MavenRepository

class AxMinesPlugin : AxPlugin() {
    private val metrics = AxMetrics(this, 4)

    companion object {
        lateinit var INSTANCE: AxMinesPlugin
        lateinit var MESSAGES: Messages
    }

    override fun enable() {
        if (Version.getServerVersion().isOlderThan(Version.v1_18)) {
            logger.severe("Your server version is not supported! Disabling!")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        Metrics(this, 20058)


        INSTANCE = this

        MESSAGES = Messages("messages.yml")

        val commandHandler = BukkitCommandHandler.create(this)

        commandHandler.registerValueResolver(Mine::class.java) { context ->
            val mine = context.popForParameter()

            return@registerValueResolver Mines.valueOf(mine)
        }

        commandHandler.autoCompleter.registerParameterSuggestions(Mine::class.java) { _, _, _ ->
            return@registerParameterSuggestions Mines.getTypes().keys
        }

        commandHandler.register(AxMinesCommand())
        commandHandler.registerBrigadier()

        Bukkit.getPluginManager().registerEvents(BlockListener(), this)

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPIIntegration().register()
        }

        if (Config.UPDATE_CHECKER_ENABLED) {
            val checker = UpdateChecker(ModrinthUpdateCheckSource("axmines"))
                .timeBetweenChecks(Duration.ofMinutes(5))
                .register("axmines.updatecheck.onjoin") { Config.UPDATE_CHECKER_MESSAGE_ON_JOIN }
                .onCheck { sender: CommandSender?, result: UpdateCheck? ->
                    if (result!!.result() == UpdateCheckResult.UPDATE_AVAILABLE) {
                        for (string in MESSAGES.UPDATE_CHECK) {
                            if (string.contains("<changelog>")) {
                                for (changelog in result.changelog()) {
                                    sender!!.sendMessage(
                                        StringUtils.formatToString(
                                            MESSAGES.CHANGELOG_VERSION,
                                            Placeholder.unparsed("version", changelog.version().string())
                                        )
                                    )
                                    for (s in changelog.changelog().split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                        .toTypedArray()) {
                                        sender.sendMessage(
                                            StringUtils.formatToString(
                                                MESSAGES.CHANGELOG,
                                                Placeholder.unparsed("changelog-entry", s)
                                            )
                                        )
                                    }
                                }
                            } else {
                                sender!!.sendMessage(
                                    StringUtils.formatToString(
                                        MESSAGES.PREFIX + string,
                                        Placeholder.parsed("version", result.version().string()),
                                        Placeholder.parsed("current", this.description.version)
                                    )
                                )
                            }
                        }
                    } else if (result.result() == UpdateCheckResult.FAILED) {
                        sender!!.sendMessage(StringUtils.formatToString(MESSAGES.PREFIX + "<#FF0000>Failed to check for updates! Check the console for more information!"))
                        result.exception().printStackTrace()
                    }
                }
                .check(Bukkit.getConsoleSender())
        }

        this.metrics.start()
        MineTicker.schedule()
        reload()
    }

    override fun disable() {
        this.metrics.cancel()
    }

    override fun dependencies(manager: DependencyManagerWrapper) {
        manager.repository(MavenRepository.jitpack())
        manager.repository(MavenRepository.mavenCentral())
        manager.dependency("org.slf4j:slf4j-api:2.0.9")
        manager.dependency("org.apache.commons:commons-text:1.11.0")
        manager.dependency("commons-io:commons-io:2.15.0")
        manager.dependency("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
    }

    override fun reload() {
        Config.reload()
        MESSAGES.reload()
        Mines.reload()
    }
}