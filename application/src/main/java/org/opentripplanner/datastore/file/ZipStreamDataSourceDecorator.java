package org.opentripplanner.datastore.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.base.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This decorator help unzip the content of any underling data source(the delegate). This make it
 * easier to provide a store implementation - since this code can be reused.
 * <p/>
 * See the Google Cloud Store implementation for an example on hwo to use it.
 */
public class ZipStreamDataSourceDecorator implements CompositeDataSource {

  /**
   * Maximum size of an uncompressed zip entry in memory. Larger entries are stored on disk.
   */
  private static final int DEFAULT_MAX_ZIP_ENTRY_SIZE_IN_MEMORY = 2_000_000_000;

  private static final Logger LOG = LoggerFactory.getLogger(ZipStreamDataSourceDecorator.class);

  private final DataSource delegate;
  private final int maxZipEntrySizeInMemory;

  /**
   * This store load the zip file into memory; hence we should load the content only once, even at
   * the risk that the source is changed since last tile a resource is accessed. To achieve this we
   * use a boolean flag to indicate if the content is loaded or not.
   */
  private boolean contentLoaded = false;

  /**
   * Cache content from first access to this data source is closed.
   */
  private List<DataSource> content = new ArrayList<>();

  /**
   * Create a Zip Stream data source decorator around another data source. The given delegate is
   * responsible for retrieving meta-data and providing an input stream to fetch the zipped
   * content.
   */
  public ZipStreamDataSourceDecorator(DataSource delegate) {
    this(delegate, DEFAULT_MAX_ZIP_ENTRY_SIZE_IN_MEMORY);
  }

  ZipStreamDataSourceDecorator(DataSource delegate, int maxZipEntrySizeInMemory) {
    this.delegate = delegate;
    this.maxZipEntrySizeInMemory = maxZipEntrySizeInMemory;
  }

  @Override
  public String name() {
    return delegate.name();
  }

  @Override
  public String path() {
    return delegate.path();
  }

  @Override
  public URI uri() {
    return URI.create(path());
  }

  @Override
  public FileType type() {
    return delegate.type();
  }

  @Override
  public long size() {
    return delegate.size();
  }

  @Override
  public long lastModified() {
    return delegate.lastModified();
  }

  @Override
  public boolean exists() {
    return delegate.exists();
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public InputStream asInputStream() {
    throw new UnsupportedOperationException(
      "This datasource type " + type() + " do not support READING. Can not read from: " + path()
    );
  }

  @Override
  public OutputStream asOutputStream() {
    throw new UnsupportedOperationException(
      "This datasource type " + type() + " do not support WRITING. Can not write to: " + path()
    );
  }

  @Override
  public String detailedInfo() {
    return delegate.detailedInfo();
  }

  @Override
  public Collection<DataSource> content() {
    loadContent();
    return content;
  }

  @Override
  public DataSource entry(String name) {
    loadContent();
    return content.stream().filter(it -> name.equals(it.name())).findFirst().orElse(null);
  }

  @Override
  public void close() {
    if (content != null) {
      for (DataSource dataSource : content) {
        if (dataSource instanceof TemporaryFileDataSource tempDataSource) {
          tempDataSource.deleteFile();
        }
      }
    }
    // Make the content available for GC.
    content = null;
  }

  @Override
  public String toString() {
    return path();
  }

  private void loadContent() {
    if (content == null) {
      throw new NullPointerException(
        "The content is accessed after the zip file is closed: " + path()
      );
    }

    if (contentLoaded) {
      return;
    }
    contentLoaded = true;

    // We support both gzip and unzipped files when reading.
    try (ZipInputStream zis = new ZipInputStream(delegate.asInputStream())) {
      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
        // we only support flat ZIP files
        if (entry.isDirectory()) {
          continue;
        }
        uncompressEntry(zis, entry);
      }
    } catch (ZipException ex) {
      throw new RuntimeException(
        "Can not read zip file " + path() + ": " + ex.getLocalizedMessage(),
        ex
      );
    } catch (IOException ie) {
      throw new RuntimeException("Failed to load " + path() + ": " + ie.getLocalizedMessage(), ie);
    }
  }

  private void uncompressEntry(ZipInputStream zis, ZipEntry entry) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream(4048);
    long nbCopiedBytes = copy(zis, buf, maxZipEntrySizeInMemory + 1L);
    byte[] byteArray = buf.toByteArray();
    if (nbCopiedBytes <= maxZipEntrySizeInMemory) {
      content.add(
        new ByteArrayDataSource(
          entry.getName() + " (" + path() + ")",
          entry.getName(),
          type(),
          byteArray.length,
          entry.getLastModifiedTime().toMillis(),
          false
        ).withBytes(byteArray)
      );
    } else {
      LOG.info(
        "The entry {} in the zip datasource {} is larger than 2GB. It will be stored on disk",
        entry.getName(),
        name()
      );
      File tmpFile = Files.createTempFile(null, null).toFile();
      try (OutputStream outputStream = Files.newOutputStream(tmpFile.toPath())) {
        outputStream.write(byteArray);
        zis.transferTo(outputStream);
      }
      content.add(new TemporaryFileDataSource(entry.getName(), tmpFile, type()));
    }
  }

  /**
   * Copies at maximum maxLength bytes from inputStream to outputStream
   * Inlined partially from IOUtils.copyLarge
   */
  private long copy(InputStream inputStream, OutputStream outputStream, long maxLength)
    throws IOException {
    byte[] buffer = new byte[8192];
    int bytesToRead = buffer.length;
    int read;
    long totalRead = 0;
    while (bytesToRead > 0 && (read = inputStream.read(buffer, 0, bytesToRead)) != -1) {
      outputStream.write(buffer, 0, read);
      totalRead += read;
      // Note the cast must work because buffer.length is an integer
      bytesToRead = (int) Math.min(maxLength - totalRead, buffer.length);
    }
    return totalRead;
  }
}
