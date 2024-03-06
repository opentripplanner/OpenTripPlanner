package org.opentripplanner.raptor.api.model;

/**
 * Raptor relies on {@code int} operation to be fast, so in many cases we use a "magic number" to
 * represent state. In general "magic numbers" should be avoided, at least encapsulated - but in
 * Raptor performance is more important. All Raptor "magic numbers" used in OTP are listed here.
 * <p>
 * They are made public, because they appear in logging, debugging and sometimes as
 * parameters/return values for functions and methods in the API and SPI. These are not meant to
 * be global OTP constants, so use them within Raptor and in interaction with Raptor only.
 * <p>
 * When choosing a value we avoid {@code -1}, {@link  Integer#MIN_VALUE} and
 * {@link Integer#MIN_VALUE}. {@code -1} is often a legal value and a calculation error can
 * often <em>overflow</em> to {@code -1} - even for strictly positive types like
 * {@code number-of-transfers}. So we use big negative "random" numbers - with some distance
 * apart from each other. We do not use {@link Integer#MIN_VALUE}, because this could potentially
 * lead to overflow situations which would be very hard to debug. If you add -1 to
 * {@code Integer.MIN_VALUE} and you get a positive number - not an exception.
 */
public class RaptorConstants {

  /**
   * Zero (0) constant used inside Raptor.
   */
  public static final int ZERO = 0;

  /**
   * One minute is 60 seconds - iteration departure times are usually increased by one minute.
   */
  public static final int ONE_MINUTE = 60;

  /**
   * This constant is used to indicate that a value is not set. This applies to parameters of type
   * {@code generalized-cost}, {@code link min-travel-time} and {@code duration} inside Raptor.
   * Most of these types are "positive" numbers, but we want Raptor to be robust and work even
   * for negative values; Hence we choose a large negative value.
   */
  public static final int NOT_SET = -1_999_000_000;

  /**
   * When searching for a stop position or trip index we will use this to signal NOT FOUND.
   * We distinguish between this and {@link #NOT_SET}.
   */
  public static final int NOT_FOUND = -2_111_000_000;

  /**
   * Raptor initialize time, generalized-cost, number-of-transfers and duration result values with
   * this constant. It is used when a small value is considered better than a high value. Since
   * all real-world values are better than this, only one comparison is needed to check if a new
   * candidate value is the new best-value.
   * <p>
   * We also avoid using the same value as {@link #NOT_SET}, so we accidentally do not confuse
   * these two different states.
   */
  public static final int UNREACHED_HIGH = 2_000_000_000;

  /**
   * In cases where a large value is better than a low value we initialize with UNREACHED_LOW.
   * For example, this is the case when we search in reverse (time).
   * <p>
   * See {@link #UNREACHED_HIGH}
   */
  public static final int UNREACHED_LOW = -2_000_000_000;

  /** Alias for {@link #NOT_SET} */
  public static final int TIME_NOT_SET = NOT_SET;

  /** Alias for {@link #UNREACHED_HIGH} */
  public static final int TIME_UNREACHED_FORWARD = UNREACHED_HIGH;

  /** Alias for {@link #UNREACHED_LOW} */
  public static final int TIME_UNREACHED_REVERSE = UNREACHED_LOW;

  /** Alias for {@link #UNREACHED_HIGH} */
  public static final int N_TRANSFERS_UNREACHED = UNREACHED_HIGH;
}
