package com.bcgg.pathfinder.util

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class LocalTimeSerializer : JsonSerializer<LocalTime> {
    override fun serialize(
        localDateTime: LocalTime,
        srcType: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(formatter.format(localDateTime))
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}