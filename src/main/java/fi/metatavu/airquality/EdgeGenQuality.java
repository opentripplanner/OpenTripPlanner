package fi.metatavu.airquality;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class for air quality edge updater
 */
public class EdgeGenQuality {

  private final Map<Integer, float[]> generticProperyValues;

  /**
   * Constructor
   */
  public EdgeGenQuality() {
    generticProperyValues = new HashMap<>();
  }

  /**
   * Adds a property value for given time
   *
   * @param time time
   * @param propertyValue propertyValue
   */
  public void addPropertyValueSample(int time, float propertyValue) {
    float[] existing = generticProperyValues.get(time);
    if (existing == null) {
      generticProperyValues.put(time, new float[] { propertyValue });
    } else {
      float[] updated = new float[existing.length + 1];
      System.arraycopy(existing, 0, updated, 0, existing.length);
      updated[existing.length] = propertyValue;
      generticProperyValues.put(time, updated);
    }
  }

  /**
   * Returns property value average for given time
   *
   * @param time time
   * @return property value
   */
  public float getPropertyValue(int time) {
    return getAverage(getPropertyValuesInTime(time));
  }

  /**
   * Returns property value averages given times
   *
   * @param times times
   * @return pollutantValues
   */
  public float[] getPeroptyValuesAverages(int times) {
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
   * @return average
   */
  private float getAverage(float[] values) {
    if (values == null) {
      return 0;
    }

    float result = 0f;

    for (float value : values) {
      result += value;
    }

    return result / values.length;
  }

  /**
   * Returns array of property indices in time
   *
   * @param time time
   * @return array of property value
   */
  private float[] getPropertyValuesInTime(int time) {
    return generticProperyValues.get(time);
  }

}
