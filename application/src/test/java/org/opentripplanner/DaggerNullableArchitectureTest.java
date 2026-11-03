package org.opentripplanner;

import static com.google.common.truth.Truth.assertWithMessage;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;

/**
 * Dagger does not recognise the type-use {@code org.jspecify.annotations.Nullable}. If it is used
 * on a Dagger site, Dagger generates null checks around the value, and a nullable provider crashes
 * at startup when its feature is off. Use the declaration-style
 * {@code jakarta.annotation.Nullable} on the Dagger sites: {@code @Provides}, {@code @Binds},
 * {@code @BindsInstance}, {@code @Inject} and the methods of {@code @Component}s and
 * {@code @Subcomponent}s. Everywhere else, use JSpecify.
 */
public class DaggerNullableArchitectureTest {

  private static final String JSPECIFY_NULLABLE = "org.jspecify.annotations.Nullable";
  private static final List<String> DAGGER_METHOD_ANNOTATIONS = List.of(
    "dagger.Provides",
    "dagger.Binds",
    "dagger.BindsInstance",
    "jakarta.inject.Inject",
    "javax.inject.Inject"
  );
  private static final List<String> DAGGER_COMPONENT_ANNOTATIONS = List.of(
    "dagger.Component",
    "dagger.Subcomponent",
    "dagger.Component$Builder",
    "dagger.Component$Factory",
    "dagger.Subcomponent$Builder",
    "dagger.Subcomponent$Factory"
  );

  @Test
  void jSpecifyNullableMustNotBeUsedOnDaggerSites() {
    var violations = new ArrayList<String>();
    for (var javaClass : ArchComponent.OTP_CLASSES) {
      Class<?> c;
      try {
        c = javaClass.reflect();
      } catch (Throwable ignore) {
        continue;
      }
      boolean componentType = hasAnyAnnotation(c, DAGGER_COMPONENT_ANNOTATIONS);
      for (Executable e : executables(c)) {
        if (componentType || hasAnyAnnotation(e, DAGGER_METHOD_ANNOTATIONS)) {
          if (hasJSpecifyNullable(e)) {
            violations.add(c.getName() + "#" + e.getName());
          }
        }
      }
      for (var f : c.getDeclaredFields()) {
        if (hasAnyAnnotation(f, DAGGER_METHOD_ANNOTATIONS)) {
          if (isNullable(f.getAnnotatedType())) {
            violations.add(c.getName() + "." + f.getName());
          }
        }
      }
    }
    assertWithMessage(
      "Use jakarta.annotation.Nullable instead of " + JSPECIFY_NULLABLE + " on Dagger sites"
    )
      .that(violations)
      .isEmpty();
  }

  private static Stream<Executable> executablesStream(Class<?> c) {
    return Stream.concat(
      Arrays.stream(c.getDeclaredMethods()),
      Arrays.stream(c.getDeclaredConstructors())
    );
  }

  private static List<Executable> executables(Class<?> c) {
    return executablesStream(c).toList();
  }

  private static boolean hasJSpecifyNullable(Executable e) {
    boolean ret = e instanceof Method m && isNullable(m.getAnnotatedReturnType());
    return (
      ret ||
      Arrays.stream(e.getAnnotatedParameterTypes()).anyMatch(
        DaggerNullableArchitectureTest::isNullable
      )
    );
  }

  private static boolean isNullable(AnnotatedType type) {
    return Arrays.stream(type.getAnnotations()).anyMatch(a ->
      a.annotationType().getName().equals(JSPECIFY_NULLABLE)
    );
  }

  private static boolean hasAnyAnnotation(AnnotatedElement element, List<String> names) {
    for (Annotation a : element.getAnnotations()) {
      if (names.contains(a.annotationType().getName())) {
        return true;
      }
    }
    return false;
  }
}
