package org.opentripplanner.gtfs.graphbuilder;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.onebusaway.csv_entities.CsvInputSource;
import org.opentripplanner.utils.lang.StringUtils;

/**
 * Represent a feed id in a GTFS feed.
 */
public class GtfsFeedIdResolver {

  /**
   * A counter that will increase for each created feed id.
   */
  private static AtomicInteger FEED_ID_COUNTER = new AtomicInteger(0);

  /**
   * Extracts a feed_id from the passed source for a GTFS feed.
   * <p>
   * This will try to fetch the experimental feed_id field from the feed_info.txt file.
   * </p>
   * <p>
   * If the feed does not contain a feed_info.txt or a feed_id field, a default GtfsFeedId will be
   * created.
   * </p>
   *
   * @param source the input source
   * @return A GtfsFeedId
   * @see <a href="http://developer.trimet.org/gtfs_ext.shtml">http://developer.trimet.org/gtfs_ext.shtml</a>
   */
  public static String fromGtfsFeed(CsvInputSource source) {
    try {
      if (source.hasResource("feed_info.txt")) {
        try (InputStream feedInfoInputStream = source.getResource("feed_info.txt")) {
          CsvReader result = new CsvReader(feedInfoInputStream, StandardCharsets.UTF_8);
          result.readHeaders();
          result.readRecord();
          return normalizeId(result.get("feed_id"));
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return generateId();
  }

  /**
   * Package local to be unit-testable.
   */
  static String normalizeId(String id) {
    var cleanId = cleanId(id);
    return cleanId == null ? generateId() : cleanId;
  }

  /**
   * Creates a new GtfsFeedId based on the static sequence: 1, 2, 3 ...
   */
  private static String generateId() {
    return String.valueOf(FEED_ID_COUNTER.incrementAndGet());
  }

  /**
   * Cleans the id before it is set. This method ensures that the id is a valid id.
   */
  @Nullable
  private static String cleanId(String id) {
    if (StringUtils.hasNoValue(id)) {
      return null;
    }
    // Colon is used as a separator in OTP so that's why we strip it out - it will confuse the
    // parsers in the API (but those could be updated if someone is keen to use colons as the feed
    // id).
    return id.replace(":", "");
  }
}
