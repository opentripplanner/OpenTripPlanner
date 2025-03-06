package org.opentripplanner.ext.datastore.gs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.file.ZipStreamDataSourceDecorator;

/**
 * This data store uses the local file system to access in-/out- data files.
 */
public class GsDataSourceRepository implements DataSourceRepository {

  private final String credentialsFilename;
  private Storage storage;

  public GsDataSourceRepository(String credentialsFilename) {
    this.credentialsFilename = credentialsFilename;
  }

  @Override
  public String description() {
    return "Google Cloud Storage";
  }

  @Override
  public void open() {
    this.storage = connectToStorage();
  }

  @Override
  public DataSource findSource(URI uri, FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    BlobId blobId = GsHelper.toBlobId(uri);
    return createSource(blobId, type);
  }

  @Override
  public CompositeDataSource findCompositeSource(URI uri, FileType type) {
    if (skipUri(uri)) {
      return null;
    }
    return createCompositeSource(GsHelper.toBlobId(uri), type);
  }

  /* private methods */

  private static boolean skipUri(URI uri) {
    return !"gs".equals(uri.getScheme());
  }

  private DataSource createSource(BlobId blobId, FileType type) {
    Blob blob = storage.get(blobId);

    if (blob != null) {
      return new GsFileDataSource(blob, type);
    } else {
      return new GsOutFileDataSource(storage, blobId, type);
    }
  }

  private CompositeDataSource createCompositeSource(BlobId blobId, FileType type) {
    if (GsHelper.isRoot(blobId)) {
      return new GsDirectoryDataSource(storage, blobId, type);
    }

    if (blobId.getName().endsWith(".zip")) {
      Blob blob = storage.get(blobId);

      if (blob == null) {
        throw new IllegalArgumentException(
          type.text() + " not found: " + GsHelper.toUriString(blobId)
        );
      }
      DataSource gsSource = new GsFileDataSource(blob, type);
      return new ZipStreamDataSourceDecorator(gsSource);
    }
    return new GsDirectoryDataSource(storage, blobId, type);
  }

  private Storage connectToStorage() {
    try {
      StorageOptions.Builder builder = StorageOptions.getDefaultInstance().toBuilder();

      if (credentialsFilename != null) {
        GoogleCredentials credentials = GoogleCredentials.fromStream(
          new FileInputStream(credentialsFilename)
        ).createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        builder.setCredentials(credentials);
      }
      return builder.build().getService();
    } catch (IOException e) {
      throw new RuntimeException(e.getLocalizedMessage(), e);
    }
  }
}
