package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class StreetRequest implements Serializable {

  public static final StreetRequest DEFAULT = new StreetRequest(StreetMode.WALK);

  private final StreetMode mode;

  public StreetRequest(StreetMode mode) {
    this.mode = mode;
  }

  public StreetMode mode() {
    return mode;
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
    return mode == that.mode;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mode);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(StreetRequest.class).addEnum("mode", mode, DEFAULT.mode).toString();
  }
}
