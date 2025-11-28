package ai.sovereignrag.identity.core.entity

enum class CompanySize {
    SIZE_1_10,
    SIZE_11_50,
    SIZE_51_200,
    SIZE_201_500,
    SIZE_501_1000,
    SIZE_1001_PLUS;

    companion object {
        fun fromDisplayValue(value: String): CompanySize = when (value) {
            "1-10" -> SIZE_1_10
            "11-50" -> SIZE_11_50
            "51-200" -> SIZE_51_200
            "201-500" -> SIZE_201_500
            "501-1000" -> SIZE_501_1000
            "1001+" -> SIZE_1001_PLUS
            else -> valueOf(value)
        }
    }

    fun toDisplayValue(): String = when (this) {
        SIZE_1_10 -> "1-10"
        SIZE_11_50 -> "11-50"
        SIZE_51_200 -> "51-200"
        SIZE_201_500 -> "201-500"
        SIZE_501_1000 -> "501-1000"
        SIZE_1001_PLUS -> "1001+"
    }
}
