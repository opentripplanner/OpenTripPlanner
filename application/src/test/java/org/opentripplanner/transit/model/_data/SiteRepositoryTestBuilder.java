package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;

import java.util.Optional;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationBuilder;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

/**
 * Test helper for buliding site repository entities with default values
 */
public class SiteRepositoryTestBuilder {

  private final SiteRepositoryBuilder siteRepositoryBuilder;

  private static final WgsCoordinate ANY_COORDINATE = new WgsCoordinate(60.0, 10.0);
  private static final Polygon ANY_POLYGON = GeometryUtils.getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        Coordinates.of(61.0, 10.0),
        Coordinates.of(61.0, 12.0),
        Coordinates.of(60.0, 11.0),
        Coordinates.of(61.0, 10.0),
      }
    );

  public SiteRepositoryTestBuilder(SiteRepositoryBuilder siteRepositoryBuilder) {
    this.siteRepositoryBuilder = siteRepositoryBuilder;
  }

  public SiteRepository build() {
    return siteRepositoryBuilder.build();
  }

  public RegularStop stop(String id) {
    return stop(id, b -> {});
  }

  public RegularStop stop(String id, Consumer<RegularStopBuilder> stopCustomizer) {
    var builder = siteRepositoryBuilder
      .regularStop(id(id))
      .withName(new NonLocalizedString(id))
      .withCode(id)
      .withCoordinate(ANY_COORDINATE);
    stopCustomizer.accept(builder);
    var stop = builder.build();
    siteRepositoryBuilder.withRegularStop(stop);
    return stop;
  }

  /**
   * Add a stop at a station. The station will be created if it does not already exist.
   */
  public RegularStop stopAtStation(String stopId, String stationId) {
    // Get or create station
    final var station = Optional.ofNullable(
      siteRepositoryBuilder.stationById().get(id(stationId))
    ).orElseGet(() -> station(stationId));

    return stop(stopId, b -> b.withParentStation(station));
  }

  public AreaStop areaStop(String id) {
    var stop = siteRepositoryBuilder
      .areaStop(id(id))
      .withName(new NonLocalizedString(id))
      .withGeometry(ANY_POLYGON)
      .build();
    siteRepositoryBuilder.withAreaStop(stop);
    return stop;
  }

  public Station station(String stationId) {
    return station(stationId, b -> {});
  }

  public Station station(String stationId, Consumer<StationBuilder> customizer) {
    var builder = Station.of(id(stationId))
      .withName(new NonLocalizedString(stationId))
      .withCode(stationId)
      .withCoordinate(ANY_COORDINATE)
      .withDescription(new NonLocalizedString("Station " + stationId))
      .withPriority(StopTransferPriority.ALLOWED);
    if (customizer != null) {
      customizer.accept(builder);
    }
    var station = builder.build();
    siteRepositoryBuilder.withStation(station);
    return station;
  }
}
