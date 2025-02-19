/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADERS;
import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZ_ID_2_HEADER;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * A centralized set of loggers that used across the SDK to log particular types of events. SDK users can then specifically enable
 * just these loggers to get the type of that they want instead of having to enable all logging.
 */
@SdkProtectedApi
public final class SdkStandardLogger {
    /**
     * Logger providing detailed information on requests/responses. Users can enable this logger to get access to AWS request IDs
     * for responses, individual requests and parameters sent to AWS, etc.
     */
    public static final Logger REQUEST_LOGGER = Logger.loggerFor("software.amazon.awssdk.request");

    /**
     * Logger used for the purpose of logging the request id extracted either from the
     * http response header or from the response body.
     */
    public static final Logger REQUEST_ID_LOGGER = Logger.loggerFor("software.amazon.awssdk.requestId");

    private SdkStandardLogger() {
    }

    /**
     * Log the response status code and request ID
     */
    public static void logRequestId(SdkHttpResponse response) {
        String placeholder = "not available";
        String requestId = String.format("Request ID: %s, Extended Request ID: %s",
                                         SdkHttpUtils.firstMatchingHeaderFromCollection(response.headers(),
                                                                                        X_AMZN_REQUEST_ID_HEADERS)
                                                     .orElse(placeholder),
                                         response.firstMatchingHeader(X_AMZ_ID_2_HEADER)
                                                 .orElse(placeholder));
        Supplier<String> logStatement = () -> String.format("Received %s response: %s, %s",
                                                            response.isSuccessful() ? "successful" : "failed",
                                                            response.statusCode(),
                                                            requestId);
        REQUEST_ID_LOGGER.debug(logStatement);
        REQUEST_LOGGER.debug(logStatement);
    }
}
