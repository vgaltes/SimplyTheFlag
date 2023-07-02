package com.vgaltes.simplytheflag

import java.time.Instant

class FromDateFlag(override val cacheMillis: Long, private val rawParameters: String) : Flag {
    private var validFrom = Instant.MAX
    override val type: String
        get() = FromDateFlag::class.java.typeName

    override fun isEnabled(parameters: Array<out Any?>): Boolean {
        return Instant.now() >= validFrom
    }

    init {
        val jsonNode = JacksonModule.OBJECT_MAPPER.readTree(rawParameters)
        validFrom = jsonNode["validFrom"].asText().toInstant()
    }


}

private fun String.toInstant(): Instant {
    return Instant.parse(this)
}
