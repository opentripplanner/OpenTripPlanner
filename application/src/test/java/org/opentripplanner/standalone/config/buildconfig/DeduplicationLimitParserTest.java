package org.opentripplanner.standalone.config.buildconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;

class DeduplicationLimitParserTest {

  @Test
  void parsesTheCoefficientFirst() {
    var subject = DeduplicationLimitParser.parse("0.005x + 30s");

    assertThat(subject).isEqualTo(new DeduplicationLimit(Cost.costOfSeconds(30), 0.005));
  }

  @Test
  void parsesTheConstantFirst() {
    var subject = DeduplicationLimitParser.parse("30s + 0.005 t");

    assertThat(subject).isEqualTo(new DeduplicationLimit(Cost.costOfSeconds(30), 0.005));
  }

  @Test
  void theCoefficientKeepsItsExactValue() {
    // OTP's linear functions round the coefficient to a step of 0.1, this must not
    assertThat(DeduplicationLimitParser.parse("0.015x + 1m").coefficient()).isEqualTo(0.015);
  }

  @Test
  void parsesAConstantWithMinutesAndSeconds() {
    var subject = DeduplicationLimitParser.parse("1m30s + 0.1 X");

    assertThat(subject.constant()).isEqualTo(Cost.costOfSeconds(90));
    assertThat(subject.coefficient()).isEqualTo(0.1);
  }

  @Test
  void theConstantCanBeLeftOut() {
    var subject = DeduplicationLimitParser.parse("0.02x");

    assertThat(subject).isEqualTo(new DeduplicationLimit(Cost.ZERO, 0.02));
  }

  @Test
  void theCoefficientCanBeLeftOut() {
    var subject = DeduplicationLimitParser.parse("45s");

    assertThat(subject).isEqualTo(new DeduplicationLimit(Cost.costOfSeconds(45), 0.0));
  }

  @Test
  void rejectsTextThatIsNotALimit() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      DeduplicationLimitParser.parse("a lot")
    );
    assertThat(ex).hasMessageThat().contains("a lot");
  }

  @Test
  void rejectsTwoConstantsOrTwoCoefficients() {
    assertThrows(IllegalArgumentException.class, () -> DeduplicationLimitParser.parse("30s + 1m"));
    assertThrows(IllegalArgumentException.class, () ->
      DeduplicationLimitParser.parse("0.1x + 0.2x")
    );
  }

  @Test
  void rejectsAnEmptyTerm() {
    assertThrows(IllegalArgumentException.class, () -> DeduplicationLimitParser.parse("30s +"));
    assertThrows(IllegalArgumentException.class, () -> DeduplicationLimitParser.parse(""));
  }
}
