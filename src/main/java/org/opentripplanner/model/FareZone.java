package org.opentripplanner.model;

import java.util.Objects;

public class FareZone extends TransitEntity<FeedScopedId> {

  private final FeedScopedId id;

  private final String name;

  /**
   * Equal to GTFS zone_id or NeTEx TariffZone.
   */
  // TODO This should at some point be connected to Agency or Operator. Currently is is up to the
  //      user to make this connection (based on TariffZone id).
  public FareZone(FeedScopedId id, String name) {
    this.id = id;
    this.name = name;
  }

  public FeedScopedId getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    if (!super.equals(o)) { return false; }
    FareZone fareZone = (FareZone) o;
    return Objects.equals(id, fareZone.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), id);
  }
}
