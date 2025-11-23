package ai.sovereignrag.notification.infrastructure

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<MessageEntity, Long> {

    @Modifying
    @Query("UPDATE MessageEntity m SET m.response = :response, m.deliveryStatus = :status WHERE m.clientIdentifier = :clientIdentifier")
    fun updateMessageResponse(clientIdentifier: String, response: String?, status: DeliveryStatus)
}