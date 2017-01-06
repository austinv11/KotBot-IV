package com.austinv11.kotbot.core

import com.austinv11.kotbot.core.api.commands.CommandRegistry
import com.austinv11.kotbot.core.config.Config
import com.austinv11.kotbot.core.scripting.ScriptManager
import com.austinv11.kotbot.core.util.ModuleObjectCleaner
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.module.ModuleDisabledEvent
import sx.blah.discord.handle.impl.events.module.ModuleEnabledEvent
import sx.blah.discord.handle.impl.events.shard.LoginEvent
import sx.blah.discord.handle.impl.events.shard.ReconnectFailureEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.handle.obj.Status
import sx.blah.discord.util.DiscordException
import kotlin.system.exitProcess

val LOGGER = Discord4J.Discord4JLogger("KotBot") //TODO: Use a real logging impl

val CLIENT: IDiscordClient
    get() = _client!!
private var _client: IDiscordClient? = null

val IDiscordClient.scriptManager: ScriptManager
    get() = _scriptManager!!
private var _scriptManager: ScriptManager? = null

val IDiscordClient.moduleObjectCleaner: ModuleObjectCleaner
    get() = _moduleObjectCleaner!!
internal var _moduleObjectCleaner: ModuleObjectCleaner? = null

val OWNER: IUser
    get() {
        if (_owner == null)
            _owner = CLIENT.applicationOwner
        
        return _owner!!
    }

private var _owner: IUser? = null

val IUser.isOwner: Boolean
    get() = this == OWNER

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw IllegalArgumentException("Missing token!")
    
    if (Discord4J.LOGGER is Discord4J.Discord4JLogger) {
        (Discord4J.LOGGER as Discord4J.Discord4JLogger).setLevel(Discord4J.Discord4JLogger.Level.DEBUG)
    }
    
    LOGGER.info("Launching KotBot...")
    
    System.setProperty("kotlin.compiler.jar", Config::class.java.protectionDomain.codeSource.location.toURI().path) //Hack to fix missing properties when compiled to a shadow jar
    
    Config //Initializes the config
    
    val token = args[0]
    try {
        _client = ClientBuilder()
                .withToken(token)
                .setMaxReconnectAttempts(20)
                .registerListener(object : IListener<LoginEvent> {
                    override fun handle(event: LoginEvent) {
                        CLIENT.changePresence(true)
                        CLIENT.changeStatus(Status.game("${CLIENT.scriptManager.loadedModules.get()}/${CLIENT.scriptManager.totalModules.get()} modules loaded"))
                        LOGGER.info("Successfully logged in as ${event.client.ourUser.name}")
                    }
                })
                .registerListener(object : IListener<ReconnectFailureEvent> {
                    override fun handle(event: ReconnectFailureEvent) {
                        if (event.isShardAbandoned) {
                            println("Unable to reconnect, attempting a hard restart...")
                            restart() //Non-zero exitcode should lead to a restart
                        }
                    }
                })
                .registerListener(object: IListener<ModuleEnabledEvent> {
                    override fun handle(event: ModuleEnabledEvent) {
                        CLIENT.scriptManager.loadedModules.incrementAndGet()
                    }
                })
                .registerListener(object: IListener<ModuleDisabledEvent> {
                    override fun handle(event: ModuleDisabledEvent) {
                        CLIENT.scriptManager.loadedModules.decrementAndGet()
                    }
                })
                .registerListener(object : IListener<ModuleEnabledEvent> {
                    override fun handle(event: ModuleEnabledEvent) {
                        if (CLIENT.scriptManager.totalModules.get() == (CLIENT.scriptManager.loadedModules.get())) {
                            CLIENT.changePresence(false)
                            CLIENT.changeStatus(Status.empty())
                            CLIENT.dispatcher.unregisterListener(this)
                        } else {
                            CLIENT.changePresence(true)
                            CLIENT.changeStatus(Status.game("${CLIENT.scriptManager.loadedModules.get()}/${CLIENT.scriptManager.totalModules.get()} modules loaded"))
                        }
                    }
                })
                .build()

        _moduleObjectCleaner = ModuleObjectCleaner(CLIENT)

        _scriptManager = ScriptManager(CLIENT)

        CommandRegistry.init(CLIENT)
        
        CLIENT.login()
    } catch (e: DiscordException) {
        LOGGER.error("Unable to launch bot! Closing launcher...", e)
        shutdown() //Using 0 to prevent boot loops
    }
}

fun shutdown(): Nothing = exitProcess(0)

fun restart(): Nothing = exitProcess(1)

fun update(): Nothing = exitProcess(-1)
