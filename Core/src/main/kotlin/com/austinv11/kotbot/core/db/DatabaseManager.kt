package com.austinv11.kotbot.core.db

import com.austinv11.kotbot.core.api.commands.PermissionLevel
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.table.DatabaseTable
import com.j256.ormlite.table.TableUtils

object DatabaseManager {
    /**
     * This is the url leading to the database.
     */
    const val DATABASE_URL = "jdbc:sqlite:database.db"

    val CONNECTION_SOURCE = JdbcConnectionSource(DATABASE_URL)

    val PERMISSIONS_DAO: Dao<PermissionHolder, Int> = DaoManager.createDao(CONNECTION_SOURCE, PermissionHolder::class.java)

    init {
        TableUtils.createTableIfNotExists(CONNECTION_SOURCE, PermissionHolder::class.java)
        Runtime.getRuntime().addShutdownHook(Thread({ CONNECTION_SOURCE.close() }))
    }

    @DatabaseTable(tableName = "accounts")
    class PermissionHolder {
        
        constructor()
        
        constructor(user: String, permissions: PermissionLevel) {
            this.user = user
            this.permissions = permissions

            PERMISSIONS_DAO.create(this)
        }


        companion object Columns {
            const val ID = "id"
            const val USER_ID = "user"
            const val PERMISSION_LEVEL = "permissions"
        }
        
        @DatabaseField(columnName = ID, generatedId = true)
        var id: Int = 0
        
        @DatabaseField(columnName = USER_ID, canBeNull = false)
        var user: String = ""
        
        @DatabaseField(columnName = PERMISSION_LEVEL, canBeNull = false, dataType = DataType.ENUM_STRING)
        var permissions: PermissionLevel = PermissionLevel.BASIC
    }
}
