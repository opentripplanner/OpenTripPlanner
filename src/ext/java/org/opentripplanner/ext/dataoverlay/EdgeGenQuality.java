package org.opentripplanner.ext.dataoverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A helper class for generic grid data edge updater
 *
 * @author Katja Danilova
 */
class EdgeGenQuality<E extends Number> {

  private final Map<Integer, List<E>> gridDataVariableValues;

  /**
   * Constructor
   */
  EdgeGenQuality() {
    gridDataVariableValues = new HashMap<>();
  }

  /**
   * Adds a property value for given time
   *
   * @param time          time
   * @param propertyValue propertyValue
   */
  void addPropertyValueSample(int time, E propertyValue) {
    List<E> existing = gridDataVariableValues.get(time);
    if (existing == null) {
      gridDataVariableValues.put(time, List.of(propertyValue));
    } else {
      List<E> updated = new ArrayList<>(existing.size() + 1);
      updated.addAll(existing);
      updated.add(existing.size(), propertyValue);
      gridDataVariableValues.put(time, updated);
    }
  }

  /**
   * Returns property value average for given time
   *
   * @param time time
   * @return property value
   */
  float getPropertyValue(int time) {
    return getAverage(getPropertyValuesInTime(time));
  }

  /**
   * Returns property value averages for given times
   *
   * @param times times
   * @return pollutantValues
   */
  float[] getPropertyValueAverage(int times) {
    float[] result = new float[times];

    for (int time = 0; time < times; time++) {
      result[time] = getPropertyValue(time);
    }

    return result;
  }

  /**
   * Returns average for values
   *
   * @param values values
   * @return average values
   */
  private float getAverage(List<E> values) {
    if (values == null) {
      return 0;
    }

    return calculateAverage(values);
  }

  /**
   * Calculates average float value for list of values
   *
   * @param values list of values
   * @return average value for the list
   */
  private float calculateAverage(List<E> values) {
    int len = values.size();
    if (values.get(0) instanceof Integer) {
      Integer total = 0;

      for (E value : values) {
        total += (Integer) value;
      }
      return (float) total / len;
    } else if (values.get(0) instanceof Float) {
      float sum = 0f;

      for (E value : values) {
        sum += (Float) value;
      }

      return sum / len;
    } else if (values.get(0) instanceof Double) {
      double sum = 0;

      for (E value : values) {
        sum += (double) value;
      }
      return (float) sum / len;
    } else if (values.get(0) instanceof Long) {
      Long total = 0L;

      for (E value : values) {
        total += (Long) value;
      }
      return (float) total / len;
    } else {
      throw new UnsupportedOperationException("Unrecognizable format of " + values.get(0));
    }
  }

  /**
   * Returns array of property indices in time
   *
   * @param time time
   * @return array of property values
   */
  private List<E> getPropertyValuesInTime(int time) {
    return gridDataVariableValues.get(time);
  }
}
