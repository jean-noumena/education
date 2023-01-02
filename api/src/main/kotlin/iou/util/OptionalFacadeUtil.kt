package iou.util

import com.noumenadigital.codegen.Amount
import com.noumenadigital.codegen.asOptional
import com.noumenadigital.npl.api.generated.lang.core.NoneFacade
import com.noumenadigital.npl.api.generated.lang.core.OptionalFacade
import com.noumenadigital.npl.api.generated.lang.core.SomeFacade

inline fun <reified T : Any?> toOptionalFacade(type: T?): OptionalFacade<T> {
    return if (null == type) {
        OptionalFacade(NoneFacade())
    } else {
        OptionalFacade(SomeFacade.create(type))
    }
}

fun toOptionalFacade(number: Int?) = toOptionalFacade(number?.toBigDecimal())

fun toOptionalFacade(number: Double?, symbol: String) =
    toOptionalFacade(number?.let { Amount(it.toBigDecimal(), symbol) })

fun toOptionalFacade(number: Int?, symbol: String) =
    toOptionalFacade(number?.let { Amount(it.toBigDecimal(), symbol) })

inline fun <reified T : Any, reified U : Any> OptionalFacade<T>.getOrNull(crossinline convert: (T) -> U?) =
    this.asOptional().map { convert(it) }.orElse(null)

inline fun <reified T : Any> OptionalFacade<T>.getOrNull() = this.getOrNull { it }
