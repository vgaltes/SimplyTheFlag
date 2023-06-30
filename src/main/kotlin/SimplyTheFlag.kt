import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import java.time.Duration
import java.time.Instant

class SimplyTheFlag(private val ssmClient: SsmAsyncClient) {

    private val cacheDuration: Duration = Duration.ofSeconds(5)
    private var lastAccesses = mutableMapOf<String, ValueOnInstant>()

    fun isEnabled(flagName: String): Boolean {
        val lastAccessToParameter = lastAccesses[flagName]?.accessedAt ?: Instant.MIN

        try {
            val value = if (Duration.between(lastAccessToParameter, Instant.now()) > cacheDuration) {
                val result = ssmClient.getParameter(GetParameterRequest.builder().name(flagName).build()).join()
                val rawFlag = result.parameter().value()
                val flag = parseBooleanFlag(rawFlag)
                val value = flag.evaluate(0)
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

    private fun parseBooleanFlag(rawFlag: String): BooleanFlag {
        val flagDefinition = JacksonModule.OBJECT_MAPPER.readTree(rawFlag)
        val cacheMillis = flagDefinition["cacheMillis"].asLong()
        val rawParameters = flagDefinition["parameters"].toString()
        return BooleanFlag(cacheMillis, rawParameters)
    }

    data class ValueOnInstant(val accessedAt: Instant, val value: Boolean)
}