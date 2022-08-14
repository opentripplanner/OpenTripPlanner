package org.opentripplanner.transit.model.site;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Equal to GTFS zone_id or NeTEx TariffZone.
 *
 * TODO This should at some point be connected to Agency or Operator. Currently is is up to the
 *      user to make this connection (based on TariffZone id).
 */
public class FareZone extends AbstractTransitEntity<FareZone, FareZoneBuilder> {

  private final String name;

  FareZone(FareZoneBuilder builder) {
    super(builder.getId());
    // Optional fields
    this.name = builder.name();
  }

  public static FareZoneBuilder of(FeedScopedId id) {
    return new FareZoneBuilder(id);
  }

  @Nullable
  public String getName() {
    return name;
  }

  @Override
  @Nonnull
  public FareZoneBuilder copy() {
    return new FareZoneBuilder(this);
  }

  @Override
  public boolean sameAs(@Nonnull FareZone other) {
    return getId().equals(other.getId()) && Objects.equals(name, other.name);
  }
}
