package ai.sovereignrag.audit.domain.model

import org.springframework.data.domain.Sort.Direction

abstract class PageQuery(
    open val page: PageRequest
)

data class PageRequest(
    val number: Int,
    val size: Int,
    val sort: Direction,
)
