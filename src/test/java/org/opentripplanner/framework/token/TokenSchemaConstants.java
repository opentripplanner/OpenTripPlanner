package org.opentripplanner.framework.token;

import java.time.Duration;
import java.time.Instant;
import org.opentripplanner.framework.time.DurationUtils;

public interface TokenSchemaConstants {
  // Token field names. These are used to reference a specific field value in theString BYTE_FIELD = "AByte";
  // token to avoid index errors. They are not part of the serialized token.String DURATION_FIELD = "ADur";
  String BYTE_FIELD = "AByte";
  String DURATION_FIELD = "ADur";
  String INT_FIELD = "ANum";
  String STRING_FIELD = "AStr";
  String TIME_INSTANT_FIELD = "ATime";

  byte BYTE_VALUE = 17;
  Duration DURATION_VALUE = DurationUtils.duration("2m13s");
  int INT_VALUE = 31;
  String STRING_VALUE = "text";
  Instant TIME_INSTANT_VALUE = Instant.parse("2023-10-23T10:00:59Z");

  String BYTE_ENCODED = "rO0ABXcEAAExEQ==";
  String DURATION_ENCODED = "rO0ABXcKAAEyAAUybTEzcw==";
  String INT_ENCODED = "rO0ABXcHAAEzAAIzMQ==";
  String STRING_ENCODED = "rO0ABXcJAAE3AAR0ZXh0";
  String TIME_INSTANT_ENCODED = "rO0ABXcaAAIxMwAUMjAyMy0xMC0yM1QxMDowMDo1OVo=";
}
