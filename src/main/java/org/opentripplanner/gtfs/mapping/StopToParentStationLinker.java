package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ParentStationNotFound;
import org.opentripplanner.model.*;
import org.opentripplanner.model.impl.EntityById;

import java.util.HashMap;
import java.util.Map;

/**
 * Links child stops with parent stations by adding bidirectional object references.
 */
class StopToParentStationLinker {
    private final EntityById<Station> otpStations = new EntityById<>();
    private final EntityById<StationElement> otpStationElements = new EntityById<>();
    private final EntityById<BoardingArea> boardingAreas = new EntityById<>();

    private final Map<StationElement, FeedScopedId> stationElementsToStations = new HashMap<>();
    private final Map<BoardingArea, FeedScopedId> boardingAreasToStops = new HashMap<>();

    private final DataImportIssueStore issueStore;

    StopToParentStationLinker(DataImportIssueStore issueStore) {
        this.issueStore = issueStore;
    }

    void addStation(Station station) {
        otpStations.add(station);
    }

    void addStationElement(StationElement stationElement, String stationId) {
        otpStationElements.add(stationElement);
        if (stationId != null) {
            stationElementsToStations.put(
                stationElement,
                new FeedScopedId(stationElement.getId().getFeedId(), stationId)
            );
        }
    }

    void addBoardingArea(BoardingArea boardingArea, String stopId) {
        boardingAreas.add(boardingArea);
        boardingAreasToStops.put(
            boardingArea,
            new FeedScopedId(boardingArea.getId().getFeedId(), stopId)
        );
    }

    void link() {
        for (Map.Entry<StationElement, FeedScopedId> entry : stationElementsToStations.entrySet()) {
            StationElement stationElement = entry.getKey();
            FeedScopedId stationId = entry.getValue();
            Station otpStation = otpStations.get(stationId);
            if (otpStation == null) {
                issueStore.add(new ParentStationNotFound(stationElement, stationId.getId()));
            } else {
                stationElement.setParentStation(otpStation);
                if (stationElement instanceof Stop) {
                    otpStation.addChildStop((Stop) stationElement);
                }
            }
        }

        for (Map.Entry<BoardingArea, FeedScopedId> entry : boardingAreasToStops.entrySet()) {
            BoardingArea boardingArea = entry.getKey();
            FeedScopedId stopId = entry.getValue();
            StationElement otpStop = otpStationElements.get(stopId);
            if (!(otpStop instanceof Stop)) {
                issueStore.add(new ParentStationNotFound(boardingArea, stopId.getId()));
            } else {
                boardingArea.setParentStop((Stop) otpStop);
                ((Stop) otpStop).addBoardingArea(boardingArea);
                boardingArea.setParentStation(otpStop.getParentStation());
            }
        }
    }
}
