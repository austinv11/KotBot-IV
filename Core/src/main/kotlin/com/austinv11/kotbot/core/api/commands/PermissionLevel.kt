package com.austinv11.kotbot.core.api.commands

import com.austinv11.kotbot.core.db.DatabaseManager
import com.austinv11.kotbot.core.db.DatabaseManager.PERMISSIONS_DAO
import com.austinv11.kotbot.core.db.DatabaseManager.PermissionHolder.Columns.GUILD_ID
import com.austinv11.kotbot.core.db.DatabaseManager.PermissionHolder.Columns.USER_ID
import com.austinv11.kotbot.core.isOwner
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IUser

fun IUser.retrievePermissionLevel(guild: IGuild): PermissionLevel {
    if (this.isBot)
        return PermissionLevel.NONE
    
    if (this.isOwner)
        return PermissionLevel.OWNER
    
    val result = PERMISSIONS_DAO.queryForFieldValuesArgs(mapOf(USER_ID to this.id, GUILD_ID to guild.id))
    if (result.size == 0)
        return PermissionLevel.BASIC
    else {
        return result.first().permissions
    }
}

fun IUser.setPermissionLevel(level: PermissionLevel, guild: IGuild) {
    if (!this.isBot && !this.isOwner && level != PermissionLevel.OWNER && this.retrievePermissionLevel(guild) != level) {
        val result = PERMISSIONS_DAO.queryForFieldValuesArgs(mapOf(USER_ID to this.id, GUILD_ID to guild.id)).firstOrNull()
        if (level == PermissionLevel.BASIC) { //Basic levels are inherent so its unnecessary to store
            PERMISSIONS_DAO.delete(result!!)
        } else {
            if (result == null) {
                DatabaseManager.PermissionHolder(this.id, level)
            } else {
                result.permissions = level
                PERMISSIONS_DAO.update(result)
            }
        } 
    }
}

enum class PermissionLevel {
    NONE, BASIC, ADMINISTRATOR, OWNER
}
