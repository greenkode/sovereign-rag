package ai.sovereignrag.audit.security

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_service:internal')")
annotation class IsService

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('MERCHANT_SUPER_ADMIN', 'MERCHANT_ADMIN', 'MERCHANT_USER')")
annotation class IsMerchant
