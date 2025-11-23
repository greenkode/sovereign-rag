package ai.sovereignrag.commons.accounting

enum class TransactionType(val description: String, val accountCharged: AccountCharged, val group: TransactionGroup, val merchantEnabled: Boolean) {
    DEPOSIT("Deposit", AccountCharged.RECIPIENT, TransactionGroup.INBOUND, merchantEnabled = true),
    REVERSAL("Reversal", AccountCharged.SENDER, TransactionGroup.P2P, merchantEnabled = true),
    LIEN_AMOUNT("Lien Amount", AccountCharged.SENDER, TransactionGroup.P2P, merchantEnabled = false),
    UNLIEN_AMOUNT("Unlien Amount", AccountCharged.SENDER, TransactionGroup.P2P, merchantEnabled = false),

    AIRTIME("Airtime Purchase", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    DATA("Data Purchase", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    TV("TV Bill Payment", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    EVENT("Event Bill Payment", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    ELECTRICITY("Electricity Bill Payment", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = true),

    EDUCATION("Education Bill Payment", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    INSURANCE("Insurance Payment", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    BETTING("Betting", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    LOAN("Loan", AccountCharged.RECIPIENT, TransactionGroup.BILL_PAYMENT, merchantEnabled = false),

    FUND_POOL_ACCOUNT("Fund Pool Account", AccountCharged.SENDER, TransactionGroup.P2P, merchantEnabled = false),

}

enum class AccountCharged {
    SENDER, RECIPIENT
}

enum class TransactionGroup {

    P2P,
    INBOUND,
    OUTBOUND,
    BILL_PAYMENT,
}

enum class TransactionStatus(val description: String) {

    PENDING("Transaction Pending"),
    COMPLETED("Transaction Complete"),
    REVERSED("Transaction Reversed"),
    FAILED("Transaction Failed")
}

enum class EntryType {

    AMOUNT,
    FEE,
    VAT,
    COMMISSION,
    REBATE
}

enum class TransactionPropertyGroup {
    TRANSACTION_HISTORY,
    INTERNAL
}

enum class TransactionPropertyName {

    IS_PREPAID,
    PREPAID_TOKEN,
    NARRATION
}

enum class AccountType(
    val skipLimitsCheck: Boolean,
    val internal: Boolean,
    val defaultParent: String,
    val padding: Int
) {

    CASH(true, true, "assets", 3),
    EQUITY(true, true, "equity", 3),
    BUSINESS(false, false, "customer-liabilities", 8),
    REVENUE(true, true, "revenue", 8),
    EXPENSE(true, true, "expenses", 8),
    LOCK(true, true, "customer-liabilities", 8),
    POOL(true, true, "assets", 4),
}

enum class AccountAddressType {

    BLOCKCHAIN,
    BANK_ACCOUNT,
    CHART_OF_ACCOUNTS
}

enum class AccountStatus {

    ENABLED,
    BLOCKED,
    PENDING,
    POST_NO_DEBIT,
    POST_NO_CREDIT,
    ARCHIVED
}