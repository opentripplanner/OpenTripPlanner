package org.opentripplanner.graph_builder.module;

import com.csvreader.CsvReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.onebusaway.csv_entities.CsvInputSource;

/**
 * Represent a feed id in a GTFS feed.
 */
public class GtfsFeedId {

  /**
   * A counter that will increase for each created feed id.
   */
  private static int FEED_ID_COUNTER = 1;

  /**
   * The id for the feed
   */
  private final String id;

  /**
   * Constructs a new feed id.
   * <p>
   * If the passed id is null or an empty string a unique feed id will be generated.
   *
   * @param id The feed id
   */
  private GtfsFeedId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public static class Builder {

    private String id;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    /**
     * Extracts a feed_id from the passed source for a GTFS feed.
     * <p>
     * This will try to fetch the experimental feed_id field from the feed_info.txt file.
     * </p>
     * <p>
     * If the feed does not contain a feed_info.txt or a feed_id field a default GtfsFeedId will be
     * created.
     * </p>
     *
     * @param source the input source
     * @return A GtfsFeedId
     * @see <a href="http://developer.trimet.org/gtfs_ext.shtml">http://developer.trimet.org/gtfs_ext.shtml</a>
     */
    public Builder fromGtfsFeed(CsvInputSource source) {
      try {
        if (source.hasResource("feed_info.txt")) {
          try (InputStream feedInfoInputStream = source.getResource("feed_info.txt")) {
            CsvReader result = new CsvReader(feedInfoInputStream, StandardCharsets.UTF_8);
            result.readHeaders();
            result.readRecord();
            this.id = result.get("feed_id");
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return this;
    }

    /**
     * Creates a new GtfsFeedId.
     *
     * @return A GtfsFeedId
     */
    public GtfsFeedId build() {
      id = cleanId(id);
      if (id == null) {
        id = String.valueOf(FEED_ID_COUNTER);
      }
      FEED_ID_COUNTER++;
      return new GtfsFeedId(id);
    }

    /**
     * Cleans the id before it is set. This method ensures that the id is a valid id.
     *
     * @param id The feed id
     * @return The cleaned id.
     */
    private String cleanId(String id) {
      if (id == null || id.trim().length() == 0) {
        return null;
      }
      // Colon is used as a separator in OTP so that's why we strip it out - it will confuse the
      // parsers in the API (but those could be updated if someone is keen to use colons as the feed
      // id).
      return id.replace(":", "");
    }
  }
}
