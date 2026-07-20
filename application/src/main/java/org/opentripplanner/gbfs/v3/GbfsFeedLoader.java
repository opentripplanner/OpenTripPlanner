package org.opentripplanner.gbfs.v3;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.List;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSGbfs;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.gbfs.GbfsFeedDetails;
import org.opentripplanner.gbfs.GbfsFeedLoaderImpl;

/**
 * Class for managing the state and loading of complete GBFS version 3.0 datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoader
  extends GbfsFeedLoaderImpl<GBFSFeed.Name, GbfsFeedLoader.GBFSFeedV30Details> {

  /**
   * Fetches the auto-configuration file from the given url and sets up updaters for the feeds
   * listed in it.
   */
  public GbfsFeedLoader(String url, HttpHeaders httpHeaders, OtpHttpClient otpHttpClient) {
    this(
      url,
      fetchAutoConfiguration(toUri(url), httpHeaders, otpHttpClient),
      httpHeaders,
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
    OtpHttpClient otpHttpClient
  ) {
    super(feedInfo(url, autoConfiguration), httpHeaders, otpHttpClient);
  }

  private static List<GBFSFeedV30Details> feedInfo(String url, JsonNode autoConfiguration) {
    GBFSGbfs data = mapAutoConfiguration(autoConfiguration, url, GBFSGbfs.class);
    return data.getData().getFeeds().stream().map(GBFSFeedV30Details::new).toList();
  }

  @Override
  protected <T> GBFSFeed.Name nameForClass(Class<T> feed) {
    return GBFSFeedName.fromClass(feed);
  }

  @Override
  protected <T> Class<T> classForName(GBFSFeed.Name name) {
    return (Class<T>) GBFSFeedName.implementingClass(name);
  }

  /* private static classes */

  protected static class GBFSFeedV30Details implements GbfsFeedDetails<GBFSFeed.Name> {

    private final GBFSFeed feed;

    private GBFSFeedV30Details(GBFSFeed feed) {
      this.feed = feed;
    }

    @Override
    public GBFSFeed.Name getName() {
      return feed.getName();
    }

    @Override
    public URI getUrl() {
      return URI.create(feed.getUrl());
    }
  }
}
