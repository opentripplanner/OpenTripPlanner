package org.opentripplanner.util;

import java.lang.reflect.Constructor;
import org.opentripplanner.annotation.Component;
import org.opentripplanner.annotation.ServiceType;

public class ConstructorDescriptor {

  Constructor constructor;
  Class<?> initialClass;
  String key;
  ServiceType type;

  public Class<?> getInitialClass() {
    return initialClass;
  }

  public String getKey() {
    return key;
  }

  public ServiceType getType() {
    return type;
  }

  public static ConstructorDescriptor getConstructorDescriptor(Class<?> clazz) {
    Component annotation = clazz.getAnnotation(Component.class);
    if (annotation != null) {
      try {
        ConstructorDescriptor descriptor = new ConstructorDescriptor();
        descriptor.key = annotation.key();
        descriptor.type = annotation.type();
        if (annotation.init() != Void.class) {
          descriptor.initialClass = annotation.init();
          descriptor.constructor = clazz.getConstructor(descriptor.initialClass);
        } else {
          descriptor.constructor = clazz.getConstructor(null);
        }
        return descriptor;
      } catch (NoSuchMethodException e) {
      }
    }
    return null;
  }

  public <T> T newInstance(Object initParam) {
    try {
      if (initialClass != null) {
        return (T) constructor.newInstance(initialClass.cast(initParam));
      } else {
        return (T) constructor.newInstance();
      }
    } catch (Exception e) {

    }
    return null;
  }
}
