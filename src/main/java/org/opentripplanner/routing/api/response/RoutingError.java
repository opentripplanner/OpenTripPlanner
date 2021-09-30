package org.opentripplanner.routing.api.response;

import java.util.Objects;

public class RoutingError {
  public final RoutingErrorCode code;
  public final InputField inputField;

  public RoutingError(RoutingErrorCode code, InputField inputField) {
    this.code = code;
    this.inputField = inputField;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RoutingError that = (RoutingError) o;
    return code == that.code && inputField == that.inputField;
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, inputField);
  }
}
