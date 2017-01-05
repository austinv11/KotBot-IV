package com.austinv11.kotbot.core

import com.austinv11.kotbot.core.api.commands.CommandRegistry
import com.austinv11.kotbot.core.scripting.ScriptManager
import com.austinv11.kotbot.core.util.ModuleObjectCleaner
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient

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
    
    val token = args[0]
    _client = ClientBuilder()
            .withToken(token)
            .setMaxReconnectAttempts(20)
            .login()

    _moduleObjectCleaner = ModuleObjectCleaner(CLIENT)
    
    _scriptManager = ScriptManager(CLIENT)
    
    CommandRegistry.init(CLIENT)
}
