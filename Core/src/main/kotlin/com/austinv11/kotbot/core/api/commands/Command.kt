package com.austinv11.kotbot.core.api.commands

import com.austinv11.kotbot.core.util.ModuleDependentObject

abstract class Command(val description: String, 
                       val aliases: Array<String> = arrayOf(), 
                       val requiredLevel: PermissionLevel = PermissionLevel.BASIC,
                       val nameOverride: String? = null) : ModuleDependentObject {
    
    val name: String
        get() {
            if (nameOverride.isNullOrEmpty()) {
                return this::class.simpleName!!.replace("Command", "").toLowerCase()
            } else {
                return nameOverride!!
            }
        }
    
    fun doesCommandMatch(input: String): Boolean = (aliases+name).firstOrNull { it.equals(input, true) } != null
    
    override fun clean() {
        CommandRegistry.commands.remove(this)
    }

    override fun inject() {
        CommandRegistry.commands.add(this)
    }
}
