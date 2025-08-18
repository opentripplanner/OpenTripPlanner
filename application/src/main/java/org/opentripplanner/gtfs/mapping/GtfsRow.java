package org.opentripplanner.gtfs.mapping;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.utils.lang.StringUtils;

public class GtfsRow {

  protected final Map<String, String> fields;

  public GtfsRow(Map<String, String> fields) {
    this.fields = Map.copyOf(fields);
  }

  public String string(String field) {
    return optionalString(field).orElseThrow(() -> iae(field));
  }

  @Nullable
  public String nullableString(String field) {
    return fields.get(field);
  }

  public Optional<String> optionalString(String field) {
    return Optional.ofNullable(fields.get(field));
  }

  public int integer(String field) {
    return optionalInteger(field).orElseThrow(() -> iae(field));
  }

  public OptionalInt optionalInteger(String field) {
    return optionalString(field).stream().mapToInt(Integer::parseInt).findFirst();
  }

  /**
   * I know that 'double' is not spelled like this, but it's a reserved word.
   */
  public double doubble(String field) {
    return optionalDouble(field).orElseThrow(() -> iae(field));
  }

  public OptionalDouble optionalDouble(String field) {
    return optionalString(field).stream().mapToDouble(Double::parseDouble).findFirst();
  }

  private IllegalArgumentException iae(String field) {
    return new IllegalArgumentException(
      "Field '%s' is required but not present in CSV row %s".formatted(field, fields)
    );
  }
}
