package ai.sovereignrag.identity.process.domain


import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.process.ProcessChannel
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessType
import ai.sovereignrag.identity.process.domain.model.ProcessEventTransition
import ai.sovereignrag.identity.process.domain.model.ProcessRequest
import ai.sovereignrag.identity.process.domain.model.ProcessRequestData
import ai.sovereignrag.identity.process.domain.model.ProcessStakeholder
import ai.sovereignrag.identity.process.domain.model.SrProcess
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
class ProcessEntity(
    val publicId: UUID,

    @Enumerated(EnumType.STRING)
    private val type: ProcessType,

    private val description: String,

    @Enumerated(EnumType.STRING) var state: ProcessState,

    @Enumerated(EnumType.STRING)
    private val channel: ProcessChannel,

    val expiry: Instant,

    val externalReference: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToMany(mappedBy = "process", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    val requests: MutableSet<ProcessRequestEntity> = mutableSetOf(),

    @OneToMany(mappedBy = "process", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    val transitions: MutableSet<ProcessEventTransitionEntity> = mutableSetOf(),
) :
    AuditableEntity(), Serializable {
    fun toDomain() = SrProcess(
        publicId,
        type,
        description,
        state,
        id ?: 0L,
        channel,
        createdAt!!,
        requests.map { it.toDomain() }.toSet(),
        transitions.map { it.toDomain() }.toSet(),
        externalReference,
    )

    fun updateState(newState: ProcessState) {
        this.state = newState
    }

    fun addRequest(request: ProcessRequestEntity) = requests.add(request)

    fun updateRequestState(requestId: Long, state: ProcessState) {
        requests.forEach { request ->
            if (request.id == requestId) {
                request.state = state
                return
            }
        }
    }

    fun addTransition(source: ProcessState, target: ProcessState, event: ProcessEvent, initiator: UUID) =
        transitions.add(
            ProcessEventTransitionEntity(
                this,
                event,
                initiator,
                source,
                target
            )
        )
}

@Entity
class ProcessRequestEntity(

    @ManyToOne
    private val process: ProcessEntity,

    private val userId: UUID,

    @Enumerated(EnumType.STRING)
    private val type: ProcessRequestType,

    @Enumerated(EnumType.STRING)
    var state: ProcessState,

    @Enumerated(EnumType.STRING)
    val channel: ProcessChannel,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToMany(mappedBy = "processRequest", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    private val data: MutableSet<ProcessRequestDataEntity> = mutableSetOf(),

    @OneToMany(mappedBy = "processRequest", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY)
    private val stakeholders: MutableSet<ProcessRequestStakeholder> = mutableSetOf()
) :
    AuditableEntity(), Serializable {
    fun toDomain() = ProcessRequest(
        process.id ?: 0L,
        userId,
        type,
        state,
        channel,
        id ?: 0L,
        data.map { it.toDomain() }.toSet(),
        stakeholders.map { it.toDomain() }.toSet()
    )

    fun addStakeholder(type: ProcessStakeholderType, userId: String) =
        stakeholders.add(ProcessRequestStakeholder(this, userId, type))

    fun addData(name: ProcessRequestDataName, value: String) {
        val item = data.firstOrNull { it.name == name }
        if (item != null) {
            data.remove(item)
        }
        data.add(ProcessRequestDataEntity(this, name, value))
    }

    fun setDataBatch(dataMap: Map<ProcessRequestDataName, String>) {
        // No longer directly manipulating data - handled by batch insert
        // This method kept for compatibility but data insertion is now handled externally
    }
}

@Entity
data class ProcessRequestDataEntity(

    @Id
    @ManyToOne
    private val processRequest: ProcessRequestEntity,

    @Enumerated(EnumType.STRING)
    @Id val name: ProcessRequestDataName,

    private var value: String,

    ) : AuditableEntity(), Serializable {
    fun toDomain() = ProcessRequestData(processRequest.id ?: 0L, name, value)
}

@Entity
class ProcessRequestStakeholder(

    @ManyToOne
    private val processRequest: ProcessRequestEntity,

    private val stakeholderId: String,

    @Enumerated(EnumType.STRING)
    private val type: ProcessStakeholderType,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

) : AuditableEntity(), Serializable {
    fun toDomain() = ProcessStakeholder(processRequest.id ?: 0L, stakeholderId, type, id ?: 0L)
}

@Entity
class ProcessEventTransitionEntity(

    @ManyToOne
    private val process: ProcessEntity,

    @Enumerated(EnumType.STRING) val event: ProcessEvent,

    val userId: UUID,

    @Enumerated(EnumType.STRING)
    val oldState: ProcessState,

    @Enumerated(EnumType.STRING)
    val newState: ProcessState,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null

) : AuditableEntity(), Serializable {

    fun toDomain() =
        ProcessEventTransition(process.id!!, event, userId, oldState, newState, id ?: 0L)

    fun createdAt() = createdAt!!
}
