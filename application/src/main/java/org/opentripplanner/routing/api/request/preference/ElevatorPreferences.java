package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.utils.lang.Units;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 *  TODO: how long does it /really/ take to  an elevator?
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class ElevatorPreferences implements Serializable {

  public static final ElevatorPreferences DEFAULT = new ElevatorPreferences();

  private final Cost boardCost;
  private final Duration boardSlack;
  private final Duration hopTime;
  private final double reluctance;

  private ElevatorPreferences() {
    this.boardCost = Cost.costOfSeconds(15);
    this.boardSlack = Duration.ofSeconds(90);
    this.hopTime = Duration.ofSeconds(20);
    this.reluctance = 2.0;
  }

  private ElevatorPreferences(Builder builder) {
    this.boardCost = builder.boardCost;
    this.boardSlack = builder.boardSlack;
    this.hopTime = builder.hopTime;
    this.reluctance = Units.reluctance(builder.reluctance);
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * What is the cost of boarding an elevator?
   */
  public int boardCost() {
    return boardCost.toSeconds();
  }

  /**
   * How long does it take to board an elevator, on average (actually, it probably should be a bit *more*
   * than average, to prevent optimistic trips)? Setting it to "seems like forever," while accurate,
   * will probably prevent OTP from working correctly.
   */
  public Duration boardSlack() {
    return boardSlack;
  }

  /**
   * How long does it take to travel one floor on an elevator?
   */
  public Duration hopTime() {
    return hopTime;
  }

  public double reluctance() {
    return reluctance;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElevatorPreferences that = (ElevatorPreferences) o;
    return (
      Objects.equals(boardCost, that.boardCost) &&
      Objects.equals(boardSlack, that.boardSlack) &&
      Objects.equals(hopTime, that.hopTime) &&
      reluctance == that.reluctance
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(boardCost, boardSlack, hopTime, reluctance);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(ElevatorPreferences.class)
      .addObj("boardCost", boardCost, DEFAULT.boardCost)
      .addDuration("boardSlack", boardSlack, DEFAULT.boardSlack)
      .addDuration("hopTime", hopTime, DEFAULT.hopTime)
      .addNum("reluctance", reluctance, DEFAULT.reluctance)
      .toString();
  }

  public static class Builder {

    private final ElevatorPreferences original;
    private Cost boardCost;
    private Duration boardSlack;
    private Duration hopTime;
    private double reluctance;

    public Builder(ElevatorPreferences original) {
      this.original = original;
      this.boardCost = original.boardCost;
      this.boardSlack = original.boardSlack;
      this.hopTime = original.hopTime;
      this.reluctance = original.reluctance;
    }

    public ElevatorPreferences original() {
      return original;
    }

    public Builder withBoardCost(int boardCost) {
      this.boardCost = Cost.costOfSeconds(boardCost);
      return this;
    }

    public Builder withBoardSlack(Duration boardSlack) {
      this.boardSlack = boardSlack;
      return this;
    }

    public Builder withHopTime(Duration hopTime) {
      this.hopTime = hopTime;
      return this;
    }

    public Builder withReluctance(double reluctance) {
      this.reluctance = reluctance;
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
