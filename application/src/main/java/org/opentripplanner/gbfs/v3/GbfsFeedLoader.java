package org.opentripplanner.gbfs.v3;

import java.net.URI;
import java.util.List;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v3_0.gbfs.GBFSGbfs;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.gbfs.GbfsAutoConfiguration;
import org.opentripplanner.gbfs.GbfsFeedDetails;
import org.opentripplanner.gbfs.GbfsFeedLoaderImpl;

/**
 * Class for managing the state and loading of complete GBFS version 3.0 datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoader
  extends GbfsFeedLoaderImpl<GBFSFeed.Name, GbfsFeedLoader.GBFSFeedV30Details> {

  /**
   * Sets up updaters for the feeds listed in the auto-configuration file.
   */
  public static GbfsFeedLoader create(
    GbfsAutoConfiguration autoConfiguration,
    HttpHeaders httpHeaders,
    OtpHttpClient otpHttpClient
  ) {
    var feeds = autoConfiguration
      .mapTo(GBFSGbfs.class)
      .getData()
      .getFeeds()
      .stream()
      .map(GBFSFeedV30Details::new)
      .toList();
    return new GbfsFeedLoader(feeds, httpHeaders, otpHttpClient);
  }

  private GbfsFeedLoader(
    List<GBFSFeedV30Details> feeds,
    HttpHeaders httpHeaders,
    OtpHttpClient otpHttpClient
  ) {
    super(feeds, httpHeaders, otpHttpClient);
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
