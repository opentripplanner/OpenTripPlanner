package org.opentripplanner.routing.core.vehicle_sharing;

public enum Provider {

    __EMPTY,
    TRAFICAR,
    PANEK,
    JEDEN_SLAD,
    VOZILLA,
    CLICK2GO,
    TAURON,
    NEXTBIKE,
    BLINKEE,
    HIVE,
    ECOSHARE,
    __PUBLIC_TRANSPORT_WARSAW,
    EASYSHARE,
    INNOGY,
    LIME,
    BIRD,
    __PUBLIC_TRANSPORT_WROCLAW,
    __PUBLIC_TRANSPORT_GDANSK,
    HOPCITY,
    __PUBLIC_TRANSPORT_STOPS_WARSAW,
    ITAXI,
    MIIMOVE,
    _4MOBILITY,
    VOLT,
    CITYBEE;

    private static final Provider[] PROVIDERS = Provider.values();

    public static Provider fromId(int id) {
        try {
            return PROVIDERS[id];
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}
