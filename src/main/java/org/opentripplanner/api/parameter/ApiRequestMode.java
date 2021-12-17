package org.opentripplanner.api.parameter;

import java.util.Set;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.model.TransitMode;

import java.util.Collection;
import java.util.Collections;

public enum ApiRequestMode {
    WALK(),
    BICYCLE(),
    SCOOTER(),
    CAR(),
    TRAM(AllowedTransitMode.fromMainModeEnum(TransitMode.TRAM)),
    SUBWAY(AllowedTransitMode.fromMainModeEnum(TransitMode.SUBWAY)),
    RAIL(AllowedTransitMode.fromMainModeEnum(TransitMode.RAIL)),
    BUS(Set.of(
            AllowedTransitMode.fromMainModeEnum(TransitMode.BUS),
            AllowedTransitMode.fromMainModeEnum(TransitMode.COACH)
    )),
    FERRY(AllowedTransitMode.fromMainModeEnum(TransitMode.FERRY)),
    CABLE_CAR(AllowedTransitMode.fromMainModeEnum(TransitMode.CABLE_CAR)),
    GONDOLA(AllowedTransitMode.fromMainModeEnum(TransitMode.GONDOLA)),
    FUNICULAR(AllowedTransitMode.fromMainModeEnum(TransitMode.FUNICULAR)),
    TRANSIT(AllowedTransitMode.getAllTransitModes()),
    AIRPLANE(AllowedTransitMode.fromMainModeEnum(TransitMode.AIRPLANE)),
    TROLLEYBUS(AllowedTransitMode.fromMainModeEnum(TransitMode.TROLLEYBUS)),
    MONORAIL(AllowedTransitMode.fromMainModeEnum(TransitMode.MONORAIL)),
    FLEX();

    private final Set<AllowedTransitMode> transitModes;

    ApiRequestMode(Set<AllowedTransitMode> transitModes) {
        this.transitModes = transitModes;
    }

    ApiRequestMode(AllowedTransitMode transitMode) {
        this.transitModes = Set.of(transitMode);
    }

    ApiRequestMode() {
        this.transitModes = Collections.emptySet();
    }

    public Collection<AllowedTransitMode> getTransitModes() {
        return transitModes;
    }
}
