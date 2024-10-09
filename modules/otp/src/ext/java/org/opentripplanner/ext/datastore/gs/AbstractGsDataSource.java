package org.opentripplanner.ext.datastore.gs;

import com.google.cloud.storage.BlobId;
import java.net.URI;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

abstract class AbstractGsDataSource implements DataSource {

  private final BlobId blobId;
  private final FileType type;

  AbstractGsDataSource(BlobId blobId, FileType type) {
    this.blobId = blobId;
    this.type = type;
  }

  @Override
  public final String name() {
    return blobId.getName();
  }

  @Override
  public final String path() {
    return GsHelper.toUriString(blobId);
  }

  @Override
  public URI uri() {
    return URI.create(path());
  }

  @Override
  public final FileType type() {
    return type;
  }

  @Override
  public final String toString() {
    return type + " " + path();
  }

  BlobId blobId() {
    return blobId;
  }

  String bucketName() {
    return blobId.getBucket();
  }
}
