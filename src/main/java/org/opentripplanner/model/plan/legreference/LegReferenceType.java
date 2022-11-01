package org.opentripplanner.model.plan.legreference;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;

/**
 * Enum for different types of LegReferences
 */
enum LegReferenceType {
  SCHEDULED_TRANSIT_LEG_V1(
    ScheduledTransitLegReference.class,
    LegReferenceSerializer::writeScheduledTransitLeg,
    LegReferenceSerializer::readScheduledTransitLeg
  );

  private final Class<? extends LegReference> legReferenceClass;

  private final Writer<LegReference> serializer;
  private final Reader<? extends LegReference> deserializer;

  LegReferenceType(
    Class<? extends LegReference> legReferenceClass,
    Writer<LegReference> serializer,
    Reader<? extends LegReference> deserializer
  ) {
    this.legReferenceClass = legReferenceClass;
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  static LegReferenceType forClass(Class<? extends LegReference> legReferenceClass) {
    for (var type : LegReferenceType.values()) {
      if (type.legReferenceClass.equals(legReferenceClass)) {
        return type;
      }
    }
    return null;
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
    T read(ObjectInputStream in) throws IOException, ParseException;
  }
}
