package org.opentripplanner.updater.vehicle_rental.datasources;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.entur.gbfs.v2_2.gbfs.GBFS;
import org.entur.gbfs.v2_2.gbfs.GBFSFeed;
import org.entur.gbfs.v2_2.gbfs.GBFSFeedName;
import org.entur.gbfs.v2_2.gbfs.GBFSFeeds;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.updater.UpdaterConstructionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFeedLoader.class);

  private static final ObjectMapper objectMapper = new ObjectMapper();
  /** One updater per feed type(?) */
  private final Map<GBFSFeedName, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();
  private final Map<String, String> httpHeaders;

  static {
    objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  public GbfsFeedLoader(String url, Map<String, String> httpHeaders, String languageCode) {
    this.httpHeaders = httpHeaders;
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new UpdaterConstructionException("Invalid url " + url);
    }

    if (!url.endsWith("gbfs.json")) {
      LOG.warn(
        "GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.",
        url
      );
    }

    // Fetch autoconfiguration file
    GBFS data = fetchFeed(uri, httpHeaders, GBFS.class);
    if (data == null) {
      throw new UpdaterConstructionException(
        "Could not fetch the feed auto-configuration file from " + uri
      );
    }

    // Pick first language if none defined
    GBFSFeeds feeds = languageCode == null
      ? data.getFeedsData().values().iterator().next()
      : data.getFeedsData().get(languageCode);
    if (feeds == null) {
      throw new UpdaterConstructionException(
        "Language " + languageCode + " does not exist in feed " + uri
      );
    }

    // Create updater for each file
    for (GBFSFeed feed : feeds.getFeeds()) {
      GBFSFeedName feedName = feed.getName();
      if (feedUpdaters.containsKey(feedName)) {
        throw new UpdaterConstructionException(
          "Feed contains duplicate url for feed " +
          feedName +
          ". " +
          "Urls: " +
          feed.getUrl() +
          ", " +
          feedUpdaters.get(feedName).url
        );
      }

      // name is null, if the file is of unknown type, skip those
      if (feed.getName() != null) {
        feedUpdaters.put(feedName, new GBFSFeedUpdater<>(feed));
      }
    }
  }

  /**
   * Checks if any of the feeds should be updated base on the TTL and fetches. Returns true, if any
   * feeds were updated.
   */
  public boolean update() {
    boolean didUpdate = false;

    for (GBFSFeedUpdater<?> updater : feedUpdaters.values()) {
      if (updater.shouldUpdate()) {
        boolean success = updater.fetchData();
        if (!success) {
          return false;
        }
        didUpdate = true;
      }
    }

    return didUpdate;
  }

  /**
   * Gets the most recent contents of the feed, which contains an object of type T.
   */
  public <T> T getFeed(Class<T> feed) {
    GBFSFeedUpdater<?> updater = feedUpdaters.get(GBFSFeedName.fromClass(feed));
    if (updater == null) {
      return null;
    }
    return feed.cast(updater.getData());
  }

  /* private static methods */

  private static <T> T fetchFeed(URI uri, Map<String, String> httpHeaders, Class<T> clazz) {
    try (InputStream is = HttpUtils.openInputStream(uri, httpHeaders);) {
      if (is == null) {
        LOG.warn("Failed to get data from url {}", uri);
        return null;
      }
      return objectMapper.readValue(is, clazz);
    } catch (IllegalArgumentException | IOException e) {
      LOG.warn("Error parsing vehicle rental feed from {}. Details: {}.", uri, e.getMessage(), e);
      return null;
    }
  }

  /* private static classes */

  private class GBFSFeedUpdater<T> {

    /** URL for the individual GBFS file */
    private final URI url;

    /** To which class should the file be deserialized to */
    private final Class<T> implementingClass;

    private int nextUpdate;
    private T data;

    private GBFSFeedUpdater(GBFSFeed feed) {
      url = feed.getUrl();
      implementingClass = (Class<T>) feed.getName().implementingClass();
    }

    private T getData() {
      return data;
    }

    private boolean fetchData() {
      T newData = GbfsFeedLoader.fetchFeed(url, httpHeaders, implementingClass);
      if (newData == null) {
        LOG.error("Invalid data for {}", url);
        nextUpdate = getCurrentTimeSeconds();
        return false;
      }
      data = newData;

      try {
        // Fetch lastUpdated and ttl from the resulting class. Due to type erasure we don't know the actual
        // class, and have to use introspection to get the method references, as they do not share a supertype.
        Integer lastUpdated = (Integer) implementingClass
          .getMethod("getLastUpdated")
          .invoke(newData);
        Integer ttl = (Integer) implementingClass.getMethod("getTtl").invoke(newData);
        if (lastUpdated == null || ttl == null) {
          nextUpdate = getCurrentTimeSeconds();
        } else {
          nextUpdate = lastUpdated + ttl;
        }
      } catch (
        NoSuchMethodException
        | InvocationTargetException
        | IllegalAccessException
        | ClassCastException e
      ) {
        LOG.error("Invalid lastUpdated or ttl for {}", url);
        nextUpdate = getCurrentTimeSeconds();
      }
      return true;
    }

    private boolean shouldUpdate() {
      return getCurrentTimeSeconds() >= nextUpdate;
    }

    private int getCurrentTimeSeconds() {
      return (int) (System.currentTimeMillis() / 1000);
    }
  }
}
