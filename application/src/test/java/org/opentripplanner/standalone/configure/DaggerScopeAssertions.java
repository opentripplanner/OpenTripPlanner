package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Shared reflection machinery behind the DI-scope regression tests: reflects over every no-arg
 * accessor on a factory or context interface, checks every accessor's binding ({@link
 * DaggerBindingKey}) is classified into exactly one bucket, then invokes the accessors to check
 * the classification matches what Dagger actually does at runtime.
 * <p>
 * Two shapes of check are needed, since a single-component factory and a per-request factory
 * disagree on what "correctly scoped" even means: {@link #assertSingleInstanceScope} invokes each
 * accessor twice on one instance; {@link #assertRequestScope} invokes twice on one request
 * instance and once on a second, independent one.
 */
final class DaggerScopeAssertions {

  private DaggerScopeAssertions() {}

  /**
   * For a single-component factory (e.g. {@code ConstructApplicationFactory}): every accessor is
   * invoked twice on {@code instance} and must be classified as a singleton (same instance both
   * times), a known unscoped bug (different instances — tracked, not yet fixed), or built fresh
   * by design (different instances, expected).
   */
  static void assertSingleInstanceScope(
    Class<?> accessorInterface,
    Object instance,
    List<DaggerBindingKey> singletons,
    List<DaggerBindingKey> knownUnscopedBugs,
    List<DaggerBindingKey> prototypeByDesign
  ) {
    try {
      var accessors = accessorsOf(accessorInterface);
      assertExhaustive(
        accessorInterface,
        accessors,
        singletons,
        knownUnscopedBugs,
        prototypeByDesign
      );

      var failures = new ArrayList<String>();
      for (var method : accessors) {
        var key = DaggerBindingKey.ofAccessor(method);
        var first = method.invoke(instance);
        var second = method.invoke(instance);

        if (singletons.contains(key) && first != second) {
          failures.add(method.getName() + "() should be a singleton but was rebuilt");
        } else if (knownUnscopedBugs.contains(key) && first == second) {
          failures.add(
            method.getName() +
              "() is a known unscoped bug but now returns a stable instance — reclassify " +
              key
          );
        } else if (prototypeByDesign.contains(key) && first == second) {
          failures.add(method.getName() + "() should build a fresh instance every call");
        }
      }
      assertThat(failures).isEmpty();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * For a per-request factory or context (e.g. {@code RequestScopedFactory}, {@code
   * GtfsGraphQLRequestContext}): every accessor is invoked twice on {@code requestOne} and once
   * on the independent {@code requestTwo}, and must be classified as an application singleton
   * (stable within and across requests), correctly request-scoped (stable within one request,
   * different across requests), a known unscoped bug (different even within one request —
   * tracked, not yet fixed), or ignored (not a Dagger binding at all — e.g. hand-memoized on the
   * wrapper itself — so no scope behavior is asserted; only listed for exhaustiveness).
   */
  static void assertRequestScope(
    Class<?> accessorInterface,
    Object requestOne,
    Object requestTwo,
    List<DaggerBindingKey> applicationSingleton,
    List<DaggerBindingKey> requestScoped,
    List<DaggerBindingKey> knownUnscopedBugs,
    List<DaggerBindingKey> ignored
  ) {
    try {
      var accessors = accessorsOf(accessorInterface);
      assertExhaustive(
        accessorInterface,
        accessors,
        applicationSingleton,
        requestScoped,
        knownUnscopedBugs,
        ignored
      );

      var failures = new ArrayList<String>();
      for (var method : accessors) {
        var key = DaggerBindingKey.ofAccessor(method);
        if (ignored.contains(key)) {
          continue;
        }
        var withinRequestFirst = method.invoke(requestOne);
        var withinRequestSecond = method.invoke(requestOne);
        var acrossRequests = method.invoke(requestTwo);

        if (knownUnscopedBugs.contains(key)) {
          if (withinRequestFirst == withinRequestSecond) {
            failures.add(
              method.getName() +
                "() is a known unscoped bug but is now stable within one request — reclassify " +
                key
            );
          }
          continue;
        }

        if (withinRequestFirst != withinRequestSecond) {
          failures.add(method.getName() + "() should be cached within one request but was rebuilt");
          continue;
        }

        if (applicationSingleton.contains(key) && withinRequestFirst != acrossRequests) {
          failures.add(
            method.getName() + "() should be stable across requests but differed across requests"
          );
        } else if (requestScoped.contains(key) && withinRequestFirst == acrossRequests) {
          failures.add(
            method.getName() +
              "() is request-scoped but returned the same instance across two independent " +
              "requests — move " +
              key +
              " to the application-singleton list"
          );
        }
      }
      assertThat(failures).isEmpty();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Method> accessorsOf(Class<?> accessorInterface) {
    return Stream.of(accessorInterface.getDeclaredMethods())
      .filter(method -> method.getParameterCount() == 0)
      .toList();
  }

  @SafeVarargs
  private static void assertExhaustive(
    Class<?> accessorInterface,
    List<Method> accessors,
    List<DaggerBindingKey>... buckets
  ) {
    var classified = Stream.of(buckets).flatMap(List::stream).toList();
    var unclassified = accessors
      .stream()
      .map(DaggerBindingKey::ofAccessor)
      .filter(key -> !classified.contains(key))
      .toList();

    assertWithMessage(
      "Every accessor on %s must be classified, but these are not: %s",
      accessorInterface.getSimpleName(),
      unclassified
    )
      .that(unclassified)
      .isEmpty();
  }
}
