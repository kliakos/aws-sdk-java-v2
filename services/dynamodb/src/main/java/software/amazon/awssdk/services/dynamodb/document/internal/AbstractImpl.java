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

package software.amazon.awssdk.services.dynamodb.document.internal;

import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
import software.amazon.awssdk.services.dynamodb.document.Table;

/**
 * Internal common base class for API implementations.
 */
abstract class AbstractImpl {
    private final Table table;
    private final DynamoDBClient client;

    protected AbstractImpl(DynamoDBClient client, Table table) {
        this.client = client;
        this.table = table;
    }

    /**
     * Returns the owning table.
     */
    public final Table getTable() {
        return table;
    }

    final DynamoDBClient getClient() {
        return client;
    }
}
