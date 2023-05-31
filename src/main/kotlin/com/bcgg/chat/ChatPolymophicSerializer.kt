package com.bcgg.chat

import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

object ChatPolymophicSerializer : JsonContentPolymorphicSerializer<Chat>(Chat::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "startPlaceId" in element.jsonObject -> Chat.ChatStartPlace.serializer()
        "endPlaceId" in element.jsonObject -> Chat.ChatEndPlace.serializer()
        "startTime" in element.jsonObject -> Chat.ChatStartTime.serializer()
        "endTime" in element.jsonObject -> Chat.ChatEndTime.serializer()
        "lat" in element.jsonObject -> Chat.ChatMapPosition.serializer()
        "mealTimes" in element.jsonObject -> Chat.ChatMealTimes.serializer()
        "points" in element.jsonObject -> Chat.ChatPoints.serializer()
        "pointToAdd" in element.jsonObject -> Chat.ChatAddPoint.serializer()
        "pointToRemove" in element.jsonObject -> Chat.ChatRemovePoint.serializer()
        "title" in element.jsonObject -> Chat.ChatTitle.serializer()
        "count" in element.jsonObject -> Chat.ChatCount.serializer()
        else -> Chat.serializer()
    }
}