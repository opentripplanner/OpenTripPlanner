package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimeTableFrameParser {

    private static final Logger LOG = LoggerFactory.getLogger(TimeTableFrameParser.class);

    private final HierarchicalMapById<JourneyPattern> journeyPatternById;

    private final Set<DayTypeRefsToServiceIdAdapter> dayTypeRefs = new HashSet<>();

    private final HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId = new HierarchicalMultimap<>();

    public TimeTableFrameParser(HierarchicalMapById<JourneyPattern> journeyPatternById) {
        this.journeyPatternById = journeyPatternById;
    }

    public void parse(TimetableFrame timetableFrame) {
        JourneysInFrame_RelStructure vehicleJourneys = timetableFrame.getVehicleJourneys();
        List<Journey_VersionStructure> datedServiceJourneyOrDeadRunOrServiceJourney = vehicleJourneys
                .getDatedServiceJourneyOrDeadRunOrServiceJourney();
        for (Journey_VersionStructure jStructure : datedServiceJourneyOrDeadRunOrServiceJourney) {
            if (jStructure instanceof ServiceJourney) {
                ServiceJourney sj = (ServiceJourney) jStructure;

                dayTypeRefs.add(new DayTypeRefsToServiceIdAdapter(sj.getDayTypes()));

                String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

                JourneyPattern journeyPattern = journeyPatternById
                        .lookup(journeyPatternId);

                if (journeyPattern != null) {
                    if (journeyPattern.getPointsInSequence().
                            getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                            .size() == sj.getPassingTimes().getTimetabledPassingTime().size()) {

                        serviceJourneyByPatternId.add(journeyPatternId, sj);
                    } else {
                        LOG.warn(
                                "Mismatch between ServiceJourney and JourneyPattern. ServiceJourney will be skipped. - "
                                        + sj.getId());
                    }
                } else {
                    LOG.warn("JourneyPattern not found. " + journeyPatternId);
                }
            }
        }
    }

    public Set<DayTypeRefsToServiceIdAdapter> getDayTypeRefs() {
        return dayTypeRefs;
    }

    public HierarchicalMultimap<String, ServiceJourney> getServiceJourneyByPatternId() {
        return serviceJourneyByPatternId;
    }
}
