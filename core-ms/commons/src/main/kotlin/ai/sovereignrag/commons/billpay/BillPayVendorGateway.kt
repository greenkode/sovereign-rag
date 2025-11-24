package ai.sovereignrag.commons.billpay

import ai.sovereignrag.commons.billpay.dto.BillPayProductDto
import ai.sovereignrag.commons.billpay.dto.BillPayVendorDto
import java.util.UUID

interface BillPayVendorGateway {
    
    fun getActiveVendors(): List<BillPayVendorDto>
    
    fun findByPublicId(publicId: UUID): BillPayVendorDto

    fun getAllVendors(): List<BillPayVendorDto>
    
    fun getVendorsByStatus(status: BillPayVendorStatus): List<BillPayVendorDto>
    
    fun getProductsByVendorPublicId(vendorPublicId: UUID): List<BillPayProductDto>
    
    fun getActiveProductsByVendorPublicId(vendorPublicId: UUID): List<BillPayProductDto>

    fun getProductById(productId: UUID): BillPayProductDto?

    fun getVendorByProductId(productId: UUID): BillPayVendorDto?
}