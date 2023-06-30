import JacksonModule.OBJECT_MAPPER
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmAsyncClient

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

interface Flag {
    val type: String
    val cacheMillis: Long
    fun evaluate(value: Any): Boolean
}


class BooleanFlag(override val cacheMillis: Long, private val rawParameters: String) : Flag {
    private var enabled = false
    override val type: String
        get() = BooleanFlag::class.java.typeName

    override fun evaluate(value: Any): Boolean {
        return enabled
    }

    init {
        val jsonNode = OBJECT_MAPPER.readTree(rawParameters)
        enabled = jsonNode["enabled"].asBoolean()
    }
}