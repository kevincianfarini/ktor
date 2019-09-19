/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.http

import io.ktor.util.*
import io.ktor.utils.io.charsets.*

/**
 * Options for URL Encoding.
 * Keys and values are encoded only when [encodeKey] and [encodeValue] are `true` respectively.
 */
@KtorExperimentalAPI
enum class UrlEncodingOption(internal val encodeKey: Boolean, internal val encodeValue: Boolean) {
    DEFAULT(true, true),
    KEY_ONLY(true, false),
    VALUE_ONLY(false, true),
    NO_ENCODING(false, false)
}

/**
 * Parse URL query parameters. Shouldn't be used for urlencoded forms because of `+` character.
 */
@KtorExperimentalAPI
fun String.parseUrlEncodedParameters(defaultEncoding: Charset = Charsets.UTF_8, limit: Int = 1000): Parameters {
    val parameters: List<Pair<String, String>> =
        split("&", limit = limit).map { it.substringBefore("=") to it.substringAfter("=", "") }
    val encoding: String =
        parameters.firstOrNull { it.first == "_charset_" }?.second ?: defaultEncoding.name

    val charset = Charset.forName(encoding)
    return Parameters.build {
        parameters.forEach { (key, value) ->
            append(
                key.decodeURLQueryComponent(charset = charset),
                value.decodeURLQueryComponent(charset = charset)
            )
        }
    }
}

/**
 * Encode form parameters from a list of pairs
 */
@Deprecated(
    "Use formUrlEncode(option)",
    ReplaceWith("formUrlEncode(UrlEncodingOption.DEFAULT)", "io.ktor.http.UrlEncodingOption"),
    DeprecationLevel.HIDDEN
)
fun List<Pair<String, String?>>.formUrlEncode(): String = formUrlEncode(UrlEncodingOption.DEFAULT)

/**
 * Encode form parameters from a list of pairs by using [option]
 */
fun List<Pair<String, String?>>.formUrlEncode(option: UrlEncodingOption = UrlEncodingOption.DEFAULT): String =
    buildString { formUrlEncodeTo(this, option) }

/**
 * Encode form parameters from a list of pairs to the specified [out] appendable
 */
@Deprecated(
    "Use formUrlEncodeTo(out, option)",
    ReplaceWith("formUrlEncodeTo(out, UrlEncodingOption.DEFAULT)", "io.ktor.http.UrlEncodingOption"),
    DeprecationLevel.HIDDEN
)
fun List<Pair<String, String?>>.formUrlEncodeTo(out: Appendable) = formUrlEncodeTo(out, UrlEncodingOption.DEFAULT)

/**
 * Encode form parameters from a list of pairs to the specified [out] appendable by using [option]
 */
fun List<Pair<String, String?>>.formUrlEncodeTo(
    out: Appendable,
    option: UrlEncodingOption = UrlEncodingOption.DEFAULT
) {
    joinTo(
        out, "&"
    ) {
        val key = if (option.encodeKey) it.first.encodeURLParameter(spaceToPlus = true) else it.first
        if (it.second == null) {
            key
        } else {
            val nonNullValue = it.second.toString()
            val value = if (option.encodeValue) nonNullValue.encodeURLParameter(spaceToPlus = true) else nonNullValue
            "$key=$value"
        }
    }
}

/**
 * Encode form parameters
 */
fun Parameters.formUrlEncode(): String = entries()
    .flatMap { e -> e.value.map { e.key to it } }
    .formUrlEncode(urlEncodingOption())

/**
 * Encode form parameters to the specified [out] appendable
 */
fun Parameters.formUrlEncodeTo(out: Appendable) {
    entries()
        .flatMap { e -> if (e.value.isEmpty()) listOf(e.key to null) else e.value.map { e.key to it } }
        .formUrlEncodeTo(out, urlEncodingOption())
}
