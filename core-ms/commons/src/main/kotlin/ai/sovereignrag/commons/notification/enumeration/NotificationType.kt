package ai.sovereignrag.commons.notification.enumeration

enum class NotificationType {
    PAYOUT,
    LOGIN,
    OTP,
    REGISTRATION,
    LOAN_APPLICATION_RECEIVED,
    LOAN_DISBURSED,
    LOAN_REPAYMENT_RECEIVED,
    LOAN_FULLY_REPAID,
    LOAN_OVERDUE_REMINDER,
    LOAN_DEFAULTED,
    
    // Transaction notification types
    TRANSACTION_CREATED,
    TRANSACTION_COMPLETED,
    TRANSACTION_FAILED,
    TRANSACTION_REVERSED,
    
    // Transaction type specific notifications
    DEPOSIT_COMPLETED,
    AIRTIME_PURCHASE_COMPLETED,
    DATA_PURCHASE_COMPLETED,
    ELECTRICITY_PAYMENT_COMPLETED,
    TV_PAYMENT_COMPLETED,
    EDUCATION_PAYMENT_COMPLETED,
    INSURANCE_PAYMENT_COMPLETED,
    BETTING_PAYMENT_COMPLETED,
    
    // Merchant specific notifications
    MERCHANT_SETTLEMENT,
    MERCHANT_BALANCE_LOW,
    MERCHANT_TRANSACTION_LIMIT_REACHED,
    
    // All transactions (catch-all for webhooks that want all transaction events)
    ALL_TRANSACTIONS
}
