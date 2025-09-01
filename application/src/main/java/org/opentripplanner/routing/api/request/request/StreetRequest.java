package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class StreetRequest implements Serializable {

  public static final StreetRequest DEFAULT = new StreetRequest(StreetMode.WALK);

  private final StreetMode mode;

  /***
   * The rentalDuration only apply to direct search, support for transit search is not implemented.
   */
  @Nullable
  private final Duration rentalDuration;

  public StreetRequest(StreetMode mode, Duration rentalDuration) {
    this.mode = mode;
    this.rentalDuration = rentalDuration;
  }

  public StreetRequest(StreetMode mode) {
    this.mode = mode;
    this.rentalDuration = null;
  }

  public StreetMode mode() {
    return mode;
  }

  public Duration rentalDuration() {
    return rentalDuration;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StreetRequest that = (StreetRequest) o;
    return (mode == that.mode && Objects.equals(rentalDuration, that.rentalDuration));
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode, rentalDuration);
  }

  @Override
  public String toString() {
    return ToStringBuilder.ofEmbeddedType()
      .addEnum("mode", mode, DEFAULT.mode)
      .addObj("rentalDuration", rentalDuration, DEFAULT.rentalDuration)
      .toString();
  }
}
