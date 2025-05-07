package org.opentripplanner.routing.api.request.request;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.utils.tostring.ToStringBuilder;

// TODO VIA: Javadoc
public class StreetRequest implements Cloneable, Serializable {

  private static final StreetRequest DEFAULT = new StreetRequest();

  private StreetMode mode;

  public StreetRequest() {
    this(StreetMode.WALK);
  }

  public StreetRequest(StreetMode mode) {
    this.mode = mode;
  }

  public void setMode(StreetMode mode) {
    this.mode = mode;
  }

  public StreetMode mode() {
    return mode;
  }

  public StreetRequest clone() {
    try {
      return (StreetRequest) super.clone();
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(Object o) {
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
