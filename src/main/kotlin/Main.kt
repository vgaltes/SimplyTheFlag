import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import java.time.Duration
import java.time.Instant

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")

    val parameterName = "/gettogethers/dev/tableName";

    val credentialsProvider =
        ProfileCredentialsProvider.builder().profileName("personal").build()

    val featureToggles = SimplyTheFlag(buildClient(credentialsProvider, Region.EU_WEST_1))
    var value = featureToggles.isEnabled(parameterName)
    println(value)
    value = featureToggles.isEnabled(parameterName)
    println(value)

}

private fun buildClient(credentialsProvider: AwsCredentialsProvider, region: Region): SsmAsyncClient =
    SsmAsyncClient
        .builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()

class SimplyTheFlag(private val ssmClient: SsmAsyncClient) {

    private val cacheDuration: Duration = Duration.ofSeconds(5)
    private var lastAccesses = mutableMapOf<String, ValueOnInstant>()

    fun isEnabled(flagName: String): String {
        val lastAccessToParameter = lastAccesses[flagName]?.accessedAt ?: Instant.MIN

        try {
            val value = if (Duration.between(lastAccessToParameter, Instant.now()) > cacheDuration) {
                val result = ssmClient.getParameter(GetParameterRequest.builder().name(flagName).build()).join()
                val value = result.parameter().value()
                lastAccesses[flagName] = ValueOnInstant(Instant.now(), value)
                println("get from ssm")
                value
            }
            else {
                println("get from cache")
                lastAccesses[flagName]!!.value
            }

            return value
        } catch (e:Exception){
            return "error"
        }
    }

    data class ValueOnInstant(val accessedAt: Instant, val value: String)
}
