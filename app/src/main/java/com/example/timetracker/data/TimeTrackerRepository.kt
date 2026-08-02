package com.example.timetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class TimeTrackerRepository(
    private val categoryDao: CategoryDao,
    private val timeRecordDao: TimeRecordDao
) {
    // Categories
    val rootCategories: Flow<List<Category>> = categoryDao.getRootCategories()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getSubCategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getSubCategories(parentId)

    suspend fun insertCategory(name: String, parentId: Long? = null): Long {
        return categoryDao.insert(Category(name = name, parentId = parentId))
    }

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getById(id)

    suspend fun updateCategoryParent(categoryId: Long, newParentId: Long?) {
        val category = categoryDao.getById(categoryId) ?: return
        categoryDao.update(category.copy(parentId = newParentId))
    }

    suspend fun updateCategoryName(categoryId: Long, newName: String) {
        val category = categoryDao.getById(categoryId) ?: return
        categoryDao.update(category.copy(name = newName.trim()))
    }

    suspend fun deleteCategory(categoryId: Long) {
        val all = allCategories.first()
        val idsToDelete = mutableListOf<Long>()
        fun collect(id: Long) {
            idsToDelete.add(id)
            all.filter { it.parentId == id }.forEach { collect(it.id) }
        }
        collect(categoryId)

        idsToDelete.forEach { id ->
            timeRecordDao.clearCategoryForDeletedCategory(id)
            val category = all.find { it.id == id } ?: return@forEach
            categoryDao.delete(category)
        }
    }

    // Records
    val allRecords: Flow<List<TimeRecord>> = timeRecordDao.getAllRecords()

    fun getRecordsForDate(date: LocalDate): Flow<List<TimeRecord>> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()
        return timeRecordDao.getRecordsBetween(start, end)
    }

    suspend fun startRecord(categoryId: Long?, categoryName: String): Long {
        val record = TimeRecord(
            categoryId = categoryId,
            categoryName = categoryName,
            startTime = Instant.now()
        )
        return timeRecordDao.insert(record)
    }

    suspend fun stopRecord(recordId: Long) {
        val record = timeRecordDao.getById(recordId) ?: return
        val endTime = Instant.now()
        val duration = ChronoUnit.SECONDS.between(record.startTime, endTime)
        timeRecordDao.update(
            record.copy(
                endTime = endTime,
                durationSeconds = duration
            )
        )
    }

    suspend fun insertManualRecord(
        categoryId: Long?,
        categoryName: String,
        startTime: Instant,
        endTime: Instant,
        notes: String
    ) {
        val duration = ChronoUnit.SECONDS.between(startTime, endTime)
        if (duration <= 0) return
        val record = TimeRecord(
            categoryId = categoryId,
            categoryName = categoryName,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = duration,
            notes = notes.trim()
        )
        timeRecordDao.insert(record)
    }

    suspend fun getTotalSecondsByCategory(categoryId: Long, start: Instant, end: Instant): Long {
        return timeRecordDao.getTotalSecondsByCategory(categoryId, start, end) ?: 0L
    }

    suspend fun deleteRecord(record: TimeRecord) {
        timeRecordDao.delete(record)
    }

    suspend fun seedDefaultCategories() {
        val existing = allCategories.first()
        if (existing.isNotEmpty()) return

        val readingId = insertCategory("读书")
        val exerciseId = insertCategory("运动")
        insertCategory("工作")
        insertCategory("写作")

        // Sub-categories for exercise
        insertCategory("瑜伽", exerciseId)
        insertCategory("羽毛球", exerciseId)
        insertCategory("跑步", exerciseId)

        // Sub-categories for reading
        insertCategory("小说", readingId)
        insertCategory("专业书", readingId)
    }
}
