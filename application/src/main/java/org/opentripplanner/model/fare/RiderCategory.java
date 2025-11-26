package org.opentripplanner.model.fare;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Fare category like "Adult", "Student" or "Senior citizen".
 */
@Sandbox
public final class RiderCategory implements Serializable {

  private final FeedScopedId id;
  private final String name;
  private final boolean isDefault;

  RiderCategory(RiderCategoryBuilder builder) {
    this.id = Objects.requireNonNull(builder.id);
    this.name = Objects.requireNonNull(builder.name);
    this.isDefault = builder.isDefault;
  }

  public static RiderCategoryBuilder of(FeedScopedId id) {
    return new RiderCategoryBuilder(id);
  }

  public FeedScopedId id() {
    return id;
  }

  public String name() {
    return name;
  }

  /**
   * Returns true if this is the default category, for example "Adult" or "Regular". What is considered
   * the default varies from location to location.
   * <p>
   * The source of this (optional) information is the GTFS feeds (no inference).
   */
  public boolean isDefault() {
    return isDefault;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (RiderCategory) obj;
    return Objects.equals(this.id, that.id) && Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addObj("id", id)
      .addStr("name", name)
      .addBoolIfTrue("isDefault", isDefault)
      .toString();
  }
}
