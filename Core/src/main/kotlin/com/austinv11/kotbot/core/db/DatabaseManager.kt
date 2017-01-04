package com.austinv11.kotbot.core.db

import com.j256.ormlite.jdbc.JdbcConnectionSource

object DatabaseManager {
    /**
     * This is the url leading to the database.
     */
    const val DATABASE_URL = "jdbc:sqlite:database.db"

    val CONNECTION_SOURCE = JdbcConnectionSource(DATABASE_URL)

    init {
        Runtime.getRuntime().addShutdownHook(Thread({ CONNECTION_SOURCE.close() }))
    }
}
