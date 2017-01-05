package com.austinv11.kotbot.core.util

import com.austinv11.kotbot.core.CLIENT
import com.austinv11.kotbot.core.moduleObjectCleaner
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.IListener
import sx.blah.discord.handle.impl.events.module.ModuleDisabledEvent
import sx.blah.discord.modules.IModule

fun IModule.register(vararg moduleObjects: ModuleDependentObject) {
    moduleObjects.forEach { register(it) }
}

fun IModule.register(moduleObject: ModuleDependentObject) {
    moduleObject.inject()
    CLIENT.moduleObjectCleaner.registry.putIfAbsent(this, mutableSetOf<ModuleDependentObject>())
    CLIENT.moduleObjectCleaner.registry[this]!!.add(moduleObject)
}

/**
 * These are objects which should be cleaned before unloading a module.
 */
interface ModuleDependentObject {

    /**
     * When this is called, any references to this object should be removed.
     */
    fun clean()

    /**
     * When this is called, this object should register any references to itself necessary.
     */
    fun inject()
}

class ModuleObjectCleaner(client: IDiscordClient) {
    
    val registry = mutableMapOf<IModule, MutableSet<ModuleDependentObject>>()
    
    init {
        client.dispatcher.registerListener(object: IListener<ModuleDisabledEvent> {
            override fun handle(event: ModuleDisabledEvent) {
                registry[event.module]?.forEach { it.apply(ModuleDependentObject::clean) }
                registry.remove(event.module)
            }
        })
    }
}
