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

package software.amazon.awssdk.opensdk.protect.auth;

import software.amazon.awssdk.auth.Aws4Signer;
import software.amazon.awssdk.auth.RequestSigner;

/**
 * An interface used to represent the default
 * AWS RequestSigner (typically {@link Aws4Signer})
 * used for requests that require IAM authorization
 */
public interface IamRequestSigner extends RequestSigner {
}
