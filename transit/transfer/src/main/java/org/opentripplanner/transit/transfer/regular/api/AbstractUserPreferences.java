package org.opentripplanner.transit.transfer.regular.api;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.core.model.basic.NormalizedDistance;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The traveler preferences a regular transfer path is computed and cached under - the concrete
 * {@code U} in {@link org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider}. The
 * values are normalized(rounded) by the individual preference classes so this class can be used
 * as a cache key.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public abstract sealed class AbstractUserPreferences<S extends AbstractUserPreferences<S>>
  implements NormalizedDistance<S>, Serializable
  permits WalkPreferences
{

  private final Speed speed;
  private final Reluctance reluctance;
  private final StreetSegmentTypeUsage stairs;

  AbstractUserPreferences(Speed speed, Reluctance reluctance, StreetSegmentTypeUsage stairs) {
    this.speed = speed;
    this.reluctance = reluctance;
    this.stairs = stairs;
  }

  AbstractUserPreferences(Builder builder) {
    this.speed = builder.speed;
    this.reluctance = builder.reluctance;
    this.stairs = builder.stairs;
  }

  public Speed speed() {
    return speed;
  }

  public Reluctance reluctance() {
    return reluctance;
  }

  public StreetSegmentTypeUsage stairs() {
    return stairs;
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
    return speed.equals(that.speed) && reluctance.equals(that.reluctance) && stairs == that.stairs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(speed, reluctance, stairs);
  }

  ToStringBuilder appendToString(ToStringBuilder builder) {
    return builder
      .addObj("speed", speed, defaultInstance().speed())
      .addObj("reluctance", reluctance, defaultInstance().reluctance())
      .addObj("stairs", stairs, defaultInstance().stairs());
  }

  @Override
  public float normalizedDistance(AbstractUserPreferences other) {
    if (getClass() != other.getClass()) {
      return 1000f;
    }
    return (
      speed.normalizedDistance(other.speed) +
      reluctance.normalizedDistance(other.reluctance) +
      stairs.normalizedDistance(other.stairs)
    );
  }

  protected abstract static class Builder<
    S extends AbstractUserPreferences,
    T extends Builder<S, T>
  > {

    private final S original;
    private Speed speed;
    private Reluctance reluctance;
    private StreetSegmentTypeUsage stairs;

    Builder(S original) {
      this.original = original;
      this.speed = original.speed();
      this.reluctance = original.reluctance();
      this.stairs = original.stairs();
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

    @SuppressWarnings("unchecked")
    public T withStairs(StreetSegmentTypeUsage stairs) {
      this.stairs = stairs;
      return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T apply(Consumer<T> body) {
      body.accept((T) this);
      return (T) this;
    }

    abstract S doBuild();

    public S build() {
      S value = doBuild();
      return original.equals(value) ? (S) original : value;
    }
  }
}
