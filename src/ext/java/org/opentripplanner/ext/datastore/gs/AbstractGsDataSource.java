package org.opentripplanner.ext.datastore.gs;

import com.google.cloud.storage.BlobId;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;

abstract class AbstractGsDataSource implements DataSource {
    private final BlobId blobId;
    private final FileType type;

     AbstractGsDataSource(BlobId blobId, FileType type) {
        this.blobId = blobId;
        this.type = type;
    }

    BlobId blobId() {
        return blobId;
    }

    String bucketName() {
        return blobId.getBucket();
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
    public final FileType type() {
        return type;
    }

    @Override
    public final String toString() {
        return type + " " + path();
    }
}
