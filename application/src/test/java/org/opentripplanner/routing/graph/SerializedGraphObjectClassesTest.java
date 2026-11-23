package org.opentripplanner.routing.graph;

import static com.google.common.truth.Truth.assertWithMessage;

import com.esotericsoftware.kryo.Kryo;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.graph.kryosupport.KryoBuilder;

/**
 * The graph is loaded with Kryo. Kryo creates an instance with the no-arg constructor if the class
 * has one, and otherwise falls back to Java serialization instantiation, which requires the class
 * to be {@link Serializable}. A class which is neither fails when the graph is loaded, and not when
 * it is built and saved. Such a class is easily added deep in the object graph, in a part not
 * covered by any test data.
 * <p>
 * This test finds every OTP class which can be a part of the serialized graph and verifies that it
 * can be instantiated by Kryo. It reads the compiled classes (ArchUnit) and does not need any
 * test data. The classes are found like this:
 * <ol>
 *   <li>Start with the types of the fields in {@link SerializedGraphObject}.</li>
 *   <li>Follow the types of all fields, including generic type arguments, but skip static and
 *       {@code transient} fields, since these are not serialized.</li>
 *   <li>An interface or a non-final OTP class can hold any of its subclasses, so these are
 *       followed too.</li>
 *   <li>Do not follow into classes with a registered Kryo serializer, nor into JDK classes.</li>
 * </ol>
 * If this test fails, there are three ways to fix it:
 * <ul>
 *   <li>Let the class implement {@link Serializable} (preferred).</li>
 *   <li>Make the field {@code transient}, if the field is not a part of the graph.</li>
 *   <li>Register a Kryo serializer in {@link KryoBuilder}.</li>
 * </ul>
 */
class SerializedGraphObjectClassesTest {

  private static final String OTP_PACKAGE = "org.opentripplanner";

  @Test
  void allSerializedClassesCanBeInstantiatedByKryo() {
    var classes = new ClassFileImporter()
      .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
      .importPackages(OTP_PACKAGE);
    var kryo = KryoBuilder.create();

    var visited = new LinkedHashSet<JavaClass>();
    var referencedBy = new HashMap<JavaClass, JavaClass>();
    var queue = new ArrayDeque<JavaClass>();
    queue.add(classes.get(SerializedGraphObject.class));

    while (!queue.isEmpty()) {
      var type = queue.poll();
      if (!visited.add(type) || isNotFollowed(type, kryo)) {
        continue;
      }
      for (var next : typesInSerializedFields(type)) {
        if (!visited.contains(next)) {
          referencedBy.putIfAbsent(next, type);
          queue.add(next);
        }
      }
    }

    var errors = new ArrayList<String>();
    for (var type : visited) {
      if (isOtpClass(type) && !isNotFollowed(type, kryo) && !canBeInstantiated(type)) {
        errors.add(describeReferencePath(type, referencedBy));
      }
    }

    assertWithMessage(
      "The class(es) below cannot be instantiated when the graph is loaded. Implement " +
        "java.io.Serializable, make the field transient, or register a Kryo serializer in " +
        "KryoBuilder:\n" +
        String.join("\n", errors)
    )
      .that(errors)
      .isEmpty();
  }

  private static List<JavaClass> typesInSerializedFields(JavaClass type) {
    var result = new ArrayList<JavaClass>();
    type
      .getAllFields()
      .stream()
      .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
      .filter(f -> !f.getModifiers().contains(JavaModifier.TRANSIENT))
      .forEach(f -> result.addAll(f.getType().getAllInvolvedRawTypes()));

    // A non-final OTP type may hold any of its subclasses
    for (var t : List.copyOf(result)) {
      if (isOtpClass(t) && !t.getModifiers().contains(JavaModifier.FINAL)) {
        result.addAll(t.getAllSubclasses());
      }
    }
    return result;
  }

  /** Concrete OTP classes only, these are the ones Kryo needs to instantiate. */
  private static boolean canBeInstantiated(JavaClass type) {
    if (
      type.isInterface() ||
      type.isEnum() ||
      type.isAnnotation() ||
      type.isRecord() ||
      type.getModifiers().contains(JavaModifier.ABSTRACT)
    ) {
      return true;
    }
    return (
      type.isAssignableTo(Serializable.class) ||
      type
        .getConstructors()
        .stream()
        .anyMatch(c -> c.getRawParameterTypes().isEmpty())
    );
  }

  private static boolean isNotFollowed(JavaClass type, Kryo kryo) {
    return !isOtpClass(type) || hasRegisteredKryoSerializer(type, kryo);
  }

  private static boolean isOtpClass(JavaClass type) {
    return type.getPackageName().startsWith(OTP_PACKAGE);
  }

  private static boolean hasRegisteredKryoSerializer(JavaClass type, Kryo kryo) {
    return kryo.getClassResolver().getRegistration(type.reflect()) != null;
  }

  private static String describeReferencePath(
    JavaClass type,
    Map<JavaClass, JavaClass> referencedBy
  ) {
    var path = new StringBuilder("  ").append(type.getName());
    var current = type;
    for (int i = 0; i < 8 && referencedBy.get(current) != null; ++i) {
      current = referencedBy.get(current);
      path.append(" <- ").append(current.getSimpleName());
    }
    return path.toString();
  }
}
