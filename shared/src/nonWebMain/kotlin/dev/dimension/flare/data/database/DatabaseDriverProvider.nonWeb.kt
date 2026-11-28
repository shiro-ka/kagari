package dev.dimension.flare.data.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public actual fun createDatabaseDriver(): SQLiteDriver = BundledSQLiteDriver()

internal actual fun RoomDatabase.refreshOnWritesFromOtherPages() {}
