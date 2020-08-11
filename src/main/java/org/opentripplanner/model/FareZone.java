package org.opentripplanner.model;

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
}
