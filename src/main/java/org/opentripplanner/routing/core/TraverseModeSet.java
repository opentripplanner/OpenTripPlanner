package org.opentripplanner.routing.core;

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

  private static final long serialVersionUID = -1640048158419762255L;

  private static final int MODE_BICYCLE = 1;

  private static final int MODE_WALK = 2;

  private static final int MODE_CAR = 4;

  private static final int MODE_BUS = 16;

  private static final int MODE_TRAM = 32;

  private static final int MODE_SUBWAY = 64;

  private static final int MODE_RAIL = 128;

  private static final int MODE_FERRY = 256;

  private static final int MODE_CABLE_CAR = 512;

  private static final int MODE_GONDOLA = 1024;

  private static final int MODE_FUNICULAR = 2048;

  private static final int MODE_AIRPLANE = 4096;

  private static final int MODE_TROLLEYBUS = 8192;

  private static final int MODE_MONORAIL = 16384;

  private static final int MODE_TRANSIT =
    MODE_TRAM |
    MODE_RAIL |
    MODE_SUBWAY |
    MODE_FUNICULAR |
    MODE_GONDOLA |
    MODE_CABLE_CAR |
    MODE_BUS |
    MODE_FERRY |
    MODE_AIRPLANE |
    MODE_TROLLEYBUS |
    MODE_MONORAIL;

  private static final int MODE_ALL = MODE_TRANSIT | MODE_WALK | MODE_BICYCLE;

  private int modes = 0;

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

  public void setMode(TraverseMode mode, boolean value) {
    int mask = getMaskForMode(mode);
    if (value) {
      modes |= mask;
    } else {
      modes &= ~mask;
    }
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

  public boolean getTram() {
    return (modes & MODE_TRAM) != 0;
  }

  public void setTram(boolean tram) {
    if (tram) {
      modes |= MODE_TRAM;
    } else {
      modes &= ~MODE_TRAM;
    }
  }

  public boolean getBus() {
    return (modes & MODE_BUS) != 0;
  }

  public void setBus(boolean bus) {
    if (bus) {
      modes |= MODE_BUS;
    } else {
      modes &= ~MODE_BUS;
    }
  }

  public boolean getGondola() {
    return (modes & MODE_GONDOLA) != 0;
  }

  public void setGondola(boolean gondola) {
    if (gondola) {
      modes |= MODE_GONDOLA;
    } else {
      modes &= ~MODE_GONDOLA;
    }
  }

  public boolean getFerry() {
    return (modes & MODE_FERRY) != 0;
  }

  public void setFerry(boolean ferry) {
    if (ferry) {
      modes |= MODE_FERRY;
    } else {
      modes &= ~MODE_FERRY;
    }
  }

  public boolean getCableCar() {
    return (modes & MODE_CABLE_CAR) != 0;
  }

  public void setCableCar(boolean cableCar) {
    if (cableCar) {
      modes |= MODE_CABLE_CAR;
    } else {
      modes &= ~MODE_CABLE_CAR;
    }
  }

  public boolean getFunicular() {
    return (modes & MODE_FUNICULAR) != 0;
  }

  public void setFunicular(boolean funicular) {
    if (funicular) {
      modes |= MODE_FUNICULAR;
    } else {
      modes &= ~MODE_FUNICULAR;
    }
  }

  public boolean getRail() {
    return (modes & MODE_RAIL) != 0;
  }

  public void setRail(boolean rail) {
    if (rail) {
      modes |= MODE_RAIL;
    } else {
      modes &= ~MODE_RAIL;
    }
  }

  public boolean getTrolleyBus() {
    return (modes & MODE_TROLLEYBUS) != 0;
  }

  public boolean geMonorail() {
    return (modes & MODE_MONORAIL) != 0;
  }

  public boolean getSubway() {
    return (modes & MODE_SUBWAY) != 0;
  }

  public void setSubway(boolean subway) {
    if (subway) {
      modes |= MODE_SUBWAY;
    } else {
      modes &= ~MODE_SUBWAY;
    }
  }

  public boolean getAirplane() {
    return (modes & MODE_AIRPLANE) != 0;
  }

  public void setAirplane(boolean airplane) {
    if (airplane) {
      modes |= MODE_AIRPLANE;
    } else {
      modes &= ~MODE_AIRPLANE;
    }
  }

  public void setTrolleybus(boolean trolleybus) {
    if (trolleybus) {
      modes |= MODE_TROLLEYBUS;
    } else {
      modes &= ~MODE_TROLLEYBUS;
    }
  }

  public void setMonorail(boolean monorail) {
    if (monorail) {
      modes |= MODE_MONORAIL;
    } else {
      modes &= ~MODE_MONORAIL;
    }
  }

  /** Returns true if the trip may use some transit mode */
  public boolean isTransit() {
    return (modes & (MODE_TRANSIT)) != 0;
  }

  public void setTransit(boolean transit) {
    if (transit) {
      modes |= MODE_TRANSIT;
    } else {
      modes &= ~MODE_TRANSIT;
    }
  }

  /** Returns a TraverseModeSet containing only the non-transit modes set. */
  public TraverseModeSet getNonTransitSet() {
    TraverseModeSet retval = new TraverseModeSet();
    retval.modes = modes;
    retval.setTransit(false);
    return retval;
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

  public boolean get(int modeMask) {
    return (modes & modeMask) != 0;
  }

  /** get this traverse mode as a string that can be fed back into the constructor */
  public String getAsStr() {
    String retVal = null;
    for (TraverseMode m : getModes()) {
      if (retVal == null) {
        retVal = "";
      } else {
        retVal += ",";
      }
      retVal += m;
    }
    return retVal;
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
        if (out.length() != 0) {
          out.append(", ");
        }
        out.append(mode);
      }
    }
    return "TraverseMode (" + out + ")";
  }

  private int getMaskForMode(TraverseMode mode) {
    return switch (mode) {
      case BICYCLE -> MODE_BICYCLE;
      case WALK -> MODE_WALK;
      case CAR -> MODE_CAR;
      case BUS -> MODE_BUS;
      case TRAM -> MODE_TRAM;
      case CABLE_CAR -> MODE_CABLE_CAR;
      case GONDOLA -> MODE_GONDOLA;
      case FERRY -> MODE_FERRY;
      case FUNICULAR -> MODE_FUNICULAR;
      case SUBWAY -> MODE_SUBWAY;
      case RAIL -> MODE_RAIL;
      case TROLLEYBUS -> MODE_TROLLEYBUS;
      case MONORAIL -> MODE_MONORAIL;
      case AIRPLANE -> MODE_AIRPLANE;
      case TRANSIT -> MODE_TRANSIT;
      default -> 0;
    };
  }
}
