package org.opentripplanner.ext.datastore.gs;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

/**
 * This is a an adapter to to simulate a file directory on a GCS. Files created using an instance of
 * this class wil have a common namespace. It does only support creating new output sources, it can
 * not be used to list files with the common namespace (directory path).
 */
public class GsDirectoryDataSource extends AbstractGsDataSource implements CompositeDataSource {

  private final Storage storage;

  GsDirectoryDataSource(Storage storage, BlobId blobId, FileType type) {
    super(blobId, type);
    this.storage = storage;
  }

  @Override
  public boolean exists() {
    return getBucket()
      .list(
        Storage.BlobListOption.prefix(name()),
        Storage.BlobListOption.pageSize(1),
        Storage.BlobListOption.currentDirectory()
      )
      .getValues()
      .iterator()
      .hasNext();
  }

  @Override
  public Collection<DataSource> content() {
    Collection<DataSource> content = new ArrayList<>();
    forEachChildBlob(blob -> content.add(new GsFileDataSource(blob, type())));
    return content;
  }

  @Override
  public DataSource entry(String name) {
    Blob blob = childBlob(name);
    // If file exist
    if (blob != null) {
      return new GsFileDataSource(blob, type());
    }
    // New file
    BlobId childBlobId = BlobId.of(bucketName(), childPath(name));
    return new GsOutFileDataSource(storage, childBlobId, type());
  }

  @Override
  public void delete() {
    forEachChildBlob(Blob::delete);
  }

  @Override
  public void close() {}

  /* private methods */

  private Bucket getBucket() {
    Bucket bucket = storage.get(bucketName());
    if (bucket == null) {
      throw new IllegalArgumentException("Bucket not found: " + bucketName());
    }
    return bucket;
  }

  private Blob childBlob(String name) {
    return getBucket().get(childPath(name));
  }

  private String childPrefix() {
    return GsHelper.isRoot(blobId()) ? "" : name() + "/";
  }

  private String childPath(String name) {
    return childPrefix() + name;
  }

  private void forEachChildBlob(Consumer<Blob> consumer) {
    int pathIndex = childPrefix().length();
    for (Blob blob : listBlobs().iterateAll()) {
      String name = blob.getName().substring(pathIndex);
      // Skip nested content
      if (!name.contains("/")) {
        consumer.accept(blob);
      }
    }
  }

  private Page<Blob> listBlobs() {
    return getBucket().list(Storage.BlobListOption.prefix(childPrefix()));
  }
}
