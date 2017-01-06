package com.austinv11.kotbot.core.util

import com.austinv11.kotbot.core.OWNER
import com.austinv11.kotbot.core.api.commands.Command
import sx.blah.discord.api.events.Event
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IDiscordObject
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer
import java.awt.Color
import java.util.*

val KOTLIN_BLURPLE = Color(118, 108, 180)
val KOTLIN_BLUE = Color(5, 148, 214)
val KOTLIN_PINK = Color(185, 90, 165)
val KOTLIN_ORANGE = Color(248, 138, 0)

fun generateRandomKotlinColor(): Color {
    when(Random().nextInt(4)) {
        0 -> return KOTLIN_BLURPLE
        1 -> return KOTLIN_BLUE
        2 -> return KOTLIN_PINK
        3 -> return KOTLIN_ORANGE
    }
    return KOTLIN_ORANGE
}

/**
 * This creates a standardized embed builder.
 */
fun createEmbedBuilder() = EmbedBuilder().withFooterText("Owned by ${OWNER.name}").withFooterIcon(OWNER.avatarURL).withColor(generateRandomKotlinColor())

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
