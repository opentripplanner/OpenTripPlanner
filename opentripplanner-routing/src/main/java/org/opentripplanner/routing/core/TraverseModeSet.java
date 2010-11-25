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

import java.util.ArrayList;
import java.util.List;

public class TraverseModeSet implements Cloneable {

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

    private static final int MODE_TRAINISH = MODE_TRAM | MODE_RAIL | MODE_SUBWAY | MODE_FUNICULAR | MODE_GONDOLA;

    private static final int MODE_BUSISH = MODE_CABLE_CAR | MODE_BUS;

    private static final int MODE_TRANSIT = MODE_TRAINISH | MODE_BUSISH | MODE_FERRY;

    private int modes = 0;

    public TraverseModeSet(String modelist) {
        modes = 0;
        for (String modeStr : modelist.split(",")) {
            setMode(TraverseMode.valueOf(modeStr), true);
        }

    }

    public TraverseModeSet(TraverseMode... modes) {
        for (TraverseMode mode : modes) {
            this.modes |= getMaskForMode(mode);
        }
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
        case TRAINISH:
            return MODE_TRAINISH;
        case BUSISH:
            return MODE_BUSISH;
        case TRANSIT:
            return MODE_TRANSIT;
        }
        return 0;
    }

    public TraverseModeSet(List<TraverseMode> modeList) {
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
    
    public boolean getTrainish() {
        return (modes & MODE_TRAINISH) != 0;
    }
    
    public boolean getBusish() {
        return (modes & MODE_BUSISH) != 0;
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

    public void setTrainish(boolean trainish) {
        if (trainish) {
            modes |= MODE_TRAINISH;
        } else {
            modes &= ~MODE_TRAINISH;
        }
    }
    
    public void setBus(boolean bus) {
        if (bus) {
            modes |= MODE_BUS;
        } else {
            modes &= ~MODE_BUS;
        }
    }

    public void setBusish(boolean busish) {
        if (busish) {
            modes |= MODE_BUSISH;
        } else {
            modes &= ~MODE_BUSISH;
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

    /** Returns true if any the trip may use some transit mode */
    public boolean getTransit() {
        return (modes & (MODE_TRANSIT)) != 0;
    }

    public void setTransit(boolean transit) {
        if (transit) {
            modes |= MODE_TRANSIT;
        } else {
            modes &= ~MODE_TRANSIT;
        }
    }
    
    /** Returns true if any the trip may use some train-like (train, subway, tram) mode */
    public boolean getTraininsh() {
        return (modes & (MODE_TRAINISH)) != 0;
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
        String out = "";
        for (TraverseMode mode : TraverseMode.values()) {
            if ((modes & getMaskForMode(mode)) != 0) {
                if (out != "") {
                    out += ", ";
                }
                out += mode;
            }
        }
        return "TraverseMode (" + out + ")";
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

    public TraverseMode getNonTransitMode() {
        if (contains(TraverseMode.CAR)) {
            return TraverseMode.CAR;
        }
        if (contains(TraverseMode.BICYCLE)) {
            return TraverseMode.BICYCLE;
        }
        if (contains(TraverseMode.WALK)) {
            return TraverseMode.WALK;
        }
        return null;
    }

}
