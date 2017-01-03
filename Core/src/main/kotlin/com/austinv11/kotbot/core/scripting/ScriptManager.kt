package com.austinv11.kotbot.core.scripting

import com.austinv11.kotbot.core.LOGGER
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.modules.IModule
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import kotlin.concurrent.thread

class ScriptManager(val client: IDiscordClient) {
    val scriptFactory = ScriptEngineManager().getEngineByExtension("kts").factory!!
    val engine: ScriptEngine
        get() = scriptFactory.scriptEngine!!
    val modulesPath = File("./modules").toPath()
    val modules = mutableMapOf<File, IModule>()

    init {
        modulesPath.toFile().listFiles { dir, name -> return@listFiles name.endsWith(".kts") }.forEach { 
            LOGGER.info("Loading module $it")
            loadModule(it) 
        }
        
        //Starting a file watcher to watch for module changes
        thread {
            val watchService: WatchService = FileSystems.getDefault().newWatchService()
            
            modulesPath.register(watchService, 
                    StandardWatchEventKinds.ENTRY_CREATE, 
                    StandardWatchEventKinds.ENTRY_DELETE, 
                    StandardWatchEventKinds.ENTRY_MODIFY)
            
            while (true) {
                val key = watchService.take()
                key.pollEvents().forEach { 
                    if (it.kind() == StandardWatchEventKinds.OVERFLOW) {
                        LOGGER.warn("Overflow event received! Ignoring...")
                        return@forEach
                    }
                    
                    val child = it.context() as Path
                    val filePath = modulesPath.resolve(child)
                    
                    if (child.toString().endsWith(".kts")) {
                        if (it.kind() == StandardWatchEventKinds.ENTRY_CREATE) { //Load new module
                            LOGGER.info("New script detected! Now loading ${child.fileName}")
                            loadModule(filePath.toFile())
                        } else if (it.kind() == StandardWatchEventKinds.ENTRY_DELETE) { //Unload module
                            LOGGER.info("Script deleted! Now unloading ${child.fileName}")
                            disableModule(filePath.toFile())
                        } else if (it.kind() == StandardWatchEventKinds.ENTRY_MODIFY) { //Reload module
                            LOGGER.info("Script modified! Now reloading ${child.fileName}")
                            disableModule(filePath.toFile())
                            loadModule(filePath.toFile())
                        }
                    }
                }
                
                if (!key.isValid)
                    break
            }
        }
    }
    
    fun loadModule(script: File): IModule {
        val module = eval(script) as IModule
        client.moduleLoader.loadModule(module)
        if (modules.containsKey(script)) {
            disableModule(script)
        }
        modules[script] = module
        return module
    }
    
    fun disableModule(script: File) {
        val module = modules[script]!!
        client.moduleLoader.unloadModule(module)
        modules.remove(script)
    }
    
    fun eval(script: String): Any? {
        return engine.eval(script)
    }
    
    fun eval(script: File): Any? {
        return eval(script.readText())
    }
}
