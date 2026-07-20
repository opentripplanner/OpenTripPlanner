package org.opentripplanner.gbfs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for managing the state and loading of complete GBFS datasets, and updating them
 * according to individual feed's TTL rules.
 */
public abstract class GbfsFeedLoaderImpl<N, F extends GbfsFeedDetails<N>>
  implements GbfsFeedLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFeedLoaderImpl.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  public static final String HEADER_ETAG = "ETag";
  public static final String HEADER_IF_NONE_MATCH = "If-None-Match";

  /** One updater per feed type(?) */
  private final Map<N, GBFSFeedUpdater<?>> feedUpdaters = new HashMap<>();
  private final HttpHeaders httpHeaders;
  private final OtpHttpClient otpHttpClient;

  static {
    OBJECT_MAPPER.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
  }

  public GbfsFeedLoaderImpl(List<F> feeds, HttpHeaders httpHeaders, OtpHttpClient otpHttpClient) {
    this.httpHeaders = httpHeaders;
    this.otpHttpClient = otpHttpClient;

    // Create updater for each file
    for (var feed : feeds) {
      var feedName = feed.getName();
      if (feedUpdaters.containsKey(feedName)) {
        throw new GbfsConstructionException(
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

  /**
   * Fetches the GBFS auto-configuration file (gbfs.json) and returns it as a JSON tree. Returning
   * the raw tree allows the caller to determine the GBFS version before mapping the file onto the
   * version-specific model with {@link #mapAutoConfiguration(JsonNode, String, Class)}, so the
   * file needs to be fetched only once. Some servers throttle or fail repeated requests for the
   * same resource, so the file must not be fetched twice during setup.
   */
  public static JsonNode fetchAutoConfiguration(
    URI uri,
    HttpHeaders httpHeaders,
    OtpHttpClient otpHttpClient
  ) {
    try {
      return otpHttpClient.getAndMapAsJsonNode(uri, httpHeaders, OBJECT_MAPPER);
    } catch (OtpHttpClientException e) {
      LOG.warn("Error fetching GBFS feed from {}. Details: {}.", uri, e.getMessage(), e);
      if (!uri.toString().endsWith("gbfs.json")) {
        LOG.warn(
          "GBFS autoconfiguration url {} does not end with gbfs.json. Make sure it follows the specification, if you get any errors using it.",
          uri
        );
      }
      throw new GbfsConstructionException(
        "Could not fetch the feed auto-configuration file from " + uri
      );
    }
  }

  /**
   * Maps an already fetched auto-configuration file onto the version-specific model class.
   */
  protected static <T> T mapAutoConfiguration(
    JsonNode autoConfiguration,
    String url,
    Class<T> clazz
  ) {
    try {
      return OBJECT_MAPPER.treeToValue(autoConfiguration, clazz);
    } catch (JsonProcessingException e) {
      LOG.warn(
        "Error parsing GBFS auto-configuration file from {}. Details: {}.",
        url,
        e.getMessage(),
        e
      );
      throw new GbfsConstructionException(
        "Could not parse the feed auto-configuration file from " + url
      );
    }
  }

  protected static URI toUri(String url) {
    try {
      return new URI(url);
    } catch (URISyntaxException e) {
      throw new GbfsConstructionException("Invalid url " + url);
    }
  }

  /**
   * Fetches a feed with conditional request support (ETag/If-None-Match).
   * Returns the response including status code and headers.
   */
  private <T> T fetchFeedWithResponse(
    URI uri,
    HttpHeaders httpHeaders,
    String etag,
    OtpHttpClient.ResponseMapper<T> contentMapper
  ) {
    // Build headers with If-None-Match if ETag is available
    var headersBuilder = HttpHeaders.of().add(httpHeaders);
    if (etag != null && !etag.isEmpty()) {
      headersBuilder.add(HEADER_IF_NONE_MATCH, etag);
    }

    return otpHttpClient.getAndMap(uri, null, headersBuilder.build(), contentMapper);
  }

  public record GBFSFeedResponse<T>(T body, @Nullable String etag, boolean statusNotModified) {}

  private class GBFSFeedUpdater<T> {

    /** URL for the individual GBFS file */
    private final URI url;

    /** To which class should the file be deserialized to */
    private final Class<T> implementingClass;

    private int nextUpdate;
    private T data;
    private String etag;

    private GBFSFeedUpdater(F feed) {
      url = feed.getUrl();
      implementingClass = classForName(feed.getName());
    }

    private T getData() {
      return data;
    }

    private boolean fetchData() {
      try {
        GBFSFeedResponse<T> response = fetchFeedWithResponse(
          url,
          httpHeaders,
          etag,
          otpHttpResponse ->
            new GBFSFeedResponse<>(
              otpHttpResponse.statusOk()
                ? OBJECT_MAPPER.readValue(otpHttpResponse.body(), implementingClass)
                : null,
              otpHttpResponse.header(HEADER_ETAG).orElse(null),
              otpHttpResponse.statusNotModified()
            )
        );

        // Handle 304 Not Modified - data hasn't changed
        if (response.statusNotModified) {
          LOG.debug("GBFS feed {} not modified (304), reusing cached data", url);
          nextUpdate = getCurrentTimeSeconds();
          return true;
        }

        if (response.body == null) {
          throw new IllegalStateException("Unexpected null body");
        }

        data = response.body;
        etag = response.etag;
        if (etag != null) {
          LOG.debug("Stored ETag {} for GBFS feed {}", etag, url);
        }

        updateNextFetchTime(response.body);
        return true;
      } catch (OtpHttpClientException e) {
        LOG.warn("Could not fetch GBFS data for {}", url, e);
        nextUpdate = getCurrentTimeSeconds();
        return false;
      } catch (Exception e) {
        LOG.warn("Exception while processing GBFS data for {}", url, e);
        nextUpdate = getCurrentTimeSeconds();
        return false;
      }
    }

    private void updateNextFetchTime(T feedData) {
      try {
        // Fetch lastUpdated and ttl from the resulting class. Due to type erasure we don't know the actual
        // class, and have to use introspection to get the method references, as they do not share a supertype.
        Object lastUpdatedValue = implementingClass.getMethod("getLastUpdated").invoke(feedData);
        Integer lastUpdated = lastUpdatedValue == null
          ? null
          : (lastUpdatedValue instanceof Date
              ? (int) ((Date) lastUpdatedValue).getTime()
              : (Integer) lastUpdatedValue);
        Integer ttl = (Integer) implementingClass.getMethod("getTtl").invoke(feedData);
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
    }

    private boolean shouldUpdate() {
      return getCurrentTimeSeconds() >= nextUpdate;
    }

    private int getCurrentTimeSeconds() {
      return (int) (System.currentTimeMillis() / 1000);
    }
  }
}
