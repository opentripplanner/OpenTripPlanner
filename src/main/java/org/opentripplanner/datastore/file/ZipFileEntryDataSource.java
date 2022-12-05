package org.opentripplanner.datastore.file;

import java.io.InputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;

public class ZipFileEntryDataSource implements DataSource {

  private final ZipFileEntryParent parent;
  private final ZipEntry entry;

  ZipFileEntryDataSource(ZipFileEntryParent parent, ZipEntry entry) {
    this.parent = parent;
    this.entry = entry;
  }

  @Override
  public String name() {
    return entry.getName();
  }

  @Override
  public String path() {
    return name() + " (" + parent.path() + ")";
  }

  @Override
  public URI uri() {
    return URI.create(path());
  }

  @Override
  public FileType type() {
    return parent.type();
  }

  @Override
  public long size() {
    return entry.getSize();
  }

  @Override
  public long lastModified() {
    return entry.getLastModifiedTime().toMillis();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public InputStream asInputStream() {
    return parent.entryStream(entry);
  }

  @Override
  public String toString() {
    return type() + " " + path();
  }
}
