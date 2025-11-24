package ai.sovereignrag.commons.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.core.type.TypeReference

object ObjectMapperFacade {
    val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return objectMapper.readValue(json, clazz)
    }

    fun <T> fromJson(json: String, typeReference: TypeReference<T>): T {
        return objectMapper.readValue(json, typeReference)
    }

    fun writeValueAsString(value: Any?): String {
        return objectMapper.writeValueAsString(value)
    }
}

fun <T> T.writeValueAsString(): String {
    return ObjectMapperFacade.objectMapper.writeValueAsString(this)
}
