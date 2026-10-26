package org.opentripplanner.standalone.configure;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

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
   * Fluent, named-argument entry point for {@link #assertSingleInstanceScope}, so the three
   * same-typed classification lists can't be mixed up at the call site.
   */
  static SingleInstanceScopeBuilder singleInstanceScope(
    Class<?> accessorInterface,
    Object instance
  ) {
    return new SingleInstanceScopeBuilder(accessorInterface, instance);
  }

  /**
   * Fluent, named-argument entry point for {@link #assertRequestScope}, so the three same-typed
   * classification lists can't be mixed up at the call site.
   */
  static RequestScopeBuilder requestScope(
    Class<?> accessorInterface,
    Object requestOne,
    Object requestTwo
  ) {
    return new RequestScopeBuilder(accessorInterface, requestOne, requestTwo);
  }

  static final class SingleInstanceScopeBuilder {

    private final Class<?> accessorInterface;
    private final Object instance;
    private List<DaggerBindingKey> singletons = List.of();
    private List<DaggerBindingKey> knownUnscopedBugs = List.of();
    private List<DaggerBindingKey> prototypeByDesign = List.of();

    private SingleInstanceScopeBuilder(Class<?> accessorInterface, Object instance) {
      this.accessorInterface = accessorInterface;
      this.instance = instance;
    }

    SingleInstanceScopeBuilder singletons(List<DaggerBindingKey> keys) {
      this.singletons = keys;
      return this;
    }

    SingleInstanceScopeBuilder knownUnscopedBugs(List<DaggerBindingKey> keys) {
      this.knownUnscopedBugs = keys;
      return this;
    }

    SingleInstanceScopeBuilder prototypeByDesign(List<DaggerBindingKey> keys) {
      this.prototypeByDesign = keys;
      return this;
    }

    void assertScopes() {
      assertSingleInstanceScope(
        accessorInterface,
        instance,
        singletons,
        knownUnscopedBugs,
        prototypeByDesign
      );
    }
  }

  static final class RequestScopeBuilder {

    private final Class<?> accessorInterface;
    private final Object requestOne;
    private final Object requestTwo;
    private List<DaggerBindingKey> applicationSingleton = List.of();
    private List<DaggerBindingKey> requestScoped = List.of();
    private List<DaggerBindingKey> knownUnscopedBugs = List.of();

    private RequestScopeBuilder(Class<?> accessorInterface, Object requestOne, Object requestTwo) {
      this.accessorInterface = accessorInterface;
      this.requestOne = requestOne;
      this.requestTwo = requestTwo;
    }

    RequestScopeBuilder applicationSingleton(List<DaggerBindingKey> keys) {
      this.applicationSingleton = keys;
      return this;
    }

    RequestScopeBuilder requestScoped(List<DaggerBindingKey> keys) {
      this.requestScoped = keys;
      return this;
    }

    RequestScopeBuilder knownUnscopedBugs(List<DaggerBindingKey> keys) {
      this.knownUnscopedBugs = keys;
      return this;
    }

    void assertScopes() {
      assertRequestScope(
        accessorInterface,
        requestOne,
        requestTwo,
        applicationSingleton,
        requestScoped,
        knownUnscopedBugs
      );
    }
  }

  /**
   * For a single-component factory (e.g. {@code ConstructApplicationFactory}): every accessor is
   * invoked twice on {@code instance} and must be classified as a singleton (same instance both
   * times), a known unscoped bug (different instances — tracked, not yet fixed), or built fresh
   * by design (different instances, expected).
   */
  private static void assertSingleInstanceScope(
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
        if (nullIsHandled(method, first, failures)) {
          continue;
        }
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
   * tracked, not yet fixed).
   */
  private static void assertRequestScope(
    Class<?> accessorInterface,
    Object requestOne,
    Object requestTwo,
    List<DaggerBindingKey> applicationSingleton,
    List<DaggerBindingKey> requestScoped,
    List<DaggerBindingKey> knownUnscopedBugs
  ) {
    try {
      var accessors = accessorsOf(accessorInterface);
      assertExhaustive(
        accessorInterface,
        accessors,
        applicationSingleton,
        requestScoped,
        knownUnscopedBugs
      );

      var failures = new ArrayList<String>();
      for (var method : accessors) {
        var key = DaggerBindingKey.ofAccessor(method);
        var withinRequestFirst = method.invoke(requestOne);
        if (nullIsHandled(method, withinRequestFirst, failures)) {
          continue;
        }
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

  /**
   * A {@code null} binding (typically a disabled sandbox feature) has no instance to compare, so
   * its scope cannot be checked and it is skipped. That is only accepted for accessors explicitly
   * marked {@link Nullable}; any other accessor returning {@code null} is reported as a failure.
   *
   * @return true if {@code value} is null, so the caller must skip the scope comparison
   */
  private static boolean nullIsHandled(Method accessor, Object value, List<String> failures) {
    if (value != null) {
      return false;
    }
    if (!accessor.isAnnotationPresent(Nullable.class)) {
      failures.add(accessor.getName() + "() returned null but is not annotated @Nullable");
    }
    return true;
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
    var accessorKeys = accessors.stream().map(DaggerBindingKey::ofAccessor).toList();

    var unclassified = accessorKeys
      .stream()
      .filter(key -> Stream.of(buckets).noneMatch(bucket -> bucket.contains(key)))
      .toList();

    assertWithMessage(
      "Every accessor on %s must be classified, but these are not: %s",
      accessorInterface.getSimpleName(),
      unclassified
    )
      .that(unclassified)
      .isEmpty();

    var multiplyClassified = accessorKeys
      .stream()
      .filter(
        key ->
          Stream.of(buckets)
            .filter(bucket -> bucket.contains(key))
            .count() > 1
      )
      .toList();

    assertWithMessage(
      "Every accessor on %s must be classified into exactly one bucket, but these are in more " +
        "than one: %s",
      accessorInterface.getSimpleName(),
      multiplyClassified
    )
      .that(multiplyClassified)
      .isEmpty();
  }
}
