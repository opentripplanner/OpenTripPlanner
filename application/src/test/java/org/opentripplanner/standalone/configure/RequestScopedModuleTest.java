package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertWithMessage;

import dagger.Provides;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.standalone.api.HttpRequestScoped;

/**
 * Every binding in {@link RequestScopedModule} must be pinned to the {@link HttpRequestScoped}
 * scope — an unscoped {@code @Provides} method would be re-created on every injection point
 * instead of once per HTTP request, breaking the consistent view of real-time data the module is
 * meant to guarantee.
 */
class RequestScopedModuleTest {

  @Test
  void everyProvidesMethodIsHttpRequestScoped() {
    var unscoped = List.of(RequestScopedModule.class.getDeclaredMethods())
      .stream()
      .filter(method -> method.isAnnotationPresent(Provides.class))
      .filter(method -> !method.isAnnotationPresent(HttpRequestScoped.class))
      .map(Method::getName)
      .toList();

    assertWithMessage(
      "All @Provides methods in %s must also be annotated @HttpRequestScoped, but these are not: %s",
      RequestScopedModule.class.getSimpleName(),
      unscoped
    )
      .that(unscoped)
      .isEmpty();
  }
}
