package org.opentripplanner.raptor.api.request;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Parameters to configure the multi-criteria search.
 * <p>
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class MultiCriteriaRequest<T extends RaptorTripSchedule> {

  @Nullable
  private final Double relaxCostAtDestination;

  private MultiCriteriaRequest() {
    this.relaxCostAtDestination = null;
  }

  public MultiCriteriaRequest(Builder<T> builder) {
    this.relaxCostAtDestination = builder.relaxCostAtDestination();
  }

  public static <S extends RaptorTripSchedule> Builder<S> of() {
    return new Builder<S>(new MultiCriteriaRequest<>());
  }

  public Builder<T> copyOf() {
    return new Builder<>(this);
  }

  /**
   * Whether to accept non-optimal trips if they are close enough - if and only if they represent an
   * optimal path for their given iteration. In other words this slack only relaxes the pareto
   * comparison at the destination.
   * <p>
   * Let {@code c} be the existing minimum pareto optimal cost to beat. Then a trip with cost
   * {@code c'} is accepted if the following is true:
   * <pre>
   * c' < Math.round(c * relaxCostAtDestination)
   * </pre>
   * If the value is less than 1.0 a normal '<' comparison is performed.
   * <p>
   * The default is not set.
   * <p>
   * @deprecated This parameter only relax the cost at the destination, not at each stop. This
   * should be replaced with relaxing the cost at each stop instead.
   */
  @Deprecated
  public Optional<Double> relaxCostAtDestination() {
    return Optional.ofNullable(relaxCostAtDestination);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MultiCriteriaRequest<?> that = (MultiCriteriaRequest<?>) o;
    return Objects.equals(relaxCostAtDestination, that.relaxCostAtDestination);
  }

  @Override
  public int hashCode() {
    return Objects.hash(relaxCostAtDestination);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(MultiCriteriaRequest.class)
      .addNum("relaxCostAtDestination", relaxCostAtDestination)
      .toString();
  }

  public static class Builder<T extends RaptorTripSchedule> {

    private final MultiCriteriaRequest<T> original;

    private Double relaxCostAtDestination = null;

    public Builder(MultiCriteriaRequest<T> original) {
      this.original = original;
    }

    @Nullable
    @Deprecated
    public Double relaxCostAtDestination() {
      return relaxCostAtDestination;
    }

    @Deprecated
    public Builder<T> withRelaxCostAtDestination(Double value) {
      relaxCostAtDestination = value;
      return this;
    }

    public MultiCriteriaRequest<T> build() {
      var newInstance = new MultiCriteriaRequest<T>(this);
      return original.equals(newInstance) ? original : newInstance;
    }

    @Override
    public String toString() {
      return ToStringBuilder
        .of(MultiCriteriaRequest.Builder.class)
        .addNum("relaxCostAtDestination", relaxCostAtDestination)
        .toString();
    }
  }
}
