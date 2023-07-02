package com.vgaltes.simplytheflag

import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest

class SSMValueRetriever(private val ssmClient: SsmAsyncClient) : ValueRetriever {
    override fun retrieve(flagName: String): String {
        val result = ssmClient.getParameter(GetParameterRequest.builder().name(flagName).build()).join()
        return result.parameter().value()
    }
}