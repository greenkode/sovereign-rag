package ai.sovereignrag.commons.billpay

import ai.sovereignrag.commons.process.enumeration.ProcessType

enum class ProductType(
    val description: String,
    val iconUrl: String,
    val display: Boolean,
    val order: Int
) {
    TV("Cable TV", "636c2144-0c7f-496b-a633-42a0e7928e54", true, 0),

    ELECTRICITY("Electricity", "e1ca604b-9ff8-49ce-841b-3525a86fd81f", true, 1),

    INTERNET("Internet", "223bbfc0-570c-4822-b9dd-91b7c6b40d22", false, 2),

    EDUCATION("Education", "d5a6db70-c545-4d48-8649-f1ef36b79ff3", true, 3),

    INSURANCE("Insurance", "0e5c479e-a7e8-4ecb-8ba5-7420c61f84c3", true, 4),

    BETTING("Sport Betting", "79e86b0e-8876-459c-8088-71f73e701fa2", true, 5),

    AIRTIME("Airtime", "488940ea-58ae-4370-9867-3c77a0634219", true, 6),

    DATA("Data", "27c74bb1-2a8a-4fcb-8f99-fd00cef2b900", true, 7);

    fun toProcessType() =
        when (this) {
            TV -> ProcessType.TV
            ELECTRICITY -> ProcessType.ELECTRICITY
            INTERNET -> ProcessType.INTERNET
            EDUCATION -> ProcessType.EDUCATION
            INSURANCE -> ProcessType.INSURANCE
            BETTING -> ProcessType.BETTING
            AIRTIME -> ProcessType.AIRTIME
            DATA -> ProcessType.DATA
        }
}
