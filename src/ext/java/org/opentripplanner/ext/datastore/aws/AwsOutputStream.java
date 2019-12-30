package org.opentripplanner.ext.datastore.aws;

import com.esotericsoftware.kryo.io.ByteBufferOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

class AwsOutputStream extends ByteBufferOutputStream implements Closeable {

  private final S3Client s3;
  private final S3Object object;
  private final List<CompletedPart> completedParts = new ArrayList<>();
  private String uploadId;
  private int partNumber = 0;

  public AwsOutputStream(S3Client s3, S3Object object) {
    super(8192);
    this.s3 = s3;
    this.object = object;
  }

  public AwsOutputStream open() {
    var request = CreateMultipartUploadRequest
      .builder()
      .bucket(object.bucket())
      .key(object.name())
      .build();
    this.uploadId = s3.createMultipartUpload(request).uploadId();
    return this;
  }

  @Override
  public void close() throws IOException {
    var completedMultipartUpload = CompletedMultipartUpload.builder().parts(completedParts).build();

    var completeRequest = CompleteMultipartUploadRequest
      .builder()
      .bucket(object.bucket())
      .key(object.name())
      .uploadId(uploadId)
      .multipartUpload(completedMultipartUpload)
      .build();

    s3.completeMultipartUpload(completeRequest);

    super.close();
  }

  @Override
  public void flush() {
    int partNumber = nextPartNumber();
    var uploadPartRequest1 = UploadPartRequest
      .builder()
      .bucket(object.bucket())
      .key(object.name())
      .uploadId(uploadId)
      .partNumber(partNumber)
      .build();

    var etag = s3
      .uploadPart(uploadPartRequest1, RequestBody.fromByteBuffer(getByteBuffer()))
      .eTag();
    completedParts.add(CompletedPart.builder().partNumber(partNumber).eTag(etag).build());

    getByteBuffer().clear();
  }

  private int nextPartNumber() {
    return ++partNumber;
  }
}
