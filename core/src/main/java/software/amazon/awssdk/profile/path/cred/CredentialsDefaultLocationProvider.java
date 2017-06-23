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

package software.amazon.awssdk.profile.path.cred;

import java.io.File;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.profile.path.AwsDirectoryBasePathProvider;

/**
 * Load shared credentials file from the default location (~/.aws/credentials).
 */
@SdkInternalApi
public class CredentialsDefaultLocationProvider extends AwsDirectoryBasePathProvider {

    private static final String DEFAULT_CREDENTIAL_PROFILES_FILENAME = "credentials";

    @Override
    public File getLocation() {
        File credentialProfiles = new File(getAwsDirectory(), DEFAULT_CREDENTIAL_PROFILES_FILENAME);
        if (credentialProfiles.exists() && credentialProfiles.isFile()) {
            return credentialProfiles;
        }
        return null;
    }
}
