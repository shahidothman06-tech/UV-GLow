package com.tubers.blacklight.data.repo

import com.tubers.blacklight.data.db.PresetDao
import com.tubers.blacklight.data.db.ThemeDao
import com.tubers.blacklight.data.db.ThemeEntity
import com.tubers.blacklight.data.db.toDomain
import com.tubers.blacklight.data.db.toEntity
import com.tubers.blacklight.domain.model.BuiltInThemes
import com.tubers.blacklight.domain.model.Preset
import com.tubers.blacklight.domain.model.ThemeItem
import com.tubers.blacklight.domain.repository.PresetRepository
import com.tubers.blacklight.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPresetRepository(private val dao: PresetDao) : PresetRepository {
    override val presets: Flow<List<Preset>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(preset: Preset): Long =
        dao.insert(preset.copy(id = 0, createdAt = System.currentTimeMillis()).toEntity())

    override suspend fun rename(id: Long, name: String) = dao.rename(id, name)

    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomThemeRepository(private val dao: ThemeDao) : ThemeRepository {
    override val customThemes: Flow<List<ThemeItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun insert(theme: ThemeItem): String {
        val id = dao.insert(
            ThemeEntity(
                name = theme.name,
                accentArgb = theme.accentArgb,
                groundArgb = theme.groundArgb,
                createdAt = System.currentTimeMillis(),
            )
        )
        return BuiltInThemes.customId(id)
    }

    override suspend fun rename(themeId: String, name: String) {
        BuiltInThemes.customDbId(themeId)?.let { dao.rename(it, name) }
    }

    override suspend fun updateColors(themeId: String, accentArgb: Int, groundArgb: Int) {
        BuiltInThemes.customDbId(themeId)?.let { dao.updateColors(it, accentArgb, groundArgb) }
    }

    override suspend fun delete(themeId: String) {
        BuiltInThemes.customDbId(themeId)?.let { dao.delete(it) }
    }
}
