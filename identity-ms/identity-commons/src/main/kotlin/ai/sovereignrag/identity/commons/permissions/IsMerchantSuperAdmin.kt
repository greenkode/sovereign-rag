package ai.sovereignrag.identity.commons.permissions

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('MERCHANT_SUPER_ADMIN')")
annotation class IsMerchantSuperAdmin