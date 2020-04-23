/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * A place where actual boarding/departing happens. It can be a bus stop on one side of a road or a
 * platform at a train station. Equivalent to GTFS stop location 0 or NeTEx quay.
 */
public final class Stop extends StationElement {

  private static final long serialVersionUID = 2L;

  /**
   * Used for GTFS fare information.
   */
  private final String zone;

  /**
   * URL to a web page containing information about this particular stop.
   */
  private final String url;

  private HashSet<BoardingArea> boardingAreas;

  public Stop(
      FeedScopedId id,
      String name,
      String code,
      String description,
      WgsCoordinate coordinate,
      WheelChairBoarding wheelchairBoarding,
      StopLevel level,
      String zone,
      String url
  ) {
    super(id, name, code, description, coordinate, wheelchairBoarding, level);
    this.zone = zone;
    this.url = url;
  }

  /**
   * Create a minimal Stop object for unit-test use, where the test only care about id, name and
   * coordinate. The feedId is static set to "TEST"
   */
  public static Stop stopForTest(String idAndName, double lat, double lon) {
    return new Stop(
        new FeedScopedId("TEST", idAndName),
        idAndName,
        idAndName,
        null,
        new WgsCoordinate(lat, lon),
        null,
        null,
        null,
        null
    );
  }


  public void addBoardingArea(BoardingArea boardingArea) {
    if (boardingAreas == null) {
      boardingAreas = new HashSet<>();
    }
    boardingAreas.add(boardingArea);
  }

  @Override
  public String toString() {
    return "<Stop " + this.id + ">";
  }

  public String getZone() {
    return zone;
  }

  public String getUrl() {
    return url;
  }

  public Collection<BoardingArea> getBoardingAreas() {
    return boardingAreas != null ? boardingAreas : Collections.emptySet();
  }

  /**
   * Get the transfer cost priority for Stop. This will fetch the value from the parent
   * [if parent exist] or return the default value.
   */
  @NotNull
  public TransferPriority getCostPriority() {
    return isPartOfStation() ? getParentStation().getCostPriority() : TransferPriority.ALLOWED;
  }
}
