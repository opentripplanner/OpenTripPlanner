package org.opentripplanner.transit.transfer.regular.api.preferences;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The traveler preferences a regular transfer path is computed and cached under. The values are
 * normalized(rounded) by the individual preference classes so this class can be used as a cache
 * key.
 * <p>
 * Each {@link TransferProfileType} has its own preferences class, with the preferences that are
 * meaningful for the type. The preferences all types share are here: the speed and the reluctance.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public abstract sealed class AbstractUserPreferences<S extends AbstractUserPreferences<S>>
  implements NormalizedDistance<S>, Serializable
  permits BicyclePreferences, WalkPreferences, WheelchairPreferences
{

  private final Speed speed;
  private final Reluctance reluctance;

  AbstractUserPreferences(Speed speed, Reluctance reluctance) {
    this.speed = speed;
    this.reluctance = reluctance;
  }

  AbstractUserPreferences(Builder builder) {
    this.speed = builder.speed;
    this.reluctance = builder.reluctance;
  }

  /** The profile type these preferences belong to. */
  public abstract TransferProfileType type();

  public Speed speed() {
    return speed;
  }

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
    return speed.equals(that.speed) && reluctance.equals(that.reluctance);
  }

  @Override
  public int hashCode() {
    return Objects.hash(speed, reluctance);
  }

  ToStringBuilder appendToString(ToStringBuilder builder) {
    return builder
      .addObj("speed", speed, defaultInstance().speed())
      .addObj("reluctance", reluctance, defaultInstance().reluctance());
  }

  @Override
  public float normalizedDistance(AbstractUserPreferences other) {
    if (getClass() != other.getClass()) {
      return 1000f;
    }
    return speed.normalizedDistance(other.speed) + reluctance.normalizedDistance(other.reluctance);
  }

  protected abstract static class Builder<
    S extends AbstractUserPreferences,
    T extends Builder<S, T>
  > {

    private final S original;
    private Speed speed;
    private Reluctance reluctance;

    Builder(S original) {
      this.original = original;
      this.speed = original.speed();
      this.reluctance = original.reluctance();
    }

    public S original() {
      return original;
    }

    @SuppressWarnings("unchecked")
    public T withSpeed(Speed speed) {
      this.speed = speed;
      return (T) this;
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
      this.speed = parent.speed();
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
