package com.vgaltes.simplytheflag

import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import java.io.File
import java.net.URL
import java.time.Duration
import java.time.Instant

class SimplyTheFlag(private val valueRetriever: ValueRetriever) {

    private val flagsRetrieved = mutableMapOf<String, CachedValue>()
    private val availableFlags = mutableMapOf<String, String>()
    private val lastErrors = mutableMapOf<String, Exception>()

    fun isEnabled(flagName: String): Boolean {
        try {
            lastErrors.remove(flagName)
            val lastRetrievedFlagValue = flagsRetrieved[flagName]

            val flagRetrievedAt = lastRetrievedFlagValue?.retrievedAt ?: Instant.MIN
            val cacheDuration = lastRetrievedFlagValue?.cacheDuration ?: Duration.ZERO

            val cachedValue = if(flagRetrievedAt + cacheDuration < Instant.now()) {
                val value = retrieveFlagValueFromProvider(flagName)
                flagsRetrieved[flagName] = value
                value
            } else {
                flagsRetrieved[flagName]!!
            }

            return cachedValue.value
        }
        catch (e: Exception) {
            lastErrors[flagName] = e
            return false
        }
    }

    fun hasFailed(flagName: String): Boolean = lastErrors.containsKey(flagName)
    fun lastError(flagName: String): Exception? = lastErrors[flagName]

    init {
        findFlags()
    }

    private fun retrieveFlagValueFromProvider(flagName: String): CachedValue {
        val rawFlag = valueRetriever.retrieve(flagName)
        val flag = createFlag(rawFlag)
        val value = flag.isEnabled()
        return CachedValue(Instant.now(), Duration.ofMillis(flag.cacheMillis), value)
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

    data class CachedValue(val retrievedAt: Instant, val cacheDuration: Duration, val value: Boolean)

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

                    val isFlag = Class.forName(fullyQualifiedClassName).interfaces.any { i -> i.simpleName == Flag::class.java.simpleName }
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

interface ValueRetriever {
    fun retrieve(flagName: String): String
}

class SSMValueRetriever(private val ssmClient: SsmAsyncClient) : ValueRetriever{
    override fun retrieve(flagName: String): String {
        val result = ssmClient.getParameter(GetParameterRequest.builder().name(flagName).build()).join()
        return result.parameter().value()
    }
}