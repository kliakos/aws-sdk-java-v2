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

package software.amazon.awssdk.util;

import software.amazon.awssdk.utils.Base16;
import software.amazon.awssdk.utils.Base64Utils;

/**
 * Currently provided encoding schemes "out of the box".
 *
 * See http://www.ietf.org/rfc/rfc4648.txt
 *
 * @author Hanson Char
 */
public enum EncodingSchemeEnum implements EncodingScheme {
    BASE16 {
        @Override
        public String encodeAsString(byte[] bytes) {
            return Base16.encodeAsString(bytes);
        }

        @Override
        public byte[] decode(String encoded) {
            return Base16.decode(encoded);
        }
    },
    BASE64 {
        @Override
        public String encodeAsString(byte[] bytes) {
            return Base64Utils.encodeAsString(bytes);
        }

        @Override
        public byte[] decode(String encoded) {
            return Base64Utils.decode(encoded);
        }
    },;

    @Override
    public abstract String encodeAsString(byte[] bytes);
}
