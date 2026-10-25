package com.tubers.blacklight.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [PresetEntity::class, ThemeEntity::class], version = 1, exportSchema = true)
abstract class UvGlowDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun themeDao(): ThemeDao

    companion object {
        fun build(context: Context): UvGlowDatabase =
            Room.databaseBuilder(context.applicationContext, UvGlowDatabase::class.java, "uv_glow.db")
                .addCallback(SeedCallback)
                .build()

        /** Three example presets so the Presets screen isn't empty on first launch. */
        private object SeedCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val sql = "INSERT INTO presets (name, modeId, colorArgb, colorName, groundArgb, themeId, " +
                    "brightness, beamWidth, glowRadius, speedTenths, createdAt) VALUES (?,?,?,?,?,?,?,?,?,?,?)"
                val seeds = listOf(
                    arrayOf<Any?>("Reading nook", "uv", 0xFFA855F7.toInt(), "Violet", 0xFF07050C.toInt(), "midnight", 45, 35, 40, 6, 3L),
                    arrayOf<Any?>("Party rays", "rays", 0xFFF472D0.toInt(), "Pink", 0xFF0B0509.toInt(), "bloom", 90, 85, 80, 14, 2L),
                    arrayOf<Any?>("Night signal", "sos", 0xFF22D3EE.toInt(), "Cyan", 0xFF03090B.toInt(), "aurora", 100, 30, 55, 10, 1L),
                )
                seeds.forEach { db.execSQL(sql, it) }
            }
        }
    }
}
