package org.opentripplanner.api.model;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;
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

    var c = envelope.center();
    center = new double[] { c.longitude(), c.latitude(), 9 };
  }
}
