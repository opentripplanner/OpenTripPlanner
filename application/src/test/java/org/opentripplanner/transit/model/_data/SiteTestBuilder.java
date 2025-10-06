package org.opentripplanner.transit.model._data;

import static org.opentripplanner.transit.model._data.TransitTestEnvironment.id;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

/**
 * Test helper for buliding a site repository
 */
public class SiteTestBuilder {

  private final SiteRepositoryBuilder siteRepositoryBuilder = SiteRepository.of();

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

  public static SiteTestBuilder of() {
    return new SiteTestBuilder();
  }

  public SiteRepository build() {
    return siteRepositoryBuilder.build();
  }

  public SiteTestBuilder withStops(String... stopIds) {
    Arrays.stream(stopIds).forEach(this::stop);
    return this;
  }

  public SiteTestBuilder withStop(String stopId) {
    stop(stopId);
    return this;
  }

  public SiteTestBuilder withStopAtStation(String stopId, String stationId) {
    stopAtStation(stopId, stationId);
    return this;
  }

  public SiteTestBuilder withAreaStops(String... stopIds) {
    Arrays.stream(stopIds).forEach(this::areaStop);
    return this;
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
    var station = Station.of(id(stationId))
      .withName(new NonLocalizedString(stationId))
      .withCode(stationId)
      .withCoordinate(ANY_COORDINATE)
      .withDescription(new NonLocalizedString("Station " + stationId))
      .withPriority(StopTransferPriority.ALLOWED)
      .build();
    siteRepositoryBuilder.withStation(station);
    return station;
  }
}
