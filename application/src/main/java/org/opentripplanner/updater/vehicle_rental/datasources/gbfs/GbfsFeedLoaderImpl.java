package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing the state and loading of complete GBFS version 2.2 and 2.3 datasets, and updating them according
 * to individual feed's TTL rules.
 */
public abstract class GbfsFeedLoaderImpl<N, F extends GbfsFeedDetails<N>>
  implements GbfsFeedLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFeedLoaderImpl.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /** One updater per feed type(?) */
  private final Map<N, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();
  private final HttpHeaders httpHeaders;
  private final OtpHttpClient otpHttpClient;

  static {
    objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  public GbfsFeedLoaderImpl(List<F> feeds, HttpHeaders httpHeaders, OtpHttpClient otpHttpClient) {
    this.httpHeaders = httpHeaders;
    this.otpHttpClient = otpHttpClient;

    // Create updater for each file
    for (var feed : feeds) {
      var feedName = feed.getName();
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
  @Override
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
    GBFSFeedUpdater<?> updater = feedUpdaters.get(nameForClass(feed));
    if (updater == null) {
      return null;
    }
    return feed.cast(updater.getData());
  }

  protected abstract <T> N nameForClass(Class<T> feed);

  protected abstract <T> Class<T> classForName(N name);

  /* protected static methods */

  protected static <T> T fetchFeed(
    URI uri,
    HttpHeaders httpHeaders,
    OtpHttpClient otpHttpClient,
    Class<T> clazz
  ) {
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

    private GBFSFeedUpdater(F feed) {
      url = feed.getUrl();
      implementingClass = classForName(feed.getName());
    }

    private T getData() {
      return data;
    }

    private boolean fetchData() {
      T newData = fetchFeed(url, httpHeaders, otpHttpClient, implementingClass);
      if (newData == null) {
        LOG.warn("Could not fetch GBFS data for {}. Retrying.", url);
        nextUpdate = getCurrentTimeSeconds();
        return false;
      }
      data = newData;

      try {
        // Fetch lastUpdated and ttl from the resulting class. Due to type erasure we don't know the actual
        // class, and have to use introspection to get the method references, as they do not share a supertype.
        Object lastUpdatedValue = implementingClass.getMethod("getLastUpdated").invoke(newData);
        Integer lastUpdated = lastUpdatedValue == null
          ? null
          : (lastUpdatedValue instanceof Date
              ? (int) ((Date) lastUpdatedValue).getTime()
              : (Integer) lastUpdatedValue);
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
