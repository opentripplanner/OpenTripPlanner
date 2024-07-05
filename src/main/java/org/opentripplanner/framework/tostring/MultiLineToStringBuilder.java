package org.opentripplanner.framework.tostring;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.framework.time.DurationUtils;

/**
 * When debug logging it is much more readable if the logging is nicely formatted with line-breaks.
 * This builder can be used to create a nice list of key/values. It also allows breaking a
 * collection of values into lines. It only allows for two nested levels, see {@code PageCursor}
 * below.
 * <p>
 * Example output:
 * <pre>
 * Response {
 * 	SearchWindowUsed : 50m
 * 	NextPage........ : PageCursor{type: NEXT_PAGE, ...}
 * 	PreviousPage.... : PageCursor{type: PREVIOUS_PAGE, ...}
 * 	Itineraries..... : [
 * 		Origin ~ Walk 2h20m49s ~ Destination [$20186]
 * 		Origin ~ Walk 1m47s ~ Malmö C ~ RAIL Pågatåg 5:55 6:03 ~ ... ~ Destination [$1587]
 * 		Origin ~ Walk 2m31s ~ Malmö C ~ RAIL Ö-tåg 5:08 5:28 ~ ... ~ Destination [$4142]
 * 	]
 * }
 * </pre>
 * The {@code '...'} is just to shorten the doc, this class does not truncate lines.
 */
public class MultiLineToStringBuilder {

  private static final String NL_INDENT_1 = "\n  ";
  private static final String NL_INDENT_2 = "\n    ";

  private final String name;
  private final List<Item> items = new ArrayList<>();

  private MultiLineToStringBuilder(String name) {
    this.name = name;
  }

  public static MultiLineToStringBuilder of(String name) {
    return new MultiLineToStringBuilder(name);
  }

  public MultiLineToStringBuilder add(String label, Object value) {
    return addIf(label, value, Objects::nonNull, Object::toString);
  }

  public MultiLineToStringBuilder addDuration(String label, Duration value) {
    return addIf(label, value, Objects::nonNull, DurationUtils::durationToStr);
  }

  public MultiLineToStringBuilder addColNl(String label, Collection<?> value) {
    return addIf(label, value, this::colExist, this::colToString);
  }

  private <T> MultiLineToStringBuilder addIf(
    String label,
    T value,
    Predicate<T> ignoreValue,
    Function<T, String> toString
  ) {
    if (ignoreValue.test(value)) {
      items.add(new Item(label, toString.apply(value)));
    }
    return this;
  }

  private boolean colExist(Collection<?> c) {
    return !(c == null || c.isEmpty());
  }

  private String colToString(Collection<?> c) {
    return c
      .stream()
      .map(Object::toString)
      .collect(Collectors.joining(NL_INDENT_2, "[" + NL_INDENT_2, NL_INDENT_1 + "]"));
  }

  public String toString() {
    var buf = new StringBuilder(name).append(" {");
    int labelSize = items.stream().mapToInt(it -> it.key.length()).max().orElse(0);

    for (Item item : items) {
      var labelTxt = padRight(item.key(), labelSize);
      buf.append(NL_INDENT_1).append(labelTxt).append(" : ").append(item.value());
    }

    return buf.append("\n}").toString();
  }

  private String padRight(String value, int size) {
    var buf = new StringBuilder(value);
    while (buf.length() < size) {
      buf.append('.');
    }
    return buf.toString();
  }

  private record Item(String key, Object value) {}
}
