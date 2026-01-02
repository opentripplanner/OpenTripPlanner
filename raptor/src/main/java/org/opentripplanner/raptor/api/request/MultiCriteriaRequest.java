package org.opentripplanner.raptor.api.request;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Parameters to configure the multi-criteria search.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class MultiCriteriaRequest<T extends RaptorTripSchedule> {

  private final RelaxFunction relaxC1;

  @Nullable
  private final RaptorTransitGroupPriorityCalculator transitPriorityCalculator;

  private MultiCriteriaRequest() {
    this.relaxC1 = RelaxFunction.NORMAL;
    this.transitPriorityCalculator = null;
  }

  public MultiCriteriaRequest(Builder<T> builder) {
    this.relaxC1 = Objects.requireNonNull(builder.relaxC1());
    this.transitPriorityCalculator = builder.transitPriorityCalculator();
  }

  public static <S extends RaptorTripSchedule> Builder<S> of() {
    return new Builder<S>(new MultiCriteriaRequest<>());
  }

  public Builder<T> copyOf() {
    return new Builder<>(this);
  }

  /**
   * Whether to accept non-optimal trips if they are close enough with respect to
   * c1(generalized-cost). In other words this relaxes the pareto comparison at
   * each stop and at the destination.
   * <p>
   * Let {@code c} be the existing minimum pareto optimal cost to beat. Then a trip
   * with cost {@code c'} is accepted if the following is true:
   * <pre>
   * c' < RelaxFunction.relax(c)
   * </pre>
   * The default is {@link RelaxFunction#NORMAL}.
   */
  public RelaxFunction relaxC1() {
    return relaxC1;
  }

  public Optional<RaptorTransitGroupPriorityCalculator> transitPriorityCalculator() {
    return Optional.ofNullable(transitPriorityCalculator);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MultiCriteriaRequest<?> that = (MultiCriteriaRequest<?>) o;
    return (
      Objects.equals(relaxC1, that.relaxC1) &&
      Objects.equals(transitPriorityCalculator, that.transitPriorityCalculator)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(relaxC1, transitPriorityCalculator);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(MultiCriteriaRequest.class)
      .addObj("relaxC1", relaxC1, RelaxFunction.NORMAL)
      .addObj("transitPriorityCalculator", transitPriorityCalculator)
      .toString();
  }

  public static class Builder<T extends RaptorTripSchedule> {

    private final MultiCriteriaRequest<T> original;
    private RelaxFunction relaxC1;
    private RaptorTransitGroupPriorityCalculator transitPriorityCalculator;

    public Builder(MultiCriteriaRequest<T> original) {
      this.original = original;
      this.relaxC1 = original.relaxC1;
      this.transitPriorityCalculator = original.transitPriorityCalculator;
    }

    @Nullable
    public RelaxFunction relaxC1() {
      return relaxC1;
    }

    public Builder<T> withRelaxC1(RelaxFunction relaxC1) {
      this.relaxC1 = relaxC1;
      return this;
    }

    @Nullable
    public RaptorTransitGroupPriorityCalculator transitPriorityCalculator() {
      return transitPriorityCalculator;
    }

    public Builder<T> withTransitPriorityCalculator(RaptorTransitGroupPriorityCalculator value) {
      transitPriorityCalculator = value;
      return this;
    }

    public MultiCriteriaRequest<T> build() {
      var newInstance = new MultiCriteriaRequest<T>(this);
      return original.equals(newInstance) ? original : newInstance;
    }

    @Override
    public String toString() {
      return ToStringBuilder.of(MultiCriteriaRequest.Builder.class)
        .addObj("relaxC1", relaxC1)
        .addObj("transitPriorityCalculator", transitPriorityCalculator)
        .toString();
    }
  }
}
