package org.opentripplanner.street.search.request;

import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Preferences for how to treat trips or stops with accessibility restrictions, like wheelchair
 * accessibility.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class AccessibilityRequest {

  /**
   * Set the unknown cost to a very high number, so in case it is used accidentally, it
   * will not cause any harm.
   */
  private static final Cost NOT_SET = Cost.costOfSeconds(9_999_999);

  private static final AccessibilityRequest DEFAULT_UNSET = new AccessibilityRequest(
    false,
    NOT_SET,
    NOT_SET
  );
  private static final AccessibilityRequest ONLY_CONSIDER_ACCESSIBLE = new AccessibilityRequest(
    true,
    NOT_SET,
    NOT_SET
  );

  private final boolean onlyConsiderAccessible;
  private final Cost unknownCost;
  private final Cost inaccessibleCost;

  private AccessibilityRequest(
    boolean onlyConsiderAccessible,
    Cost unknownCost,
    Cost inaccessibleCost
  ) {
    this.onlyConsiderAccessible = onlyConsiderAccessible;
    this.unknownCost = unknownCost;
    this.inaccessibleCost = inaccessibleCost;
  }

  /**
   * Create a feature which only considers wheelchair-accessible trips/stops.
   */
  public static AccessibilityRequest ofOnlyAccessible() {
    return ONLY_CONSIDER_ACCESSIBLE;
  }

  /**
   * Create a feature which considers trips/stops that don't have an accessibility of POSSIBLE.
   */
  public static AccessibilityRequest ofCost(int unknownCost, int inaccessibleCost) {
    return new AccessibilityRequest(
      false,
      Cost.costOfSeconds(unknownCost),
      Cost.costOfSeconds(inaccessibleCost)
    );
  }

  public static Builder of() {
    return DEFAULT_UNSET.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this, this);
  }

  public Builder copyOfWithDefaultCosts(AccessibilityRequest defaultCosts) {
    return new Builder(this, defaultCosts);
  }

  /**
   * Whether to include trips or stops which don't have a wheelchair accessibility of POSSIBLE
   */
  public boolean onlyConsiderAccessible() {
    return onlyConsiderAccessible;
  }

  /**
   * The extra cost to add when using a trip/stop which has an accessibility of UNKNOWN.
   */
  public int unknownCost() {
    return unknownCost.toSeconds();
  }

  /**
   * The extra cost to add when using a trip/stop which has an accessibility of NOT_POSSIBLE.
   */
  public int inaccessibleCost() {
    return inaccessibleCost.toSeconds();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessibilityRequest that = (AccessibilityRequest) o;
    return (
      onlyConsiderAccessible == that.onlyConsiderAccessible &&
      Objects.equals(unknownCost, that.unknownCost) &&
      Objects.equals(inaccessibleCost, that.inaccessibleCost)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(onlyConsiderAccessible, unknownCost, inaccessibleCost);
  }

  @Override
  public String toString() {
    return toString(DEFAULT_UNSET);
  }

  public String toString(AccessibilityRequest defaultCosts) {
    if (onlyConsiderAccessible) {
      return "OnlyConsiderAccessible";
    }

    return ToStringBuilder.of(AccessibilityRequest.class)
      .addObj("unknownCost", unknownCost, defaultCosts.unknownCost)
      .addObj("inaccessibleCost", inaccessibleCost, defaultCosts.inaccessibleCost)
      .toString();
  }

  public static class Builder {

    private final AccessibilityRequest original;
    private boolean onlyConsiderAccessible;
    private Cost unknownCost;
    private Cost inaccessibleCost;

    private Builder(AccessibilityRequest original, AccessibilityRequest defaultCosts) {
      this.original = original;

      if (original.onlyConsiderAccessible) {
        this.onlyConsiderAccessible = true;
        this.unknownCost = defaultCosts.unknownCost;
        this.inaccessibleCost = defaultCosts.inaccessibleCost;
      } else {
        this.onlyConsiderAccessible = false;
        this.unknownCost = original.unknownCost;
        this.inaccessibleCost = original.inaccessibleCost;
      }
    }

    public boolean onlyConsiderAccessible() {
      return onlyConsiderAccessible;
    }

    public Builder withAccessibleOnly() {
      this.onlyConsiderAccessible = true;
      return this;
    }

    public int unknownCost() {
      return unknownCost.toSeconds();
    }

    public Builder withUnknownCost(int unknownCost) {
      this.onlyConsiderAccessible = false;
      this.unknownCost = Cost.costOfSeconds(unknownCost);
      return this;
    }

    public int inaccessibleCost() {
      return inaccessibleCost.toSeconds();
    }

    public Builder withInaccessibleCost(int inaccessibleCost) {
      this.onlyConsiderAccessible = false;
      this.inaccessibleCost = Cost.costOfSeconds(inaccessibleCost);
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public AccessibilityRequest build() {
      var value = onlyConsiderAccessible
        ? ofOnlyAccessible()
        : ofCost(unknownCost.toSeconds(), inaccessibleCost.toSeconds());
      return original.equals(value) ? original : value;
    }
  }
}
