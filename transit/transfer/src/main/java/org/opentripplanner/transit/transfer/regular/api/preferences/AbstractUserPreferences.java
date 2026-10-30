package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The traveler preferences a regular transfer path is computed and cached under. The values are
 * normalized(rounded) by the individual preference classes so this class can be used as a cache
 * key.
 * <p>
 * Each {@link TransferProfileType} has its own preferences class, with the preferences that are
 * meaningful for the type. The only preference all types share is the reluctance, a car has no
 * speed or stairs of its own, see {@link WithSpeed} and {@link WithStairs}.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public abstract sealed class AbstractUserPreferences<S extends AbstractUserPreferences<S>>
  implements NormalizedDistance<S>, Serializable
  permits BicyclePreferences, CarPreferences, WalkPreferences, WheelchairPreferences
{

  private final Reluctance reluctance;

  AbstractUserPreferences(Reluctance reluctance) {
    this.reluctance = reluctance;
  }

  AbstractUserPreferences(Builder builder) {
    this.reluctance = builder.reluctance;
  }

  /** The profile type these preferences belong to. */
  public abstract TransferProfileType type();

  public Reluctance reluctance() {
    return reluctance;
  }

  abstract S defaultInstance();

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractUserPreferences that = (AbstractUserPreferences) o;
    return reluctance.equals(that.reluctance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reluctance);
  }

  ToStringBuilder appendToString(ToStringBuilder builder) {
    return builder.addObj("reluctance", reluctance, defaultInstance().reluctance());
  }

  @Override
  public float normalizedDistance(AbstractUserPreferences other) {
    if (getClass() != other.getClass()) {
      return 1000f;
    }
    return reluctance.normalizedDistance(other.reluctance);
  }

  protected abstract static class Builder<
    S extends AbstractUserPreferences,
    T extends Builder<S, T>
  > {

    private final S original;
    private Reluctance reluctance;

    Builder(S original) {
      this.original = original;
      this.reluctance = original.reluctance();
    }

    public S original() {
      return original;
    }

    @SuppressWarnings("unchecked")
    public T withReluctance(Reluctance reluctance) {
      this.reluctance = reluctance;
      return (T) this;
    }

    /**
     * Use the values of {@code parent} for the preferences both have, even if the parent is of
     * another type. A value that only exists for one of the types is not changed. The values of
     * another type are only a starting point, and should be overridden where they are not
     * meaningful for this type.
     */
    @SuppressWarnings("unchecked")
    public T inheritFrom(AbstractUserPreferences<?> parent) {
      this.reluctance = parent.reluctance();
      return (T) this;
    }

    abstract S doBuild();

    public S build() {
      S value = doBuild();
      return original.equals(value) ? (S) original : value;
    }
  }
}
