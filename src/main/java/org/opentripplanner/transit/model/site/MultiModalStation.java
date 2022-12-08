package org.opentripplanner.transit.model.site;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * The next level grouping of stops above Station. Equivalent to NeTEx multimodal StopPlace. As a
 * Station (NeTEx StopPlace) only supports a single transit mode, you are required to group several
 * Stations together using a MultiModalStation in order to support several modes. This entity is not
 * part of GTFS.
 */
public class MultiModalStation
  extends AbstractTransitEntity<MultiModalStation, MultiModalStationBuilder>
  implements StopLocationsGroup {

  private final Collection<Station> childStations;

  private final I18NString name;

  private final WgsCoordinate coordinate;

  private final String code;

  private final String description;

  private final I18NString url;

  /**
   * Create a new multi modal station with the given list of child stations.
   */
  MultiModalStation(MultiModalStationBuilder builder) {
    super(builder.getId());
    // Required fields
    this.childStations = Objects.requireNonNull(builder.childStations());
    this.name = I18NString.assertHasValue(builder.name());

    // Optional fields
    // TODO Make required
    this.coordinate = builder.coordinate();
    this.code = builder.code();
    this.description = builder.description();
    this.url = builder.url();
  }

  public static MultiModalStationBuilder of(FeedScopedId id) {
    return new MultiModalStationBuilder(id);
  }

  public I18NString getName() {
    return name;
  }

  public Collection<Station> getChildStations() {
    return this.childStations;
  }

  public Collection<StopLocation> getChildStops() {
    return this.childStations.stream().flatMap(s -> s.getChildStops().stream()).toList();
  }

  @Override
  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  /**
   * Public facing station code (short text or number)
   */
  public String getCode() {
    return code;
  }

  /**
   * Additional information about the station (if needed)
   */
  public String getDescription() {
    return description;
  }

  /**
   * URL to a web page containing information about this particular station
   */
  public I18NString getUrl() {
    return url;
  }

  @Override
  public boolean sameAs(@Nonnull MultiModalStation other) {
    return (
      getId().equals(other.getId()) &&
      Objects.equals(childStations, other.getChildStations()) &&
      Objects.equals(name, other.getName()) &&
      Objects.equals(coordinate, other.getCoordinate()) &&
      Objects.equals(code, other.getCode()) &&
      Objects.equals(description, other.getDescription()) &&
      Objects.equals(url, other.getUrl())
    );
  }

  @Nonnull
  @Override
  public MultiModalStationBuilder copy() {
    return new MultiModalStationBuilder(this);
  }
}
