package org.opentripplanner.api.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.util.WorldEnvelope;

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
    WorldEnvelope graphEnvelope,
    Collection<FeedInfo> feedInfos,
    @Nullable Coordinate transitServiceCenter
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
        graphEnvelope.getLowerLeftLongitude(),
        graphEnvelope.getLowerLeftLatitude(),
        graphEnvelope.getUpperRightLongitude(),
        graphEnvelope.getUpperRightLatitude(),
      };

    if (transitServiceCenter == null) {
      center = null;
    } else {
      center = new double[] { transitServiceCenter.x, transitServiceCenter.y, 9 };
    }
  }
}
