package org.opentripplanner.gbfs.v2;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFS;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeeds;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.gbfs.GbfsConstructionException;
import org.opentripplanner.gbfs.GbfsFeedDetails;
import org.opentripplanner.gbfs.GbfsFeedLoaderImpl;

/**
 * Class for managing the state and loading of complete GBFS version 2.2 and 2.3 datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoader
  extends GbfsFeedLoaderImpl<GBFSFeedName, GbfsFeedLoader.GBFSFeedV23Details> {

  /**
   * Fetches the auto-configuration file from the given url and sets up updaters for the feeds
   * listed in it.
   */
  public GbfsFeedLoader(
    String url,
    HttpHeaders httpHeaders,
    String languageCode,
    OtpHttpClient otpHttpClient
  ) {
    this(
      url,
      fetchAutoConfiguration(toUri(url), httpHeaders, otpHttpClient),
      httpHeaders,
      languageCode,
      otpHttpClient
    );
  }

  /**
   * Sets up updaters for the feeds listed in an already fetched auto-configuration file, avoiding
   * a second fetch of that file.
   */
  public GbfsFeedLoader(
    String url,
    JsonNode autoConfiguration,
    HttpHeaders httpHeaders,
    String languageCode,
    OtpHttpClient otpHttpClient
  ) {
    super(feedInfo(url, autoConfiguration, languageCode), httpHeaders, otpHttpClient);
  }

  private static List<GBFSFeedV23Details> feedInfo(
    String url,
    JsonNode autoConfiguration,
    String languageCode
  ) {
    GBFS data = mapAutoConfiguration(autoConfiguration, url, GBFS.class);

    // Pick first language if none defined
    GBFSFeeds feeds = languageCode == null
      ? data.getFeedsData().values().iterator().next()
      : data.getFeedsData().get(languageCode);
    if (feeds == null) {
      throw new GbfsConstructionException(
        "Language " + languageCode + " does not exist in feed " + url
      );
    }

    return feeds.getFeeds().stream().map(GBFSFeedV23Details::new).toList();
  }

  @Override
  protected <T> GBFSFeedName nameForClass(Class<T> feed) {
    return GBFSFeedName.fromClass(feed);
  }

  @Override
  protected <T> Class<T> classForName(GBFSFeedName name) {
    return (Class<T>) name.implementingClass();
  }

  /* private static classes */

  protected static class GBFSFeedV23Details implements GbfsFeedDetails<GBFSFeedName> {

    private final GBFSFeed feed;

    private GBFSFeedV23Details(GBFSFeed feed) {
      this.feed = feed;
    }

    @Override
    public GBFSFeedName getName() {
      return feed.getName();
    }

    @Override
    public URI getUrl() {
      return feed.getUrl();
    }
  }
}
