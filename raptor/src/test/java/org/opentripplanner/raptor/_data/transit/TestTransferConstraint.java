package org.opentripplanner.raptor._data.transit;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;

/**
 * This class holds transfer constraint information.
 * <p>
 * The class is immutable.
 */
public class TestTransferConstraint implements Serializable, RaptorTransferConstraint {

  private enum Type {
    NOT_ALLOWED,
    STAY_SEATED,
    GUARANTEED;

    boolean is(Type type) {
      return this == type;
    }
  }

  private final Type type;

  public TestTransferConstraint(Type type) {
    this.type = type;
  }

  public static TestTransferConstraint notAllowed() {
    return new TestTransferConstraint(Type.NOT_ALLOWED);
  }

  public static TestTransferConstraint staySeated() {
    return new TestTransferConstraint(Type.STAY_SEATED);
  }

  public static TestTransferConstraint guaranteed() {
    return new TestTransferConstraint(Type.GUARANTEED);
  }

  public static RaptorTransferConstraint regular() {
    return RaptorTransferConstraint.REGULAR_TRANSFER;
  }

  public boolean isGuaranteed() {
    return type.is(Type.GUARANTEED);
  }

  @Override
  public boolean isNotAllowed() {
    return type.is(Type.NOT_ALLOWED);
  }

  @Override
  public boolean isRegularTransfer() {
    return false;
  }

  @Override
  public boolean isStaySeated() {
    return type.is(Type.STAY_SEATED);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final TestTransferConstraint that)) {
      return false;
    }
    return type == that.type;
  }

  public String toString() {
    return "{" + type + "}";
  }
}
