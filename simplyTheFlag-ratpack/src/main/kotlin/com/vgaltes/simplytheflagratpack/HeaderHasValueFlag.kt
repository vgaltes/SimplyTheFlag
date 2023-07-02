package com.vgaltes.simplytheflagratpack

import com.vgaltes.simplytheflag.Flag
import com.vgaltes.simplytheflag.JacksonModule
import ratpack.handling.Context

class HeaderHasValueFlag(override val cacheMillis: Long, private val rawParameters: String) : Flag {

    private var expectedValue: String
    private val headerName: String
    override val type: String
        get() = HeaderHasValueFlag::class.java.typeName
    override fun isEnabled(parameters: Array<out Any?>): Boolean {
        val context = parameters.first() as Context

        return context.header(headerName).get() == expectedValue
    }

    init {
        val jsonNode = JacksonModule.OBJECT_MAPPER.readTree(rawParameters)
        headerName = jsonNode["headerName"].asText()
        expectedValue = jsonNode["expectedValue"].asText()
    }
}