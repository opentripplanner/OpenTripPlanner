package org.opentripplanner.test.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.params.provider.ArgumentsSource;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ArgumentsSource(FilePatternArgumentsProvider.class)
public @interface FilePatternSource {
  /**
   * The glob pattern to search for
   */
  String[] pattern();
}
