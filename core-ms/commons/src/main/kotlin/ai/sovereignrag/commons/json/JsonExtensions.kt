package ai.sovereignrag.commons.json

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue

inline fun <reified T> String.fromJson(): T {
    return ObjectMapperFacade.objectMapper.readValue(this)
}

inline fun <reified T> String.fromJson(typeReference: TypeReference<T>): T {
    return ObjectMapperFacade.objectMapper.readValue(this, typeReference)
}
