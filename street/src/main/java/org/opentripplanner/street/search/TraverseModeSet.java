package org.opentripplanner.street.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A set of traverse modes -- typically, one non-transit mode (walking, biking, car) and zero or
 * more transit modes (bus, tram, etc).  This class allows efficiently adding or removing modes from
 * a set.
 *
 * @author novalis
 * <p>
 * <p>
 * TODO OTP2 - Replace this with the use of a EnumSet
 */
public class TraverseModeSet implements Cloneable, Serializable {

  private static final int MODE_BICYCLE = 1;

  private static final int MODE_WALK = 2;

  private static final int MODE_CAR = 4;

  private static final int MODE_ALL = MODE_CAR | MODE_WALK | MODE_BICYCLE;

  private byte modes = 0;

  public TraverseModeSet(TraverseMode... modes) {
    for (TraverseMode mode : modes) {
      this.modes |= getMaskForMode(mode);
    }
  }

  public TraverseModeSet(Collection<TraverseMode> modeList) {
    this(modeList.toArray(new TraverseMode[0]));
  }

  /**
   * Returns a mode set containing all modes.
   */
  public static TraverseModeSet allModes() {
    TraverseModeSet modes = new TraverseModeSet();
    modes.modes = MODE_ALL;
    return modes;
  }

  public boolean getBicycle() {
    return (modes & MODE_BICYCLE) != 0;
  }

  public void setBicycle(boolean bicycle) {
    if (bicycle) {
      modes |= MODE_BICYCLE;
    } else {
      modes &= ~MODE_BICYCLE;
    }
  }

  public boolean getWalk() {
    return (modes & MODE_WALK) != 0;
  }

  public void setWalk(boolean walk) {
    if (walk) {
      modes |= MODE_WALK;
    } else {
      modes &= ~MODE_WALK;
    }
  }

  public boolean getCar() {
    return (modes & MODE_CAR) != 0;
  }

  public void setCar(boolean car) {
    if (car) {
      modes |= MODE_CAR;
    } else {
      modes &= ~MODE_CAR;
    }
  }

  public List<TraverseMode> getModes() {
    ArrayList<TraverseMode> modeList = new ArrayList<>();
    for (TraverseMode mode : TraverseMode.values()) {
      if ((modes & getMaskForMode(mode)) != 0) {
        modeList.add(mode);
      }
    }
    return modeList;
  }

  public boolean isValid() {
    return modes != 0;
  }

  public boolean contains(TraverseMode mode) {
    return (modes & getMaskForMode(mode)) != 0;
  }

  /**
   * Clear the mode set so that no modes are included.
   */
  public void clear() {
    modes = 0;
  }

  public int hashCode() {
    return modes;
  }

  public boolean equals(Object other) {
    if (other instanceof TraverseModeSet) {
      return modes == ((TraverseModeSet) other).modes;
    }
    return false;
  }

  @Override
  public TraverseModeSet clone() {
    try {
      return (TraverseModeSet) super.clone();
    } catch (CloneNotSupportedException e) {
      /* this will never happen since our super is the cloneable object */
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    StringBuilder out = new StringBuilder();
    for (TraverseMode mode : TraverseMode.values()) {
      int mask = getMaskForMode(mode);
      if (mask != 0 && (modes & mask) == mask) {
        if (!out.isEmpty()) {
          out.append(", ");
        }
        out.append(mode);
      }
    }
    return "TraverseMode (" + out + ")";
  }

  private int getMaskForMode(TraverseMode mode) {
    return switch (mode) {
      case BICYCLE, SCOOTER -> MODE_BICYCLE;
      case WALK -> MODE_WALK;
      case CAR -> MODE_CAR;
      case FLEX -> 0;
    };
  }
}
