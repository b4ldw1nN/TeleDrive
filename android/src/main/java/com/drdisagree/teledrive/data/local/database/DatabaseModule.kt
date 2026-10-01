package com.drdisagree.teledrive.data.local.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(androidContext(), TeleDriveDatabase::class.java, TeleDriveDatabase.NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }
}
