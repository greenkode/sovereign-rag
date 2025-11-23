//package ai.sovereignrag.audit.listener
//
//import ai.sovereignrag.audit.domain.model.AuditLog
//import ai.sovereignrag.audit.event.AuditLogEvent
//
//fun AuditLogEvent.toDomain(): ai.sovereignrag.audit.domain.model.AuditLog {
//    return AuditLog(
//        id,
//        identity.toString(),
//        identityType.toString(),
//        resource.toString(),
//        event.toString(),
//        eventTime,
//        timeRecorded,
//        payload.toString()
//    )
//}
//
