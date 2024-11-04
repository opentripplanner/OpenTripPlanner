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

  /**
   * STAY_SEATED is not a priority, but we assign a cost to it to be able to compare it with other
   * transfers with a priority and the {@link #GUARANTEED_TRANSFER_COST}.
   */
  private static final int STAY_SEATED_TRANSFER_COST = 10_00;

  /**
   * GUARANTEED is not a priority, but we assign a cost to it to be able to compare it with other
   * transfers with a priority. The cost is better than a pure prioritized transfer, but the
   * priority and GUARANTEED attribute is added together; Hence a (GUARANTEED, RECOMMENDED) transfer
   * is better than (GUARANTEED, ALLOWED).
   */
  private static final int GUARANTEED_TRANSFER_COST = 20_00;

  /**
   * A cost penalty of 10 points is added to a Transfer which is NOT stay-seated or guaranteed. This
   * makes sure that stay-seated and guaranteed transfers take precedence over the priority cost.
   */
  private static final int NONE_FACILITATED_COST = 30_00;

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
