package com.austinv11.kotbot.core

import com.austinv11.kotbot.core.api.commands.CommandRegistry
import com.austinv11.kotbot.core.config.Config
import com.austinv11.kotbot.core.scripting.ScriptManager
import com.austinv11.kotbot.core.util.ModuleObjectCleaner
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent
import sx.blah.discord.util.DiscordException
import kotlin.system.exitProcess

internal val LOGGER = Discord4J.Discord4JLogger("KotBot") //TODO: Use a real logging impl

internal val CLIENT: IDiscordClient
    get() = _client!!
private var _client: IDiscordClient? = null

val IDiscordClient.scriptManager: ScriptManager
    get() = _scriptManager!!
private var _scriptManager: ScriptManager? = null

val IDiscordClient.moduleObjectCleaner: ModuleObjectCleaner
    get() = _moduleObjectCleaner!!
internal var _moduleObjectCleaner: ModuleObjectCleaner? = null

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw IllegalArgumentException("Missing token!")
    
    if (Discord4J.LOGGER is Discord4J.Discord4JLogger) {
        (Discord4J.LOGGER as Discord4J.Discord4JLogger).setLevel(Discord4J.Discord4JLogger.Level.DEBUG)
    }
    
    LOGGER.info("Launching KotBot...")
    
    Config //Initializes the config
    
    val token = args[0]
    try {
        _client = ClientBuilder()
                .withToken(token)
                .setMaxReconnectAttempts(20)
                .registerListener(object : IListener<ReadyEvent> {
                    override fun handle(event: ReadyEvent) {
                        LOGGER.info("Successfully logged in as ${event.client.ourUser.name}")
                    }
                })
                .registerListener(object : IListener<ReconnectFailureEvent> {
                    override fun handle(event: ReconnectFailureEvent) {
                        if (event.isShardAbandoned) {
                            println("Unable to reconnect, attempting a hard restart...")
                            exitProcess(1) //Non-zero exitcode should lead to a restart
                        }
                    }
                })
                .login()
    } catch (e: DiscordException) {
        LOGGER.error("Unable to launch bot! Closing launcher...", e)
        exitProcess(0) //Using 0 to prevent boot loops
    }

    _moduleObjectCleaner = ModuleObjectCleaner(CLIENT)
    
    _scriptManager = ScriptManager(CLIENT)
    
    CommandRegistry.init(CLIENT)
}
