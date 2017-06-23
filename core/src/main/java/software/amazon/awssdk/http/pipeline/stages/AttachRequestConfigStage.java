/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.http.pipeline.stages;

import software.amazon.awssdk.RequestExecutionContext;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.pipeline.RequestToRequestPipeline;

/**
 * Attach {@link org.apache.http.client.config.RequestConfig} to the {@link SdkHttpFullRequest}.
 */
@ReviewBeforeRelease("This may be better to do in the ClientHandler shortly after marshalling")
public class AttachRequestConfigStage implements RequestToRequestPipeline {
    @Override
    public SdkHttpFullRequest execute(SdkHttpFullRequest input, RequestExecutionContext context)
            throws Exception {
        return input.toBuilder()
                    .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, context.requestConfig())
                    .build();
    }
}
