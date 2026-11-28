package dev.dimension.flare.data.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import kotlin.native.HiddenFromObjC

@HiddenFromObjC
public expect fun createDatabaseDriver(): SQLiteDriver

internal expect fun RoomDatabase.refreshOnWritesFromOtherPages()
