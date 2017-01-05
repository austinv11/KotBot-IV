package com.austinv11.kotbot.core.util

import com.austinv11.kotbot.core.api.commands.Command
import sx.blah.discord.api.events.Event
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IDiscordObject
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MessageTokenizer
import sx.blah.discord.util.RequestBuffer
import java.util.*

/**
 * This checks if a specified event has a field which has an object with the same id as the provided object.
 * @param obj The object to check for.
 * @param T The object type to search for.
 * @return True if an object with a matching id is found.
 */
inline fun <reified T: IDiscordObject<T>> Event.isObjectPresent(obj: T): Boolean {
    return isIDPresent<T>(obj.id)
}

/**
 * This checks if a specified event has a field which has an object with the same id as provided.
 * @param id The id to check for.
 * @param T The object type to search for.
 * @return True if an object with the provided id is found.
 */
inline fun <reified T: IDiscordObject<T>> Event.isIDPresent(id: String): Boolean {
    return (this::class.java.fields+this::class.java.declaredFields)
            .filter { T::class.java.isAssignableFrom(it.type) }
            .apply { this.firstOrNull()?.isAccessible = true }
            .firstOrNull { (it.get(this) as? T)?.id == id }
            ?.run { true } ?: false
}

/**
 * This is a simple function wrapper for RequestBuffer.
 */
fun buffer(block: () -> Unit) {
    buffer<Unit>(block)
}

/**
 * This is a simple function wrapper for RequestBuffer.
 */
fun <T> buffer(block: () -> T): T {
    return RequestBuffer.request(block).get()
}

/**
 * This represents caller information.
 */
val Any.caller: Caller
    get() = Caller()

internal val contextMap: MutableMap<Int, CommandContext> = mutableMapOf()

/**
 * This represents a command's context.
 */
val Command.context: CommandContext
    get() {
        val callr = caller
        return contextMap[Objects.hash(callr.thread, callr.`class`)]!!
    }

/**
 * This represents a caller context.
 */
data class Caller(val thread: Thread = Thread.currentThread(),
                  val `class`: String = spoofManager.getCallerClassName(4))

private val spoofManager = SpoofSecurityManager()

internal class SpoofSecurityManager : SecurityManager() {
    fun getCallerClassName(callStackDepth: Int): String {
        return classContext[callStackDepth].name
    }
}

/**
 * This represents the context for a command.
 */
data class CommandContext(val message: IMessage,
                          val channel: IChannel = message.channel,
                          val user: IUser = message.author,
                          val content: String = message.content)
