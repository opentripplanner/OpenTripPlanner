package org.opentripplanner.model.plan.legreference;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Optional;

/**
 * Enum for different types of LegReferences
 */
enum LegReferenceType {
  SCHEDULED_TRANSIT_LEG_V1(
    1,
    ScheduledTransitLegReference.class,
    LegReferenceSerializer::writeScheduledTransitLegV1,
    LegReferenceSerializer::readScheduledTransitLegV1
  ),

  SCHEDULED_TRANSIT_LEG_V2(
    2,
    ScheduledTransitLegReference.class,
    LegReferenceSerializer::writeScheduledTransitLegV2,
    LegReferenceSerializer::readScheduledTransitLegV2
  );

  private final int version;
  private final Class<? extends LegReference> legReferenceClass;

  private final Writer<LegReference> serializer;
  private final Reader<? extends LegReference> deserializer;

  LegReferenceType(
    int version,
    Class<? extends LegReference> legReferenceClass,
    Writer<LegReference> serializer,
    Reader<? extends LegReference> deserializer
  ) {
    this.version = version;
    this.legReferenceClass = legReferenceClass;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  /**
   * Return the latest LegReferenceType version for a given leg reference class.
   */
  static LegReferenceType forClass(Class<? extends LegReference> legReferenceClass) {
    Optional<LegReferenceType> latestVersion = Arrays
      .stream(LegReferenceType.values())
      .filter(legReferenceType -> legReferenceType.legReferenceClass.equals(legReferenceClass))
      .reduce((legReferenceType, other) -> {
        if (legReferenceType.version > other.version) {
          return legReferenceType;
        }
        return other;
      });

    return latestVersion.orElse(null);
  }

  Writer<LegReference> getSerializer() {
    return serializer;
  }

  Reader<? extends LegReference> getDeserializer() {
    return deserializer;
  }

  @FunctionalInterface
  interface Writer<T> {
    void write(T t, ObjectOutputStream out) throws IOException;
  }

  @FunctionalInterface
  interface Reader<T> {
    T read(ObjectInputStream in) throws IOException;
  }
}
