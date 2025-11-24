package ai.sovereignrag.commons.billpay.dto

import ai.sovereignrag.commons.billpay.ProductPropertyName
import ai.sovereignrag.commons.billpay.ProductStatus
import ai.sovereignrag.commons.billpay.ProductType
import java.util.UUID
import javax.money.MonetaryAmount

data class BillPayProductDto(

    val name: String,

    val accountDescription: String,

    val type: ProductType,

    val integratorId: String,

    val vendor: BillPayVendorDto,

    val publicId: UUID = UUID.randomUUID(),

    val status: ProductStatus = ProductStatus.ACTIVE,

    val fixedPrice: Boolean = false,

    val canLend: Boolean = false,

    val price: MonetaryAmount? = null,

    val properties: Map<ProductPropertyName, String>
) : java.io.Serializable