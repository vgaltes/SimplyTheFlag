package com.vgaltes.simplytheflag

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

    val featureToggles = SimplyTheFlag(SSMValueRetriever(buildClient(credentialsProvider, Region.EU_WEST_1)), "com.vgaltes.simplytheflag")
    var value = featureToggles.state(parameterName)
    println(value)
    value = featureToggles.state(parameterName)
    println(value)

}

private fun buildClient(credentialsProvider: AwsCredentialsProvider, region: Region): SsmAsyncClient =
    SsmAsyncClient
        .builder()
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()




