package org.opentripplanner.framework.token;

import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import org.opentripplanner.utils.time.DurationUtils;

public interface TestTokenSchemaConstants {
  // Token field names. These are used to reference a specific field value in theString BYTE_FIELD = "AByte";
  // token to avoid index errors. They are not part of the serialized token.String DURATION_FIELD = "ADur";
  String BOOLEAN_TRUE_FIELD = "ABoolTrue";
  String BOOLEAN_FALSE_FIELD = "ABoolFalse";
  String DURATION_FIELD = "ADur";
  String ENUM_FIELD = "EnField";
  String INT_FIELD = "ANum";
  String STRING_FIELD = "AStr";
  String TIME_INSTANT_FIELD = "ATime";

  Duration DURATION_VALUE = DurationUtils.duration("2m13s");
  Month ENUM_VALUE = Month.MAY;
  Class<Month> ENUM_CLASS = Month.class;
  int INT_VALUE = 31;
  String STRING_VALUE = "text";
  Instant TIME_INSTANT_VALUE = Instant.parse("2023-10-23T10:00:59Z");

  String BOOLEAN_ENCODED = "MXx0cnVlfGZhbHNlfA==";
  String DURATION_ENCODED = "MnwybTEzc3w=";
  String ENUM_ENCODED = "M3xNQVl8";
  String INT_ENCODED = "NXwzMXw=";
  String STRING_ENCODED = "N3x0ZXh0fA==";
  String TIME_INSTANT_ENCODED = "MTN8MjAyMy0xMC0yM1QxMDowMDo1OVp8";
}
