package org.opentripplanner.apis.support;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelope;

/**
 * Container for <a href="https://github.com/mapbox/tilejson-spec">TileJSON</a> response
 */
public class TileJson implements Serializable {

  // Some fields(all @SuppressWarnings("unused")) below are required to support the TileJSON format.

  @SuppressWarnings("unused")
  public final String tilejson = "2.2.0";

  @SuppressWarnings("unused")
  public final String scheme = "xyz";

  public final int minzoom;

  public final int maxzoom;

  public final String name = "OpenTripPlanner";
  public final String attribution;
  public final String[] tiles;
  public final double[] bounds;
  public final double[] center;

  public TileJson(
    String tileUrl,
    WorldEnvelope envelope,
    String attribution,
    int minZoom,
    int maxZoom
  ) {
    this.attribution = attribution;
    tiles = new String[] { tileUrl };

    bounds = new double[] {
      envelope.lowerLeft().longitude(),
      envelope.lowerLeft().latitude(),
      envelope.upperRight().longitude(),
      envelope.upperRight().latitude(),
    };

    var c = envelope.center();
    center = new double[] { c.longitude(), c.latitude(), 9 };

    minzoom = minZoom;
    maxzoom = maxZoom;
  }

  public TileJson(
    String tileUrl,
    WorldEnvelope envelope,
    Collection<FeedInfo> feedInfos,
    int minZoom,
    int maxZoom
  ) {
    this(tileUrl, envelope, attributionFromFeedInfo(feedInfos), minZoom, maxZoom);
  }

  /**
   * Creates a vector source layer URL from a hard-coded path plus information from the incoming
   * HTTP request.
   */
  public static String urlWithDefaultPath(
    UriInfo uri,
    HttpHeaders headers,
    List<String> layers,
    String ignoreRouterId,
    String path
  ) {
    return "%s/otp/routers/%s/%s/%s/{z}/{x}/{y}.pbf".formatted(
        HttpUtils.getBaseAddress(uri, headers),
        ignoreRouterId,
        path,
        String.join(",", layers)
      );
  }

  /**
   * Creates a vector source layer URL from a configured base path plus information from the incoming
   * HTTP request.
   */
  public static String urlFromOverriddenBasePath(
    UriInfo uri,
    HttpHeaders headers,
    String overridePath,
    List<String> layers
  ) {
    var strippedPath = StringUtils.stripStart(overridePath, "/");
    strippedPath = StringUtils.stripEnd(strippedPath, "/");
    return "%s/%s/%s/{z}/{x}/{y}.pbf".formatted(
        HttpUtils.getBaseAddress(uri, headers),
        strippedPath,
        String.join(",", layers)
      );
  }

  private static String attributionFromFeedInfo(Collection<FeedInfo> feedInfos) {
    return feedInfos
      .stream()
      .map(feedInfo ->
        "<a href='%s'>%s</a>".formatted(feedInfo.getPublisherUrl(), feedInfo.getPublisherName())
      )
      .distinct()
      .collect(Collectors.joining(", "));
  }
}
