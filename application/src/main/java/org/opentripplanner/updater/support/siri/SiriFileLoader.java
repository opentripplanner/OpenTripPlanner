package org.opentripplanner.updater.support.siri;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.Siri;

/**
 * Load real-time updates from SIRI-SX and SIRI-ET feeds from a local directory. The
 * files are renamed during the processing like this:
 * <pre>
 * update.xml  ➞  update.xml.inProgress  ➞  ( update.xml.ok | update.xml.failed )
 * </pre>
 * The renaming of the file guarantees that the file is not processed twice. A {@code .ok} file
 * indicate that the file is processed and the content parsed ok. The status of the update
 * might be different - see logs for update errors.
 * <p>
 * The file updater will pick up any file matching {@code *.xml} in the configured directory.
 */
public class SiriFileLoader implements SiriLoader {

  private static final Logger LOG = LoggerFactory.getLogger(SiriFileLoader.class);
  public static final String SUFFIX_IN_PROGRESS = ".inProgress";
  public static final String SUFFIX_OK = ".ok";
  public static final String SUFFIX_FAILED = ".failed";
  private final File directory;

  public SiriFileLoader(String url) {
    try {
      this.directory = new File(new URL(url).toURI());

      if (!directory.exists() || !directory.isDirectory()) {
        throw new IllegalArgumentException("Could not find directory: " + url);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  public static boolean matchesUrl(String url) {
    return url.startsWith("file:");
  }

  /**
   * Send a SIRI-SX service request and unmarshal the response as JAXB.
   */
  @Override
  public Optional<Siri> fetchSXFeed(String requestorRef) {
    return fetchFeed();
  }

  /**
   * Send a SIRI-ET service request and unmarshal the response as JAXB.
   */
  @Override
  public Optional<Siri> fetchETFeed(String requestorRef) {
    return fetchFeed();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private Optional<Siri> fetchFeed() {
    File[] files = directory.listFiles();
    if (files == null) {
      return Optional.empty();
    }

    for (File file : files) {
      if (!matchFilename(file)) {
        continue;
      }
      LOG.info("Process real-time input file: " + file.getAbsolutePath());
      var inProgressFile = newFile(file, SUFFIX_IN_PROGRESS);
      try {
        file.renameTo(inProgressFile);
        Siri siri = null;

        try (InputStream is = new FileInputStream(inProgressFile)) {
          siri = SiriHelper.unmarshal(is);
          inProgressFile.renameTo(newFile(file, SUFFIX_OK));
          return Optional.of(siri);
        }
      } catch (Exception ex) {
        inProgressFile.renameTo(newFile(file, SUFFIX_FAILED));
        throw new RuntimeException(ex.getMessage(), ex);
      }
    }
    return Optional.empty();
  }

  private static boolean matchFilename(File file) {
    return file.getName().endsWith(".xml");
  }

  private static File newFile(File originalFile, String suffix) {
    return new File(originalFile.getParentFile(), originalFile.getName() + suffix);
  }
}
