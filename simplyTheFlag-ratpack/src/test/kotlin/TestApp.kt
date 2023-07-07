import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.vgaltes.simplytheflag.SSMValueRetriever
import com.vgaltes.simplytheflag.SimplyTheFlag
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import ratpack.guice.Guice
import ratpack.handling.Context
import ratpack.handling.Handler
import ratpack.server.RatpackServer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import javax.inject.Singleton

object TestApp {

    val localStackContainer = initSSMLocalStack()

    @JvmStatic
    fun main(args: Array<String>) {
        RatpackServer.start { s ->
            s.registry(
            Guice.registry { bindings ->
                bindings.module(TestModule::class.java)
            })
            .handlers { chain ->
                chain
                    .get("headerExists", HeaderExistsHandler::class.java)
                    .get("headerHasValue", HeaderHasValueHandler::class.java)
            }
        }
    }

    private fun initSSMLocalStack(): LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
        .withServices(LocalStackContainer.Service.SSM)
        .withReuse(true)
        .apply {
            start()
        }
}

class TestModule : AbstractModule() {
    @Singleton
    @Provides
    fun getSimplyTheFlag(): SimplyTheFlag {
        val valueRetriever = SSMValueRetriever(buildClient(TestApp.localStackContainer))

        return SimplyTheFlag(valueRetriever)
    }

    @Singleton
    @Provides
    fun getHeaderExistsHandler(
        simplyTheFlag: SimplyTheFlag
    ) : HeaderExistsHandler {
        return HeaderExistsHandler(simplyTheFlag)
    }

    @Singleton
    @Provides
    fun getHeaderHasValueHandler(
        simplyTheFlag: SimplyTheFlag
    ) : HeaderHasValueHandler {
        return HeaderHasValueHandler(simplyTheFlag)
    }

    private fun buildClient(container: LocalStackContainer): SsmAsyncClient = SsmAsyncClient
        .builder()
        .endpointOverride(container.getEndpointOverride((LocalStackContainer.Service.SSM)))
        .region(Region.of(container.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    container.accessKey,
                    container.secretKey,
                ),
            ),
        )
        .build()
}

class HeaderExistsHandler(private val flag: SimplyTheFlag) : Handler {
    override fun handle(ctx: Context) {
        if(flag.state("headerExistsFlag", ctx).getOrNull() == true)
            ctx.response.status(200).send("headerExists")
        else ctx.response.status(200).send("headerNotExists")
    }
}

class HeaderHasValueHandler(private val flag: SimplyTheFlag) : Handler {
    override fun handle(ctx: Context) {
        if(flag.state("headerHasValueFlag", ctx).getOrNull() == true)
            ctx.response.status(200).send("headerHasValue")
        else ctx.response.status(200).send("headerNotHasValue")
    }
}