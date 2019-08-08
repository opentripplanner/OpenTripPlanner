package org.opentripplanner.netex.loader.parser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.util.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Journey_VersionStructure;
import org.rutebanken.netex.model.JourneysInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.Timetable_VersionFrameStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

class TimeTableFrameParser extends NetexParser<Timetable_VersionFrameStructure> {

    private static final Logger LOG = LoggerFactory.getLogger(TimeTableFrameParser.class);

    private final ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById;

    private final Set<DayTypeRefsToServiceIdAdapter> dayTypeRefs = new HashSet<>();

    private final Multimap<String, ServiceJourney> serviceJourneyByPatternId = ArrayListMultimap.create();

    TimeTableFrameParser(ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById) {
        this.journeyPatternById = journeyPatternById;
    }

    @Override
    void parse(Timetable_VersionFrameStructure frame) {
        parseJourneys(frame.getVehicleJourneys());

        logUnknownElement(LOG, frame.getNetworkView());
        logUnknownElement(LOG, frame.getLineView());
        logUnknownElement(LOG, frame.getOperatorView());
        logUnknownElement(LOG, frame.getAccessibilityAssessment());

        // Keep list sorted alphabetically
        logUnknownElement(LOG, frame.getBookingTimes());
        logUnknownElement(LOG, frame.getVehicleTypeRef());
        logUnknownElement(LOG, frame.getCoupledJourneys());
        logUnknownElement(LOG, frame.getDefaultInterchanges());
        logUnknownElement(LOG, frame.getFlexibleServiceProperties());
        logUnknownElement(LOG, frame.getFrequencyGroups());
        logUnknownElement(LOG, frame.getGroupsOfServices());
        logUnknownElement(LOG, frame.getInterchangeRules());
        logUnknownElement(LOG, frame.getJourneyAccountingRef());
        logUnknownElement(LOG, frame.getJourneyAccountings());
        logUnknownElement(LOG, frame.getJourneyInterchanges());
        logUnknownElement(LOG, frame.getJourneyMeetings());
        logUnknownElement(LOG, frame.getJourneyPartCouples());
        logUnknownElement(LOG, frame.getNotices());
        logUnknownElement(LOG, frame.getNoticeAssignments());
        logUnknownElement(LOG, frame.getServiceCalendarFrameRef());
        logUnknownElement(LOG, frame.getServiceFacilitySets());
        logUnknownElement(LOG, frame.getTimeDemandTypes());
        logUnknownElement(LOG, frame.getTimeDemandTypeAssignments());
        logUnknownElement(LOG, frame.getTimingLinkGroups());
        logUnknownElement(LOG, frame.getTrainNumbers());
        logUnknownElement(LOG, frame.getTypesOfService());
        logUnknownElement(LOG, frame.getVehicleTypes());

        checkCommonProperties(LOG, frame);
    }

    @Override
    void setResultOnIndex(NetexImportDataIndex netexIndex) {
        netexIndex.serviceJourneyByPatternId.addAll(serviceJourneyByPatternId);
        netexIndex.dayTypeRefs.addAll(dayTypeRefs);
    }

    private void parseJourneys(JourneysInFrame_RelStructure element) {

        for (Journey_VersionStructure it : element.getDatedServiceJourneyOrDeadRunOrServiceJourney()) {
            if (it instanceof ServiceJourney) {
                parseServiceJourney((ServiceJourney)it);
            }
            else {
                logUnknownObject(LOG, it);
            }
        }
    }

    private void parseServiceJourney(ServiceJourney sj) {
        dayTypeRefs.add(new DayTypeRefsToServiceIdAdapter(sj.getDayTypes()));

        String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

        // TODO OTP2 - This check belongs to the mapping or as a separate validation
        // TODO OTP2 - step. The problem is that we do not want to relay on the
        // TODO OTP2 - the order in witch elements are loaded.
        JourneyPattern journeyPattern = journeyPatternById.lookup(journeyPatternId);

        if (journeyPattern != null) {
            if (journeyPattern.getPointsInSequence().
                    getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                    .size() == sj.getPassingTimes().getTimetabledPassingTime().size()) {

                serviceJourneyByPatternId.put(journeyPatternId, sj);
            } else {
                LOG.warn(
                        "Mismatch between ServiceJourney and JourneyPattern. " +
                        "ServiceJourney will be skipped. - " + sj.getId()
                );
            }
        } else {
            LOG.warn("JourneyPattern not found. " + journeyPatternId);
        }
    }
}
