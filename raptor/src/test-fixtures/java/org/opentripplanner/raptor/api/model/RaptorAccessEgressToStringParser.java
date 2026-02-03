package org.opentripplanner.raptor.api.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;

public final class RaptorAccessEgressToStringParser {

  private static final String LABEL = "(Walk|Flex|Flex\\+Walk|Free) ";
  private static final String BODY = "([^(~]*)";
  private static final String OPENING_HOURS = "(Open\\(.*\\) )?";
  private static final String STOP = "~ (\\w|\\d+)";
  private static final Pattern PARSE_INPUT = Pattern.compile(LABEL + BODY + OPENING_HOURS + STOP);

  private final ToIntFunction<String> stopIndexResolver;
  private final String input;
  private String label;
  private int stopIndex = -1;
  private int duration = RaptorConstants.NOT_SET;
  private boolean free = false;
  private boolean stopReachedOnBoard = false;
  private int openFrom = RaptorConstants.TIME_NOT_SET;
  private int openTo = RaptorConstants.TIME_NOT_SET;
  private boolean closed = false;
  private List<RaptorValue> fields = new ArrayList<>();

  public RaptorAccessEgressToStringParser(ToIntFunction<String> stopIndexResolver, String input) {
    this.stopIndexResolver = Objects.requireNonNull(stopIndexResolver);
    this.input = input.trim();
  }

  public static RaptorAccessEgressToStringParser parseAccessEgress(
    String input,
    ToIntFunction<String> stopIndexResolver
  ) {
    return new RaptorAccessEgressToStringParser(stopIndexResolver, input).parse();
  }

  public String label() {
    return label;
  }

  public int stopIndex() {
    return stopIndex;
  }

  public int duration() {
    return duration;
  }

  public boolean isFree() {
    return free;
  }

  public boolean isStopReachedOnBoard() {
    return stopReachedOnBoard;
  }

  public int openFrom() {
    return openFrom;
  }

  public int openTo() {
    return openTo;
  }

  public boolean isClosed() {
    return closed;
  }

  public Collection<RaptorValue> fields() {
    return List.copyOf(fields);
  }

  public Optional<RaptorValue> getField(RaptorValueType type) {
    return fields
      .stream()
      .filter(it -> it.type() == type)
      .findFirst();
  }

  private RaptorAccessEgressToStringParser parse() {
    var m = PARSE_INPUT.matcher(input);
    if (!m.matches()) {
      throw new IllegalArgumentException("'" + input + "' does not match :/" + PARSE_INPUT + "'");
    }
    var stopValue = m.group(4);
    this.stopIndex = (stopValue.matches("\\d+"))
      ? Integer.parseInt(stopValue)
      : stopIndexResolver.applyAsInt(stopValue);

    this.label = m.group(1);
    if ("Free".equals(label)) {
      this.free = true;
      this.duration = RaptorConstants.ZERO;
      return this;
    }

    var tokens = Arrays.stream(m.group(2).split(" +")).filter(StringUtils::hasValue).iterator();
    this.duration = DurationUtils.durationInSeconds(tokens.next());

    if ("Flex".equals(label)) {
      stopReachedOnBoard = true;
    }
    while (tokens.hasNext()) {
      var token = tokens.next();
      if (StringUtils.hasValue(token)) {
        if ("closed".equals(token)) {
          this.closed = true;
        } else {
          var value = RaptorValue.of(token);
          fields.add(value);
        }
      }
    }
    var openingHoures = m.group(3);
    if (StringUtils.hasValue(openingHoures)) {
      var hours = openingHoures.substring(5, openingHoures.length() - 2).split(" +");
      this.openFrom = TimeUtils.time(hours[0]);
      this.openTo = TimeUtils.time(hours[1]);
    }
    int rides = getField(RaptorValueType.RIDES).map(RaptorValue::value).orElse(0);
    if (label.startsWith("Flex") && rides == 0) {
      throw new IllegalArgumentException(
        "A Flex access/egress must have at leas one ride: " + input
      );
    }
    return this;
  }
}
