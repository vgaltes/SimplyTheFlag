package com.vgaltes.simplytheflag

import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import java.io.File
import java.net.URL
import java.time.Duration
import java.time.Instant

class SimplyTheFlag(private val ssmClient: SsmAsyncClient) {

    private val cacheDuration: Duration = Duration.ofSeconds(5)
    private val lastAccesses = mutableMapOf<String, ValueOnInstant>()
    private val availableFlags = mutableMapOf<String, String>()

    fun isEnabled(flagName: String): Boolean {
        val lastAccessToParameter = lastAccesses[flagName]?.accessedAt ?: Instant.MIN

        try {
            val value = if (Duration.between(lastAccessToParameter, Instant.now()) > cacheDuration) {
                val result = ssmClient.getParameter(GetParameterRequest.builder().name(flagName).build()).join()
                val rawFlag = result.parameter().value()
                val flag = createFlag(rawFlag) // treure aixo a fora pq pugui accedir a cacheMillis. Fer test
                val value = flag.isEnabled()
                lastAccesses[flagName] = ValueOnInstant(Instant.now(), value)
                value
            }
            else {
                lastAccesses[flagName]!!.value
            }

            return value
        } catch (e:Exception){
            return false
        }
    }

    private fun createFlag(rawFlag: String): Flag {
        val flagDefinition = JacksonModule.OBJECT_MAPPER.readTree(rawFlag)
        val type = flagDefinition["type"].asText()
        val cacheMillis = flagDefinition["cacheMillis"].asLong()
        val rawParameters = flagDefinition["parameters"].toString()
        val booleanFlag = Class.forName(availableFlags[type]).constructors.find { it.parameterCount == 2 }
            ?.newInstance(cacheMillis, rawParameters)
        return booleanFlag as Flag
    }

    data class ValueOnInstant(val accessedAt: Instant, val value: Boolean)

    init {
        findFlags()
    }

    private fun findFlags() {
        // Translate the package name into an absolute path
        val packageName = "com.vgaltes.simplytheflag"
        var name = packageName
        if (!name.startsWith("/")) {
            name = "/$name"
        }
        name = name.replace('.', '/')
        // Get a File object for the package
        val url: URL = SimplyTheFlag::class.java.getResource(name)
        val directory = File(url.file)

        if (directory.exists()) {
            // Get the list of the files contained in the package
            directory.walk()
                .filter { f -> f.isFile && !f.name.contains('$') && f.name.endsWith(".class") }
                .forEach { it ->
                    val className = it.canonicalPath.removePrefix(directory.canonicalPath)
                        .dropLast(6) // remove .class
                        .drop(1) // drop initial .
                        .replace('/', '.')
                    val fullyQualifiedClassName = "$packageName.$className"

                    val isFlag = Class.forName(fullyQualifiedClassName).interfaces.any { i -> i.simpleName == "Flag" }
                    if (isFlag) {
                        availableFlags[className] = fullyQualifiedClassName
                    }
                }
        }
    }
}

interface Flag {
    val type: String
    val cacheMillis: Long
    fun isEnabled(): Boolean
}