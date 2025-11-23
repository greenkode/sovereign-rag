package ai.sovereignrag.audit.api.dto

import ai.sovereignrag.audit.domain.model.PageRequest
import org.springframework.data.domain.Sort.Direction

abstract class PageRequestDto(
    open val page: Int?,
    open val size: Int?,
    open val sort: Direction?,
) {
    fun toDomain(): PageRequest {
        return PageRequest(page ?: 0, size ?: 20, sort ?: Direction.DESC)
    }
}