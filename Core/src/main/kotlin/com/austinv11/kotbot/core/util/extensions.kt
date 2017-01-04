package com.austinv11.kotbot.core.util

import sx.blah.discord.api.events.Event
import sx.blah.discord.handle.obj.IDiscordObject

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
