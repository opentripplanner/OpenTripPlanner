package org.opentripplanner.standalone.configure;

import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Identifies a Dagger binding as seen from a factory accessor: its generic return type, plus the
 * qualifier annotation (if any) distinguishing it from other bindings of the same type — e.g. the
 * {@code @GtfsSchema}- and {@code @TransmodelSchema}-qualified {@code GraphQLSchema} bindings, or
 * two unqualified accessors returning different parameterizations of the same generic type, such
 * as {@code RepositoryHandle<A, B>} vs. {@code RepositoryHandle<C, D>} — Dagger itself keys
 * generic bindings by their full parameterized type, so the erased raw type alone would wrongly
 * collapse those two into a single binding.
 */
record DaggerBindingKey(Type type, Class<? extends Annotation> qualifier) {
  static DaggerBindingKey of(Class<?> type) {
    return new DaggerBindingKey(type, null);
  }

  static DaggerBindingKey of(Class<?> type, Class<? extends Annotation> qualifier) {
    return new DaggerBindingKey(type, qualifier);
  }

  /**
   * For an accessor returning a parameterized generic type, e.g. {@code RepositoryHandle<A, B>},
   * where the raw type alone wouldn't distinguish it from another accessor's differently
   * parameterized binding of the same generic type. Capture the full type at the call site the
   * same way Dagger's own {@code TypeLiteral} (or Guava's {@code TypeToken}) does, e.g. {@code
   * of(new DaggerBindingKey.GenericType<RepositoryHandle<A, B>>() {})}.
   */
  static DaggerBindingKey of(GenericType<?> type) {
    return new DaggerBindingKey(type.type, null);
  }

  static DaggerBindingKey of(GenericType<?> type, Class<? extends Annotation> qualifier) {
    return new DaggerBindingKey(type.type, qualifier);
  }

  static DaggerBindingKey ofAccessor(Method method) {
    return new DaggerBindingKey(method.getGenericReturnType(), qualifierAnnotation(method));
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
    var typeName = type instanceof Class<?> cls ? cls.getSimpleName() : type.getTypeName();
    return qualifier == null ? typeName : "@" + qualifier.getSimpleName() + " " + typeName;
  }

  /**
   * Captures a fully parameterized generic type at the call site by subclassing anonymously with
   * a concrete type argument — reflection recovers it from the anonymous subclass's superclass.
   */
  abstract static class GenericType<T> {

    private final Type type;

    protected GenericType() {
      var superclass = getClass().getGenericSuperclass();
      this.type = ((ParameterizedType) superclass).getActualTypeArguments()[0];
    }
  }
}
