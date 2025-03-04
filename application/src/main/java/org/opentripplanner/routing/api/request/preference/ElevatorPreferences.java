package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.Units;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 *  TODO: how long does it /really/ take to  an elevator?
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ElevatorPreferences implements Serializable {

  public static final ElevatorPreferences DEFAULT = new ElevatorPreferences();

  private final Cost boardCost;
  private final int boardTime;
  private final Cost hopCost;
  private final int hopTime;

  private ElevatorPreferences() {
    this.boardCost = Cost.costOfSeconds(90);
    this.boardTime = 90;
    this.hopCost = Cost.costOfSeconds(20);
    this.hopTime = 20;
  }

  private ElevatorPreferences(Builder builder) {
    this.boardCost = builder.boardCost;
    this.boardTime = Units.duration(builder.boardTime);
    this.hopCost = builder.hopCost;
    this.hopTime = Units.duration(builder.hopTime);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /** What is the cost of boarding an elevator? */
  public int boardCost() {
    return boardCost.toSeconds();
  }

  /**
   * How long does it take to board an elevator, on average (actually, it probably should be a bit *more*
   * than average, to prevent optimistic trips)? Setting it to "seems like forever," while accurate,
   * will probably prevent OTP from working correctly.
   */
  public int boardTime() {
    return boardTime;
  }

  /** How long does it take to advance one floor on an elevator? */
  public int hopCost() {
    return hopCost.toSeconds();
  }

  /**
   * What is the cost of travelling one floor on an elevator?
   * It is assumed that getting off an elevator is completely free.
   */
  public int hopTime() {
    return hopTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ElevatorPreferences that = (ElevatorPreferences) o;
    return (
      boardCost.equals(that.boardCost) &&
      boardTime == that.boardTime &&
      hopTime == that.hopTime &&
      hopCost.equals(that.hopCost)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardCost, boardTime, hopTime, hopCost);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ElevatorPreferences.class)
      .addObj("boardCost", boardCost, DEFAULT.boardCost)
      .addDurationSec("boardTime", boardTime, DEFAULT.boardTime)
      .addObj("hopCost", hopCost, DEFAULT.hopCost)
      .addDurationSec("hopTime", hopTime, DEFAULT.hopTime)
      .toString();
  }

  public static class Builder {

    private final ElevatorPreferences original;
    private Cost boardCost;
    private int boardTime;
    private int hopTime;
    private Cost hopCost;

    public Builder(ElevatorPreferences original) {
      this.original = original;
      this.boardCost = original.boardCost;
      this.boardTime = original.boardTime;
      this.hopCost = original.hopCost;
      this.hopTime = original.hopTime;
    }

    public ElevatorPreferences original() {
      return original;
    }

    public Builder withBoardCost(int boardCost) {
      this.boardCost = Cost.costOfSeconds(boardCost);
      return this;
    }

    public Builder withBoardTime(int boardTime) {
      this.boardTime = boardTime;
      return this;
    }

    public Builder withHopTime(int hopTime) {
      this.hopTime = hopTime;
      return this;
    }

    public Builder withHopCost(int hopCost) {
      this.hopCost = Cost.costOfSeconds(hopCost);
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    ElevatorPreferences build() {
      var value = new ElevatorPreferences(this);
      return original.equals(value) ? original : value;
    }
  }
}
