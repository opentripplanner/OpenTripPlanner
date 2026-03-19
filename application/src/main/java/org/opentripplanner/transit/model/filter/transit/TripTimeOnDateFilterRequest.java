package org.opentripplanner.transit.model.filter.transit;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A filter for {@link org.opentripplanner.model.TripTimeOnDate} objects using select/not semantics.
 * <p>
 * Select: a TripTimeOnDate must match at least one select criterion (OR between selects).
 * Not: a TripTimeOnDate is excluded if it matches any not criterion.
 * A filter with no select and no not matches everything.
 */
public class TripTimeOnDateFilterRequest {

  @Nullable
  private final List<TripTimeOnDateSelectRequest> select;

  @Nullable
  private final List<TripTimeOnDateSelectRequest> not;

  private TripTimeOnDateFilterRequest(Builder builder) {
    this.select = builder.select.isEmpty() ? null : List.copyOf(builder.select);
    this.not = builder.not.isEmpty() ? null : List.copyOf(builder.not);
  }

  public static Builder of() {
    return new Builder();
  }

  @Nullable
  public List<TripTimeOnDateSelectRequest> select() {
    return select;
  }

  @Nullable
  public List<TripTimeOnDateSelectRequest> not() {
    return not;
  }

  @Override
  public String toString() {
    if (select == null && not == null) {
      return "ALL";
    }
    return ToStringBuilder.ofEmbeddedType().addCol("select", select).addCol("not", not).toString();
  }

  public static class Builder {

    private final List<TripTimeOnDateSelectRequest> select = new ArrayList<>();
    private final List<TripTimeOnDateSelectRequest> not = new ArrayList<>();

    public Builder addSelect(TripTimeOnDateSelectRequest selectRequest) {
      this.select.add(selectRequest);
      return this;
    }

    public Builder addNot(TripTimeOnDateSelectRequest selectRequest) {
      this.not.add(selectRequest);
      return this;
    }

    public TripTimeOnDateFilterRequest build() {
      return new TripTimeOnDateFilterRequest(this);
    }
  }
}
