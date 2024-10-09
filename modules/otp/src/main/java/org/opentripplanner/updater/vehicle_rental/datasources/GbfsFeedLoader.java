package org.opentripplanner.updater.vehicle_rental.datasources;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFS;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeeds;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
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
  private final HttpHeaders httpHeaders;
  private final OtpHttpClient otpHttpClient;

  static {
    objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  public GbfsFeedLoader(
    String url,
    HttpHeaders httpHeaders,
    String languageCode,
    OtpHttpClient otpHttpClient
  ) {
    this.httpHeaders = httpHeaders;
    this.otpHttpClient = otpHttpClient;
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new UpdaterConstructionException("Invalid url " + url);
    }

    // Fetch autoconfiguration file
    GBFS data = fetchFeed(uri, httpHeaders, GBFS.class);
    if (data == null) {
      if (!url.endsWith("gbfs.json")) {
        LOG.warn(
          "GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.",
          url
        );
      }
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

  GbfsFeedLoader(String url, HttpHeaders httpHeaders, String languageCode) {
    this(url, httpHeaders, languageCode, new OtpHttpClientFactory().create(LOG));
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

  private <T> T fetchFeed(URI uri, HttpHeaders httpHeaders, Class<T> clazz) {
    try {
      return otpHttpClient.getAndMapAsJsonObject(uri, httpHeaders.asMap(), objectMapper, clazz);
    } catch (OtpHttpClientException e) {
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
      T newData = fetchFeed(url, httpHeaders, implementingClass);
      if (newData == null) {
        LOG.warn("Could not fetch GBFS data for {}. Retrying.", url);
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
