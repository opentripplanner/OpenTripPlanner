package org.opentripplanner.api.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.opentripplanner.framework.geometry.WorldEnvelope;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.model.FeedInfo;

/**
 * Container for <a href="https://github.com/mapbox/tilejson-spec">TileJSON</a> response
 */
public class TileJson implements Serializable {

  // Some fields(all @SuppressWarnings("unused")) below are required to support the TileJSON format.

  @SuppressWarnings("unused")
  public final String tilejson = "2.2.0";

  @SuppressWarnings("unused")
  public final String scheme = "xyz";

  @SuppressWarnings("unused")
  public final int minzoom = 9;

  @SuppressWarnings("unused")
  public final int maxzoom = 20;

  public final String name = "OpenTripPlanner";
  public final String attribution;
  public final String[] tiles;
  public final double[] bounds;
  public final double[] center;

  public TileJson(
    UriInfo uri,
    HttpHeaders headers,
    String layers,
    String ignoreRouterId,
    String path,
    WorldEnvelope envelope,
    Collection<FeedInfo> feedInfos
  ) {
    attribution =
      feedInfos
        .stream()
        .map(feedInfo ->
          "<a href='" + feedInfo.getPublisherUrl() + "'>" + feedInfo.getPublisherName() + "</a>"
        )
        .collect(Collectors.joining(", "));

    tiles =
      new String[] {
        "%s/otp/routers/%s/%s/%s/{z}/{x}/{y}.pbf".formatted(
            HttpUtils.getBaseAddress(uri, headers),
            ignoreRouterId,
            path,
            layers
          ),
      };

    bounds =
      new double[] {
        envelope.lowerLeft().longitude(),
        envelope.lowerLeft().latitude(),
        envelope.upperRight().longitude(),
        envelope.upperRight().latitude(),
      };

    // TODO: Should we replace this with a fallback to the mean center
    //       if median transit center do not exist?
    if (envelope.transitMedianCenter().isPresent()) {
      var c = envelope.transitMedianCenter().get();
      center = new double[] { c.longitude(), c.latitude(), 9 };
    } else {
      center = null;
    }
  }
}
