package com.example.data.local

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val checklistType = Types.newParameterizedType(List::class.java, ChecklistItem::class.java)
    private val checklistAdapter = moshi.adapter<List<ChecklistItem>>(checklistType)

    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    private val stringListAdapter = moshi.adapter<List<String>>(stringListType)

    fun checklistToJson(items: List<ChecklistItem>): String {
        return checklistAdapter.toJson(items)
    }

    fun jsonToChecklist(json: String): List<ChecklistItem> {
        if (json.isBlank()) return emptyList()
        return runCatching { checklistAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    fun stringListToJson(list: List<String>): String {
        return stringListAdapter.toJson(list)
    }

    fun jsonToStringList(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching { stringListAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }
}
