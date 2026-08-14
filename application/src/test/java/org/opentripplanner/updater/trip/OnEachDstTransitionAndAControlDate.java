package org.opentripplanner.updater.trip;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

/**
 * Runs a test on each of the two daylight-saving transitions in the time zone of
 * {@link org.opentripplanner.transit.model.TransitTestEnvironment}, and on a control date without
 * one. The test takes the case name and the service date as its parameters.
 * <p>
 * Applying a real-time update should give the same result whether or not the service date contains
 * a daylight-saving transition.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ParameterizedTest(name = "{0}")
@ArgumentsSource(OnEachDstTransitionAndAControlDate.ServiceDates.class)
public @interface OnEachDstTransitionAndAControlDate {
  class ServiceDates implements ArgumentsProvider {

    /** Clocks jump 02:00 → 03:00, so the calendar day is 23 hours long. */
    private static final LocalDate SPRING_FORWARD = LocalDate.of(2024, 3, 31);

    /** Clocks fall back 03:00 → 02:00, so the calendar day is 25 hours long. */
    private static final LocalDate FALL_BACK = LocalDate.of(2024, 10, 27);

    /** Control: no transition, so calendar midnight and the start of service coincide. */
    private static final LocalDate NO_TRANSITION = LocalDate.of(2024, 5, 7);

    @Override
    public Stream<? extends Arguments> provideArguments(
      ParameterDeclarations parameters,
      ExtensionContext context
    ) {
      return Stream.of(
        Arguments.of("spring forward (23-hour day)", SPRING_FORWARD),
        Arguments.of("fall back (25-hour day)", FALL_BACK),
        Arguments.of("no transition", NO_TRANSITION)
      );
    }
  }
}
