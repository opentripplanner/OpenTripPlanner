/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A set of traverse modes -- typically, one non-transit mode (walking, biking, car) and zero or
 * more transit modes (bus, tram, etc).  This class allows efficiently adding or removing modes
 * from a set.
 * @author novalis
 *
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

    private static final int MODE_TRANSIT = MODE_TRAM | MODE_RAIL | MODE_SUBWAY | MODE_FUNICULAR
            | MODE_GONDOLA | MODE_CABLE_CAR | MODE_BUS | MODE_FERRY | MODE_AIRPLANE;
 
    private static final int MODE_ALL = MODE_TRANSIT | MODE_WALK | MODE_BICYCLE;

    private int modes = 0;

    public TraverseModeSet(String modelist) {
        modes = 0;
        for (String modeStr : modelist.split(",")) {
            if (modeStr.length() == 0) {
                continue;
            }
            setMode(TraverseMode.valueOf(modeStr), true);
        }

    }

    public TraverseModeSet(TraverseMode... modes) {
        for (TraverseMode mode : modes) {
            this.modes |= getMaskForMode(mode);
        }
    }
    
    /**
     * Returns a mode set containing all modes.
     * 
     * @return
     */
    public static TraverseModeSet allModes() {
    	TraverseModeSet modes = new TraverseModeSet();
    	modes.modes = MODE_ALL;
    	return modes;
    }

    private final int getMaskForMode(TraverseMode mode) {
        switch (mode) {
        case BICYCLE:
            return MODE_BICYCLE;
        case WALK:
            return MODE_WALK;
        case CAR:
            return MODE_CAR;
        case BUS:
            return MODE_BUS;
        case TRAM:
            return MODE_TRAM;
        case CABLE_CAR:
            return MODE_CABLE_CAR;
        case GONDOLA:
            return MODE_GONDOLA;
        case FERRY:
            return MODE_FERRY;
        case FUNICULAR:
            return MODE_FUNICULAR;
        case SUBWAY:
            return MODE_SUBWAY;
        case RAIL:
            return MODE_RAIL;
        case AIRPLANE:
            return MODE_AIRPLANE;
        case TRANSIT:
            return MODE_TRANSIT;
        }
        return 0;
    }

    public TraverseModeSet(Collection<TraverseMode> modeList) {
        this(modeList.toArray(new TraverseMode[0]));
    }
    
    public int getMask() {
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

    public boolean getWalk() {
        return (modes & MODE_WALK) != 0;
    }

    public boolean getCar() {
        return (modes & MODE_CAR) != 0;
    }
    
    public boolean getTram() {
        return (modes & MODE_TRAM) != 0;
    }
    
    public boolean getBus() {
        return (modes & MODE_BUS) != 0;
    }
    
    public boolean getGondola() {
        return (modes & MODE_GONDOLA) != 0;
    }
    
    public boolean getFerry() {
        return (modes & MODE_FERRY) != 0;
    }
    
    public boolean getCableCar() {
        return (modes & MODE_CABLE_CAR) != 0;
    }

    public boolean getFunicular() {
        return (modes & MODE_FUNICULAR) != 0;
    }
    
    public boolean getRail() {
        return (modes & MODE_RAIL) != 0;
    }
    
    public boolean getSubway() {
        return (modes & MODE_SUBWAY) != 0;
    }

    public boolean getAirplane() {
        return (modes & MODE_AIRPLANE) != 0;
    }

    public void setBicycle(boolean bicycle) {
        if (bicycle) {
            modes |= MODE_BICYCLE;
        } else {
            modes &= ~MODE_BICYCLE;
        }
    }

    public void setWalk(boolean walk) {
        if (walk) {
            modes |= MODE_WALK;
        } else {
            modes &= ~MODE_WALK;
        }
    }

    public void setCar(boolean car) {
        if (car) {
            modes |= MODE_CAR;
        } else {
            modes &= ~MODE_CAR;
        }
    }
    
    public void setTram(boolean tram) {
        if (tram) {
            modes |= MODE_TRAM;
        } else {
            modes &= ~MODE_TRAM;
        }
    }
    
    public void setBus(boolean bus) {
        if (bus) {
            modes |= MODE_BUS;
        } else {
            modes &= ~MODE_BUS;
        }
    }
   
    public void setFerry(boolean ferry) {
        if (ferry) {
            modes |= MODE_FERRY;
        } else {
            modes &= ~MODE_FERRY;
        }
    }


    public void setCableCar(boolean cableCar) {
        if (cableCar) {
            modes |= MODE_CABLE_CAR;
        } else {
            modes &= ~MODE_CABLE_CAR;
        }
    }

    public void setGondola(boolean gondola) {
        if (gondola) {
            modes |= MODE_GONDOLA;
        } else {
            modes &= ~MODE_GONDOLA;
        }
    }

    public void setFunicular(boolean funicular) {
        if (funicular) {
            modes |= MODE_FUNICULAR;
        } else {
            modes &= ~MODE_FUNICULAR;
        }
    }

    public void setSubway(boolean subway) {
        if (subway) {
            modes |= MODE_SUBWAY;
        } else {
            modes &= ~MODE_SUBWAY;
        }
    }
    
    public void setRail(boolean rail) {
        if (rail) {
            modes |= MODE_RAIL;
        } else {
            modes &= ~MODE_RAIL;
        }
    }

    public void setAirplane(boolean airplane) {
        if (airplane) {
            modes |= MODE_AIRPLANE;
        } else {
            modes &= ~MODE_AIRPLANE;
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
        ArrayList<TraverseMode> modeList = new ArrayList<TraverseMode>();
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

    /** get this traverse mode as a string that can be fed back into the constructor */
    public String getAsStr() {
        String retVal = null;
        for (TraverseMode m : getModes()) {
            if (retVal == null)
                retVal = "";
            else
                retVal += ",";
            retVal += m;
        }
        return retVal;
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
            return modes == ((TraverseModeSet)other).modes;
        }
        return false;
    }

}
