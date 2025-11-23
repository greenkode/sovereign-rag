package ai.sovereignrag.process.dao

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProcessRequestRepository : JpaRepository<ProcessRequestEntity, Long> {
    
    @Query("""
        SELECT pr FROM ProcessRequestEntity pr 
        LEFT JOIN FETCH pr.process p 
        LEFT JOIN FETCH pr.data
        LEFT JOIN FETCH pr.stakeholders
        WHERE pr.id = :id
    """)
    fun findByIdWithProcess(@Param("id") id: Long): ProcessRequestEntity?
    
    @Query("""
        SELECT pr FROM ProcessRequestEntity pr 
        LEFT JOIN FETCH pr.process p 
        WHERE pr.id IN :ids
    """)
    fun findAllByIdWithProcess(@Param("ids") ids: Set<Long>): List<ProcessRequestEntity>
}