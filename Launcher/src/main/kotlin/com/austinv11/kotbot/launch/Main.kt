package com.austinv11.kotbot.launch

import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

const val REPO_MIRROR_DIR = "./git_mirror/"
const val INSTALLATION_DIR = "./bot.jar"
const val COMMIT_URL = "https://api.github.com/repos/austinv11/KotBot-IV/commits/master"
const val COMMIT_CACHE = REPO_MIRROR_DIR+"commit.txt"
const val GIT_REPO = "https://github.com/austinv11/KotBot-IV.git"

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw IllegalArgumentException("At least one argument must be provided!")
    
    val token = args[0]
    
    installUpdate()
    
    println("Launching the bot...")
    do {
        val returnVal = runBot(token)
        if (returnVal == -1) //Update
            installUpdate()
    } while (returnVal != 0) //This ensures 100% uptime
}

fun runBot(token: String): Int = ProcessBuilder("java", "-jar", File(INSTALLATION_DIR).absolutePath, token).inheritIO().start().waitFor()

fun installUpdate(): Boolean {
    val mirror = File(REPO_MIRROR_DIR)
    val bot = File(INSTALLATION_DIR)
    val commit = File(COMMIT_CACHE)

    val latestCommit = getLatestCommit()
    if (!mirror.exists() || !commit.exists() || !bot.exists() || commit.readText() != latestCommit) { //Time to (re)install
        println("Updating the bot...")

        if (bot.exists())
            bot.delete()

        if (mirror.exists())
            mirror.deleteRecursively()

        println("Cloning the git repo...")
        if (ProcessBuilder("git", "clone", GIT_REPO, REPO_MIRROR_DIR).inheritIO().start().waitFor() != 0)
            exitProcess(1)

        commit.writeText(latestCommit)

        println("Building the bot...")
        ProcessBuilder("./gradlew", "build").inheritIO().directory(File(REPO_MIRROR_DIR+"Core/")).start().waitFor()
        Files.move(Paths.get(REPO_MIRROR_DIR+"Core/build/libs/Core-1.0-all.jar"), Paths.get(INSTALLATION_DIR))
        return true
    }
    
    return false
}

fun getLatestCommit(): String {
    val (_, response, result) = COMMIT_URL.httpGet().responseString()
    return result.fold({
        return@fold Gson().fromJson(it, CondensedCommitObj::class.java).sha
    },{
        exitProcess(1)
    })
}

data class CondensedCommitObj(var sha: String)
