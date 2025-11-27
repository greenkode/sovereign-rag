package ai.sovereignrag.notification.core.repository

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.notification.core.entity.MessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MessageRepository : JpaRepository<MessageEntity, Long> {
    fun findByPublicId(publicId: UUID): MessageEntity?

    fun findByClientIdentifier(clientIdentifier: String): MessageEntity?

    @Modifying
    @Query("UPDATE MessageEntity m SET m.response = :response, m.deliveryStatus = :status WHERE m.clientIdentifier = :clientIdentifier")
    fun updateMessageResponse(clientIdentifier: String, response: String?, status: DeliveryStatus)
}
