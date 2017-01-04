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

    override var command_prefix: String by ConfigDelegate()

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

    private data class BackingConfigObject(override var command_prefix: String = "~"): IConfig
}

interface IConfig {
    var command_prefix: String
}
