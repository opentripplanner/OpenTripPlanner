package org.opentripplanner.routing.api.response;

import java.util.Objects;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class RoutingError {

  public final RoutingErrorCode code;
  public final InputField inputField;

  public RoutingError(RoutingErrorCode code, InputField inputField) {
    this.code = code;
    this.inputField = inputField;
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, inputField);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoutingError that = (RoutingError) o;
    return code == that.code && inputField == that.inputField;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RoutingError.class)
      .addEnum("code", code)
      .addEnum("inputField", inputField)
      .toString();
  }
}
