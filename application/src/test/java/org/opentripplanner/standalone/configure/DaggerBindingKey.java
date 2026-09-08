package org.opentripplanner.standalone.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Identifies a Dagger binding as seen from a factory accessor: its return type, plus the
 * qualifier annotation (if any) distinguishing it from other bindings of the same type — e.g. the
 * {@code @GtfsSchema}- and {@code @TransmodelSchema}-qualified {@code GraphQLSchema} bindings.
 */
record DaggerBindingKey(Class<?> type, Class<? extends Annotation> qualifier) {
  static DaggerBindingKey of(Class<?> type) {
    return new DaggerBindingKey(type, null);
  }

  static DaggerBindingKey of(Class<?> type, Class<? extends Annotation> qualifier) {
    return new DaggerBindingKey(type, qualifier);
  }

  static DaggerBindingKey ofAccessor(Method method) {
    return new DaggerBindingKey(method.getReturnType(), qualifierAnnotation(method));
  }

  private static Class<? extends Annotation> qualifierAnnotation(Method method) {
    return Arrays.stream(method.getAnnotations())
      .map(Annotation::annotationType)
      .filter(annotationType -> annotationType.isAnnotationPresent(Qualifier.class))
      .findFirst()
      .orElse(null);
  }

  @Override
  public String toString() {
    return qualifier == null
      ? type.getSimpleName()
      : "@" + qualifier.getSimpleName() + " " + type.getSimpleName();
  }
}
