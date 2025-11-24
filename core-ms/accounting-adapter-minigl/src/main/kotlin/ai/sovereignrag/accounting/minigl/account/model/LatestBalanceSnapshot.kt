package ai.sovereignrag.accounting.minigl.account.model

import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.io.Serializable
import java.math.BigDecimal

@Entity
@Table(name = "latest_balance_snapshot")
@IdClass(LatestBalanceSnapshotId::class)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
data class LatestBalanceSnapshot(
    
    @Id
    @Column(name = "journal")
    var journal: Long = 0,
    
    @Id
    @Column(name = "account")
    var account: String = "",
    
    @Id
    @Column(name = "layers")
    var layers: String = "",
    
    @Column(name = "balance", precision = 14, scale = 2)
    var balance: BigDecimal = BigDecimal.ZERO,
    
    @Column(name = "currency")
    var currency: String = ""
) : Serializable

@Embeddable
data class LatestBalanceSnapshotId(
    var journal: Long = 0,
    var account: String = "",
    var layers: String = ""
) : Serializable