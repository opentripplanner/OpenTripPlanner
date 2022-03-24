package org.opentripplanner.api.parameter;

import static org.opentripplanner.model.modes.AllowedTransitMode.fromMainModeEnum;

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
    TRAM(fromMainModeEnum(TransitMode.TRAM)),
    SUBWAY(fromMainModeEnum(TransitMode.SUBWAY)),
    RAIL(fromMainModeEnum(TransitMode.RAIL)),
    BUS(Set.of(
            fromMainModeEnum(TransitMode.BUS),
            fromMainModeEnum(TransitMode.COACH)
    )),
    FERRY(fromMainModeEnum(TransitMode.FERRY)),
    CABLE_CAR(fromMainModeEnum(TransitMode.CABLE_CAR)),
    GONDOLA(fromMainModeEnum(TransitMode.GONDOLA)),
    FUNICULAR(fromMainModeEnum(TransitMode.FUNICULAR)),
    TRANSIT(AllowedTransitMode.getAllTransitModes()),
    AIRPLANE(fromMainModeEnum(TransitMode.AIRPLANE)),
    TROLLEYBUS(fromMainModeEnum(TransitMode.TROLLEYBUS)),
    MONORAIL(fromMainModeEnum(TransitMode.MONORAIL)),
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
