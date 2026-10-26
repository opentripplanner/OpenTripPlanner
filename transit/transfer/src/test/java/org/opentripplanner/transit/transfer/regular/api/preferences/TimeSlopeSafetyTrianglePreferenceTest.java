package org.opentripplanner.transit.transfer.regular.api.preferences;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TimeSlopeSafetyTrianglePreferenceTest {

  private static final double EPSILON = 0.0001;

  @Test
  void theValuesAreRelativeAndSumToOne() {
    assertNormalized(1.0, 0.0, 0.0, 1.0, 0.0, 0.0);
    assertNormalized(0.0, 1.0, 0.0, 0.0, 1.0, 0.0);
    assertNormalized(0.0, 0.0, 1.0, 0.0, 0.0, 1.0);
    assertNormalized(0.1, 0.0, 0.0, 1.0, 0.0, 0.0);
    assertNormalized(5.0, 0.0, 0.0, 1.0, 0.0, 0.0);
    assertNormalized(1.0, 1.0, 0.0, 0.5, 0.5, 0.0);
    assertNormalized(0.2, 0.3, 0.5, 0.2, 0.3, 0.5);
    assertNormalized(1.0, 2.0, 3.0, 0.15, 0.35, 0.5);
    assertNormalized(1.0, 1.0, 1.0, 0.35, 0.35, 0.3);
  }

  private static void assertNormalized(
    double time,
    double slope,
    double safety,
    double expectedTime,
    double expectedSlope,
    double expectedSafety
  ) {
    var subject = TimeSlopeSafetyTrianglePreference.of(time, slope, safety);

    assertThat(subject.time().asDouble()).isWithin(EPSILON).of(expectedTime);
    assertThat(subject.slope().asDouble()).isWithin(EPSILON).of(expectedSlope);
    assertThat(subject.safety().asDouble()).isWithin(EPSILON).of(expectedSafety);
    assertThat(subject.time().asDouble() + subject.slope().asDouble() + subject.safety().asDouble())
      .isWithin(EPSILON)
      .of(1.0);
  }

  @Test
  void aValueThatIsNotAMultipleOfTheStepIsRoundedAndTheSumIsKept() {
    var subject = TimeSlopeSafetyTrianglePreference.of(0.33, 0.33, 0.34);

    assertThat(subject.time().asDouble() + subject.slope().asDouble() + subject.safety().asDouble())
      .isWithin(EPSILON)
      .of(1.0);
  }

  @Test
  void noCombinationOfValuesFailsOrBreaksTheSumOfOne() {
    for (int t = 0; t <= 40; ++t) {
      for (int sl = 0; sl <= 40; ++sl) {
        for (int sa = 0; sa <= 40; ++sa) {
          if (t + sl + sa == 0) {
            continue;
          }
          var subject = TimeSlopeSafetyTrianglePreference.of(t / 40.0, sl / 40.0, sa / 40.0);
          double sum =
            subject.time().asDouble() + subject.slope().asDouble() + subject.safety().asDouble();
          assertThat(sum).isWithin(EPSILON).of(1.0);
        }
      }
    }
  }

  @Test
  void allValuesZeroIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 0.0)
    );
  }

  @Test
  void aNegativeValueIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      TimeSlopeSafetyTrianglePreference.of(-0.1, 0.5, 0.5)
    );
    assertThrows(IllegalArgumentException.class, () ->
      TimeSlopeSafetyTrianglePreference.of(0.5, -0.1, 0.5)
    );
    assertThrows(IllegalArgumentException.class, () ->
      TimeSlopeSafetyTrianglePreference.of(0.5, 0.5, -0.1)
    );
  }

  @Test
  void relativeValuesThatAreTheSameAreEqual() {
    var subject = TimeSlopeSafetyTrianglePreference.of(1.0, 0.0, 0.0);

    assertThat(TimeSlopeSafetyTrianglePreference.of(0.1, 0.0, 0.0)).isEqualTo(subject);
    assertThat(TimeSlopeSafetyTrianglePreference.of(0.1, 0.0, 0.0).hashCode()).isEqualTo(
      subject.hashCode()
    );
    assertThat(TimeSlopeSafetyTrianglePreference.of(0.0, 1.0, 0.0)).isNotEqualTo(subject);
  }

  @Test
  void theDistanceIsTheSumOfTheDistancesOfTheValues() {
    var time = TimeSlopeSafetyTrianglePreference.of(1.0, 0.0, 0.0);
    var slope = TimeSlopeSafetyTrianglePreference.of(0.0, 1.0, 0.0);

    assertThat(time.normalizedDistance(time)).isEqualTo(0f);
    assertThat(time.normalizedDistance(slope)).isWithin(0.0001f).of(2.0f);
    assertThat(time.normalizedDistance(slope)).isEqualTo(slope.normalizedDistance(time));
  }

  @Test
  void toStringHasTheValues() {
    assertThat(TimeSlopeSafetyTrianglePreference.of(1.0, 0.0, 0.0).toString()).contains("time");
  }
}
