package com.austinv11.kotbot.core.api

import com.austinv11.kotbot.core.util.ModuleDependentObject
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.Event
import sx.blah.discord.api.events.IListener

abstract class ModuleDependentIListener<T : Event>(val client: IDiscordClient) : ModuleDependentObject, IListener<T> {
    
    override fun clean() {
        client.dispatcher.unregisterListener(this)
    }

    override fun inject() {
        client.dispatcher.registerListener(this)
    }
}

class ModuleDependentGenericListener(val client: IDiscordClient) : ModuleDependentObject {
    
    override fun clean() {
        client.dispatcher.unregisterListener(this)
    }

    override fun inject() {
        client.dispatcher.registerListener(this)
    }
}
