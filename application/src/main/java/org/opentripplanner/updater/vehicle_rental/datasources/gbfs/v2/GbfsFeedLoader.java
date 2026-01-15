package org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFS;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeed;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeedName;
import org.mobilitydata.gbfs.v2_3.gbfs.GBFSFeeds;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
import org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsFeedDetails;
import org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsFeedLoaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing the state and loading of complete GBFS version 2.2 and 2.3 datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoader
  extends GbfsFeedLoaderImpl<GBFSFeedName, GbfsFeedLoader.GBFSFeedV23Details> {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFeedLoader.class);

  public GbfsFeedLoader(
    String url,
    HttpHeaders httpHeaders,
    String languageCode,
    OtpHttpClient otpHttpClient
  ) {
    super(fetchFeedInfo(url, languageCode, httpHeaders, otpHttpClient), httpHeaders, otpHttpClient);
  }

  private static List<GBFSFeedV23Details> fetchFeedInfo(
    String url,
    String languageCode,
    HttpHeaders httpHeaders,
    OtpHttpClient otpHttpClient
  ) {
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new UpdaterConstructionException("Invalid url " + url);
    }

    // Fetch autoconfiguration file
    GBFS data = fetchFeed(uri, httpHeaders, otpHttpClient, GBFS.class);
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
