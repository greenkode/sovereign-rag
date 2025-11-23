package ai.sovereignrag.identity.commons

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import mu.KotlinLogging.logger
import java.io.Serializable
import java.util.Locale

data class PhoneNumber(val value: String, val locale: Locale) : Serializable {

    init {
        isValid()
    }

    @Transient
    private val log = logger {}

    fun toInternationalFormat(): String {
        try {
            val util = PhoneNumberUtil.getInstance()

            val number = util.parse(value, locale.country)

            if (!util.isValidNumber(number)) throw RuntimeException(
                "The phone number $value is not a valid ${locale.country} number."
            )

            return util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: NumberParseException) {
            log.error(e) { e.message }
            throw RuntimeException("Invalid phone number format [$value]")
        }
    }

    fun isValid(): Boolean {
        val util = PhoneNumberUtil.getInstance()

        try {
            val number = util.parse(value, locale.country)

            return util.isValidNumber(number)
        } catch (e: NumberParseException) {

            log.error(e) { e.message }

            return false
        }
    }

    fun toNationalFormat(): String {
        try {
            val util = PhoneNumberUtil.getInstance()

            val number = util.parse(value, locale.country)

            util.isValidNumber(number)

            val numberInNationalFormat =
                util.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL).replace(" ", "")

            return numberInNationalFormat.replace("-", "")
        } catch (e: NumberParseException) {
            log.error(e) { e.message }
        }
        throw RuntimeException("Invalid phone number format [$value]")
    }

    fun maskPhoneNumber(remaining: Int): String {
        if (value.length <= remaining) {
            return value
        }
        val maskLen = value.length - remaining
        return "*".repeat(maskLen) + value.substring(maskLen)
    }

    fun maskPhoneNumber(): String {
        if (value.length <= 4) {
            return value
        }
        val maskLen = value.length - 4
        return "*".repeat(maskLen) + value.substring(maskLen)
    }
}