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

package software.amazon.awssdk.transfer.s3.internal;

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3ArnConverter;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.CompletedDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileDownload;
import software.amazon.awssdk.transfer.s3.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.CompletedUpload;
import software.amazon.awssdk.transfer.s3.DirectoryDownload;
import software.amazon.awssdk.transfer.s3.DirectoryUpload;
import software.amazon.awssdk.transfer.s3.Download;
import software.amazon.awssdk.transfer.s3.DownloadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.DownloadRequest;
import software.amazon.awssdk.transfer.s3.FileDownload;
import software.amazon.awssdk.transfer.s3.FileUpload;
import software.amazon.awssdk.transfer.s3.S3ClientConfiguration;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.S3TransferManagerOverrideConfiguration;
import software.amazon.awssdk.transfer.s3.Upload;
import software.amazon.awssdk.transfer.s3.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.UploadRequest;
import software.amazon.awssdk.transfer.s3.internal.progress.TransferProgressUpdater;
import software.amazon.awssdk.utils.CompletableFutureUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class DefaultS3TransferManager implements S3TransferManager {
    private final S3CrtAsyncClient s3CrtAsyncClient;
    private final TransferManagerConfiguration transferConfiguration;
    private final UploadDirectoryHelper uploadDirectoryHelper;
    private final DownloadDirectoryHelper downloadDirectoryHelper;

    public DefaultS3TransferManager(DefaultBuilder tmBuilder) {
        transferConfiguration = resolveTransferManagerConfiguration(tmBuilder);
        s3CrtAsyncClient = initializeS3CrtClient(tmBuilder);
        uploadDirectoryHelper = new UploadDirectoryHelper(transferConfiguration, this::uploadFile);
        ListObjectsHelper listObjectsHelper = new ListObjectsHelper(s3CrtAsyncClient::listObjectsV2);
        downloadDirectoryHelper = new DownloadDirectoryHelper(transferConfiguration,
                                                              listObjectsHelper,
                                                              this::downloadFile);
    }

    @SdkTestInternalApi
    DefaultS3TransferManager(S3CrtAsyncClient s3CrtAsyncClient,
                             UploadDirectoryHelper uploadDirectoryHelper,
                             TransferManagerConfiguration configuration,
                             DownloadDirectoryHelper downloadDirectoryHelper) {
        this.s3CrtAsyncClient = s3CrtAsyncClient;
        this.transferConfiguration = configuration;
        this.uploadDirectoryHelper = uploadDirectoryHelper;
        this.downloadDirectoryHelper = downloadDirectoryHelper;
    }

    private static TransferManagerConfiguration resolveTransferManagerConfiguration(DefaultBuilder tmBuilder) {
        TransferManagerConfiguration.Builder transferConfigBuilder = TransferManagerConfiguration.builder();
        tmBuilder.transferManagerConfiguration.uploadDirectoryConfiguration()
                                              .ifPresent(transferConfigBuilder::uploadDirectoryConfiguration);
        tmBuilder.transferManagerConfiguration.executor().ifPresent(transferConfigBuilder::executor);
        return transferConfigBuilder.build();
    }

    private static S3CrtAsyncClient initializeS3CrtClient(DefaultBuilder tmBuilder) {
        S3CrtAsyncClient.S3CrtAsyncClientBuilder clientBuilder = S3CrtAsyncClient.builder();
        tmBuilder.s3ClientConfiguration.credentialsProvider().ifPresent(clientBuilder::credentialsProvider);
        tmBuilder.s3ClientConfiguration.maxConcurrency().ifPresent(clientBuilder::maxConcurrency);
        tmBuilder.s3ClientConfiguration.minimumPartSizeInBytes().ifPresent(clientBuilder::minimumPartSizeInBytes);
        tmBuilder.s3ClientConfiguration.region().ifPresent(clientBuilder::region);
        tmBuilder.s3ClientConfiguration.targetThroughputInGbps().ifPresent(clientBuilder::targetThroughputInGbps);

        return clientBuilder.build();
    }

    @Override
    public Upload upload(UploadRequest uploadRequest) {
        Validate.paramNotNull(uploadRequest, "uploadRequest");

        AsyncRequestBody requestBody = uploadRequest.requestBody();
        
        CompletableFuture<CompletedUpload> uploadFuture = new CompletableFuture<>();
        
        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadRequest, requestBody);
        progressUpdater.transferInitiated();
        requestBody = progressUpdater.wrapRequestBody(requestBody);
        progressUpdater.registerCompletion(uploadFuture);

        try {
            assertNotUnsupportedArn(uploadRequest.putObjectRequest().bucket(), "upload");

            CompletableFuture<PutObjectResponse> putObjFuture =
                s3CrtAsyncClient.putObject(uploadRequest.putObjectRequest(), requestBody);

            // Forward upload cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(uploadFuture, putObjFuture);

            CompletableFutureUtils.forwardTransformedResultTo(putObjFuture, uploadFuture,
                                                              r -> CompletedUpload.builder()
                                                                                  .response(r)
                                                                                  .build());
        } catch (Throwable throwable) {
            uploadFuture.completeExceptionally(throwable);
        }

        return new DefaultUpload(uploadFuture, progressUpdater.progress());
    }
    
    @Override
    public FileUpload uploadFile(UploadFileRequest uploadFileRequest) {
        Validate.paramNotNull(uploadFileRequest, "uploadFileRequest");

        AsyncRequestBody requestBody = AsyncRequestBody.fromFile(uploadFileRequest.source());

        CompletableFuture<CompletedFileUpload> uploadFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(uploadFileRequest, requestBody);
        progressUpdater.transferInitiated();
        requestBody = progressUpdater.wrapRequestBody(requestBody);
        progressUpdater.registerCompletion(uploadFuture);

        try {
            assertNotUnsupportedArn(uploadFileRequest.putObjectRequest().bucket(), "upload");

            CompletableFuture<PutObjectResponse> putObjFuture =
                s3CrtAsyncClient.putObject(uploadFileRequest.putObjectRequest(), requestBody);
            
            // Forward upload cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(uploadFuture, putObjFuture);

            CompletableFutureUtils.forwardTransformedResultTo(putObjFuture, uploadFuture,
                                                              r -> CompletedFileUpload.builder()
                                                                                      .response(r)
                                                                                      .build());
        } catch (Throwable throwable) {
            uploadFuture.completeExceptionally(throwable);
        }

        return new DefaultFileUpload(uploadFuture, progressUpdater.progress());
    }

    @Override
    public DirectoryUpload uploadDirectory(UploadDirectoryRequest uploadDirectoryRequest) {
        Validate.paramNotNull(uploadDirectoryRequest, "uploadDirectoryRequest");
        
        try {
            assertNotUnsupportedArn(uploadDirectoryRequest.bucket(), "uploadDirectory");

            return uploadDirectoryHelper.uploadDirectory(uploadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultDirectoryUpload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public <ResultT> Download<ResultT> download(DownloadRequest<ResultT> downloadRequest) {
        Validate.paramNotNull(downloadRequest, "downloadRequest");

        AsyncResponseTransformer<GetObjectResponse, ResultT> responseTransformer =
            downloadRequest.responseTransformer();

        CompletableFuture<CompletedDownload<ResultT>> downloadFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(downloadRequest, null);
        progressUpdater.transferInitiated();
        responseTransformer = progressUpdater.wrapResponseTransformer(responseTransformer);
        progressUpdater.registerCompletion(downloadFuture);

        try {
            assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");

            CompletableFuture<ResultT> getObjectFuture =
                s3CrtAsyncClient.getObject(downloadRequest.getObjectRequest(), responseTransformer);

            // Forward download cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(downloadFuture, getObjectFuture);

            CompletableFutureUtils.forwardTransformedResultTo(getObjectFuture, downloadFuture,
                                                              r -> CompletedDownload.builder()
                                                                                    .result(r)
                                                                                    .build());
        } catch (Throwable throwable) {
            downloadFuture.completeExceptionally(throwable);
        }

        return new DefaultDownload<>(downloadFuture, progressUpdater.progress());
    }

    @Override
    public FileDownload downloadFile(DownloadFileRequest downloadRequest) {
        Validate.paramNotNull(downloadRequest, "downloadFileRequest");

        AsyncResponseTransformer<GetObjectResponse, GetObjectResponse> responseTransformer =
            AsyncResponseTransformer.toFile(downloadRequest.destination());

        CompletableFuture<CompletedFileDownload> downloadFuture = new CompletableFuture<>();

        TransferProgressUpdater progressUpdater = new TransferProgressUpdater(downloadRequest, null);
        progressUpdater.transferInitiated();
        responseTransformer = progressUpdater.wrapResponseTransformer(responseTransformer);
        progressUpdater.registerCompletion(downloadFuture);

        try {
            assertNotUnsupportedArn(downloadRequest.getObjectRequest().bucket(), "download");

            CompletableFuture<GetObjectResponse> getObjectFuture =
                s3CrtAsyncClient.getObject(downloadRequest.getObjectRequest(), responseTransformer);

            // Forward download cancellation to CRT future
            CompletableFutureUtils.forwardExceptionTo(downloadFuture, getObjectFuture);

            CompletableFutureUtils.forwardTransformedResultTo(getObjectFuture, downloadFuture,
                                                              r -> CompletedFileDownload.builder()
                                                                                        .response(r)
                                                                                        .build());
        } catch (Throwable throwable) {
            downloadFuture.completeExceptionally(throwable);
        }

        return new DefaultFileDownload(downloadFuture, progressUpdater.progress());
    }

    @Override
    public DirectoryDownload downloadDirectory(DownloadDirectoryRequest downloadDirectoryRequest) {
        Validate.paramNotNull(downloadDirectoryRequest, "downloadDirectoryRequest");

        try {
            assertNotUnsupportedArn(downloadDirectoryRequest.bucket(), "downloadDirectoryRequest");

            return downloadDirectoryHelper.downloadDirectory(downloadDirectoryRequest);
        } catch (Throwable throwable) {
            return new DefaultDirectoryDownload(CompletableFutureUtils.failedFuture(throwable));
        }
    }

    @Override
    public void close() {
        s3CrtAsyncClient.close();
        transferConfiguration.close();
    }

    public static Builder builder() {
        return new DefaultBuilder();
    }

    private static void assertNotUnsupportedArn(String bucket, String operation) {
        if (!bucket.startsWith("arn:")) {
            return;
        }

        if (isObjectLambdaArn(bucket)) {
            String error = String.format("%s does not support S3 Object Lambda resources", operation);
            throw new IllegalArgumentException(error);
        }

        Arn arn = Arn.fromString(bucket);

        if (isMrapArn(arn)) {
            String error = String.format("%s does not support S3 multi-region access point ARN", operation);
            throw new IllegalArgumentException(error);
        }
    }

    private static boolean isObjectLambdaArn(String arn) {
        return arn.contains(":s3-object-lambda");
    }

    private static boolean isMrapArn(Arn arn) {
        S3Resource s3Resource = S3ArnConverter.create().convertArn(arn);

        S3AccessPointResource s3EndpointResource =
            Validate.isInstanceOf(S3AccessPointResource.class, s3Resource,
                                  "An ARN was passed as a bucket parameter to an S3 operation, however it does not "
                                  + "appear to be a valid S3 access point ARN.");

        return !s3EndpointResource.region().isPresent();
    }

    private static final class DefaultBuilder implements S3TransferManager.Builder {
        private S3ClientConfiguration s3ClientConfiguration = S3ClientConfiguration.builder().build();
        private S3TransferManagerOverrideConfiguration transferManagerConfiguration =
            S3TransferManagerOverrideConfiguration.builder().build();

        private DefaultBuilder() {
        }

        @Override
        public Builder s3ClientConfiguration(S3ClientConfiguration configuration) {
            this.s3ClientConfiguration = configuration;
            return this;
        }

        @Override
        public Builder transferConfiguration(S3TransferManagerOverrideConfiguration transferManagerConfiguration) {
            this.transferManagerConfiguration = transferManagerConfiguration;
            return this;
        }

        @Override
        public S3TransferManager build() {
            return new DefaultS3TransferManager(this);
        }
    }
}
