package org.opentripplanner.datastore.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a wrapper around a ZipFile, it can be used to read the content, but not write to it. The
 * {@link #asOutputStream()} is throwing an exception.
 */
public class ZipFileDataSource
  extends AbstractFileDataSource
  implements CompositeDataSource, ZipFileEntryParent {

  private static final Logger LOG = LoggerFactory.getLogger(ZipFileDataSource.class);
  private final Collection<DataSource> content = new ArrayList<>();
  private ZipFile zipFile;

  public ZipFileDataSource(File file, FileType type) {
    super(file, type);
  }

  @Override
  public void close() {
    try {
      if (zipFile != null) {
        zipFile.close();
        content.clear();
        zipFile = null;
      }
    } catch (IOException e) {
      LOG.error(path() + " close failed. Details: " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public Collection<DataSource> content() {
    loadContent();
    return content;
  }

  @Override
  public DataSource entry(String name) {
    loadContent();
    return content.stream().filter(it -> it.name().equals(name)).findFirst().orElse(null);
  }

  @Override
  public InputStream entryStream(ZipEntry entry) {
    try {
      return zipFile.getInputStream(entry);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read " + path() + ": " + e.getLocalizedMessage(), e);
    }
  }

  /**
   * Map ZipEntries into a DataSource. This does not read the content of each zip entry, only create a
   * {@link DataSource} around it.
   */
  private static Collection<DataSource> listZipEntries(ZipFileEntryParent parent, ZipFile zipFile)
    throws IOException, ZipException {
    Collection<DataSource> content = new ArrayList<DataSource>();
    Enumeration<? extends ZipEntry> entries = zipFile.entries();

    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      content.add(new ZipFileEntryDataSource(parent, entry));
    }
    return content;
  }

  private void loadContent() {
    // Load content once
    if (zipFile != null) {
      return;
    }

    try {
      // The get name on ZipFile returns the full path, we want just the name.
      this.zipFile = new ZipFile(file, ZipFile.OPEN_READ);
      content.addAll(ZipFileDataSource.listZipEntries(this, zipFile));
    } catch (ZipException ze) {
      // Java needs help for standard ZIP files with names encoded in cp437
      // this allows decoding of utf-8 and cp437 at the same time!
      // As Cp437 is not guaranteed to be available try it but don't depend on it.
      // see APPENDIX D - Language Encoding (EFS) in the format documentation
      // https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
      // additional Recommendations for Interoperability can be found at Apache Commons
      // https://commons.apache.org/proper/commons-compress/zip.html
      //
      // See https://github.com/opentripplanner/OpenTripPlanner/pull/4835 for a
      // discussion on this.
      LOG.info(
        "Failed to read {}: {}\n" +
        "Retrying with cp437 charset just in case it was 'bad entry name'.\n" +
        "Consider sticking to ASCII characters for file names.\n" +
        "See https://github.com/opentripplanner/OpenTripPlanner/pull/4835 for a discussion on this.",
        path(),
        ze.getLocalizedMessage(),
        ze
      );

      try {
        Charset charset = Charset.forName("Cp437");
        this.zipFile = new ZipFile(file, ZipFile.OPEN_READ, charset);
        content.addAll(ZipFileDataSource.listZipEntries(this, zipFile));
      } catch (IOException e) {
        throw new RuntimeException("Failed to load " + path() + ": " + e.getLocalizedMessage(), e);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + path() + ": " + e.getLocalizedMessage(), e);
    }
  }
}
