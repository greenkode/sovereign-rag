package ai.sovereignrag.identity.process.domain

import ai.sovereignrag.identity.commons.CacheNames
import ai.sovereignrag.identity.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.identity.commons.process.enumeration.ProcessState
import ai.sovereignrag.identity.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.process.domain.model.ProcessBasicInfo
import ai.sovereignrag.identity.process.domain.model.ProcessTransitionInfo
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface ProcessRepository : JpaRepository<ProcessEntity, Long> {

    @Cacheable(value = [CacheNames.PROCESS], key = "#publicId.toString()", unless = "#result == null")
    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders
        LEFT JOIN FETCH p.transitions 
        WHERE p.publicId = :publicId
    """
    )
    fun findByPublicId(@Param("publicId") publicId: UUID): ProcessEntity?

    @Cacheable(
        value = [CacheNames.PROCESS],
        key = "#publicId.toString() + '_' + #state.name()",
        unless = "#result == null"
    )
    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders
        LEFT JOIN FETCH p.transitions 
        WHERE p.publicId = :publicId AND p.state = :state
    """
    )
    fun findByPublicIdAndState(@Param("publicId") publicId: UUID, @Param("state") state: ProcessState): ProcessEntity?

    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders
        LEFT JOIN FETCH p.transitions 
        WHERE p.externalReference = :externalReference AND p.type = :type AND p.state = :state
    """
    )
    fun findByExternalReferenceAndTypeAndState(
        @Param("externalReference") externalReference: String,
        @Param("type") type: ProcessType,
        @Param("state") state: ProcessState
    ): ProcessEntity?

    fun findByExternalReferenceAndTypeInAndState(
        externalReference: String,
        type: Set<ProcessType>,
        state: ProcessState
    ): ProcessEntity?

    @Cacheable(
        value = [CacheNames.PROCESS],
        key = "#externalReference + '_' + #state.name()",
        unless = "#result == null"
    )
    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders
        LEFT JOIN FETCH p.transitions 
        WHERE p.externalReference = :externalReference AND p.state = :state
    """
    )
    fun findByExternalReferenceAndState(
        @Param("externalReference") externalReference: String,
        @Param("state") state: ProcessState
    ): ProcessEntity?

    @Cacheable(value = [CacheNames.PROCESS], key = "#externalReference", unless = "#result == null")
    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders
        LEFT JOIN FETCH p.transitions 
        WHERE p.externalReference = :externalReference
    """
    )
    fun findByExternalReference(
        @Param("externalReference") externalReference: String
    ): ProcessEntity?

    @Caching(
        evict = [
            CacheEvict(value = [CacheNames.PROCESS], key = "#entity.publicId.toString()"),
            CacheEvict(value = [CacheNames.PROCESS], key = "#entity.publicId.toString() + '_' + #entity.state.name()"),
            CacheEvict(
                value = [CacheNames.PROCESS],
                key = "#entity.externalReference + '_' + #entity.state.name()",
                condition = "#entity.externalReference != null"
            ),
            CacheEvict(value = [CacheNames.PROCESS], key = "'basic_' + #entity.publicId.toString()"),
            CacheEvict(value = [CacheNames.PROCESS], allEntries = true, condition = "#entity.state != null")
        ]
    )
    override fun <S : ProcessEntity> save(entity: S): S

    @CacheEvict(value = [CacheNames.PROCESS], allEntries = true)
    override fun <S : ProcessEntity> saveAll(entities: MutableIterable<S>): MutableList<S>

    // ========== LIGHTWEIGHT QUERY METHODS ==========

    @Cacheable(value = [CacheNames.PROCESS], key = "'basic_info_' + #publicId.toString()", unless = "#result == null")
    @Query(
        """
        SELECT new ai.sovereignrag.identity.process.domain.model.ProcessBasicInfo(
            p.id, p.publicId, p.type, p.state, p.channel, p.createdDate, p.externalReference
        )
        FROM ProcessEntity p 
        WHERE p.publicId = :publicId
    """
    )
    fun findBasicInfoByPublicId(@Param("publicId") publicId: UUID): ProcessBasicInfo?


    @Query(
        """
        SELECT new ai.sovereignrag.identity.process.domain.model.ProcessTransitionInfo(
            t.id, t.process.id, t.event, t.userId, t.oldState, t.newState, t.createdDate
        )
        FROM ProcessEventTransitionEntity t
        WHERE t.process.publicId = :publicId
        ORDER BY t.createdDate DESC
    """
    )
    fun findTransitionsByProcessId(@Param("publicId") publicId: UUID): List<ProcessTransitionInfo>

    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders
        WHERE p.type = :type
    """
    )
    fun findByType(@Param("type") type: ProcessType, pageable: Pageable): Page<ProcessEntity>

    // ========== DIRECT UPDATE QUERIES ==========

    @Modifying
    @Query(
        """
        UPDATE ProcessEntity p 
        SET p.state = :newState 
        WHERE p.publicId = :publicId 
        AND p.state = :currentState
    """
    )
    @CacheEvict(value = [CacheNames.PROCESS], allEntries = true)
    fun updateStateIfInState(
        @Param("publicId") publicId: UUID,
        @Param("newState") newState: ProcessState,
        @Param("currentState") currentState: ProcessState
    ): Int

    @Modifying
    @Query(
        """
        UPDATE ProcessRequestEntity r 
        SET r.state = :newState 
        WHERE r.id = :requestId 
        AND r.process.publicId = :processId
        AND r.process.state = :processState
    """
    )
    @CacheEvict(value = [CacheNames.PROCESS], allEntries = true)
    fun updateRequestStateIfProcessInState(
        @Param("processId") processId: UUID,
        @Param("requestId") requestId: Long,
        @Param("newState") newState: ProcessState,
        @Param("processState") processState: ProcessState
    ): Int

    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders s
        WHERE p.type = :processType 
        AND s.type = :stakeholderType 
        AND s.stakeholderId = :userId
        AND p.createdDate >= :sinceDate
        AND p.state = :state
        ORDER BY p.createdDate DESC
    """
    )
    fun findRecentPendingProcessesByTypeAndForUserId(
        @Param("processType") processType: ProcessType,
        @Param("stakeholderType") stakeholderType: ProcessStakeholderType,
        @Param("userId") userId: String,
        @Param("sinceDate") sinceDate: Instant,
        @Param("state") state: ProcessState
    ): List<ProcessEntity>

    @Modifying
    @Query(
        """
        UPDATE ProcessEntity p 
        SET p.state = :expiredState 
        WHERE p.expiry < :currentTime 
        AND p.state IN :activeStates
    """
    )
    @CacheEvict(value = [CacheNames.PROCESS], allEntries = true)
    fun bulkExpireProcesses(
        @Param("currentTime") currentTime: Instant,
        @Param("expiredState") expiredState: ProcessState = ProcessState.EXPIRED,
        @Param("activeStates") activeStates: Set<ProcessState> = setOf(ProcessState.PENDING)
    ): Int

    @Query(
        """
        SELECT DISTINCT p FROM ProcessEntity p 
        LEFT JOIN FETCH p.requests r 
        LEFT JOIN FETCH r.data
        LEFT JOIN FETCH r.stakeholders s
        WHERE p.type = :processType 
        AND s.type = :stakeholderType 
        AND s.stakeholderId = :userId
        AND p.createdDate >= :sinceDate
        ORDER BY p.createdDate DESC LIMIT 1
    """
    )
    fun findLatestPendingProcessByTypeAndForUserId(
        processType: ProcessType,
        stakeholderType: ProcessStakeholderType,
        userId: String
    ): ProcessEntity?

}