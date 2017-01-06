package com.austinv11.kotbot.core.config

import com.google.gson.GsonBuilder
import java.io.File
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.memberProperties

object Config : IConfig {
    const val FILE_NAME = "./config.json"
    val FILE = File(FILE_NAME)
    val GSON = GsonBuilder().serializeNulls().setPrettyPrinting().create()
    private var _backing = BackingConfigObject()
    
    init {
        if (FILE.exists()) {
            _backing = GSON.fromJson(FILE.readText(), BackingConfigObject::class.java)
        }

        save()
    }

    override var command_prefix: Char by ConfigDelegate()
    override var command_error_format: String by ConfigDelegate()
    override var default_command_success_message: String by ConfigDelegate()
    override var default_command_failure_message: String by ConfigDelegate()
    override var missing_permission_message: String by ConfigDelegate()

    fun save() {
        val writer = FILE.writer()
        GSON.toJson(_backing, writer)
        writer.close()
    }

    private class ConfigDelegate<T> {

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            thisRef as Config
            val field = thisRef._backing.javaClass.kotlin.memberProperties.find { it.name == property.name }!!
            field.isAccessible = true
            return field.get(thisRef._backing) as T
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            thisRef as Config
            val field = thisRef._backing.javaClass.kotlin.memberProperties.find { it.name == property.name }!!.javaField!!
            field.isAccessible = true
            field.set(thisRef._backing, value)
            save()
        }
    }

    private data class BackingConfigObject(override var command_prefix: Char = '~',
                                           override var command_error_format: String = ":warning: %s",
                                           override var default_command_success_message: String = ":ok_hand:", 
                                           override var default_command_failure_message: String = ":x:",
                                           override var missing_permission_message: String = ":no_entry_sign: You must have at least a permission level of `%s`"): IConfig
}

interface IConfig {
    var command_prefix: Char
    var command_error_format: String
    var default_command_success_message: String
    var default_command_failure_message: String
    var missing_permission_message: String
}
