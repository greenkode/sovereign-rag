package ai.sovereignrag.commons.security

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('MERCHANT_SUPER_ADMIN')")
annotation class IsMerchantSuperAdmin

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('MERCHANT_SUPER_ADMIN', 'MERCHANT_ADMIN')")
annotation class IsMerchantAdmin

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('MERCHANT_SUPER_ADMIN', 'MERCHANT_ADMIN', 'MERCHANT_USER')")
annotation class IsMerchant

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_service:internal')")
annotation class IsService
