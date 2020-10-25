package org.opentripplanner.netex.loader.parser;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Journey_VersionStructure;
import org.rutebanken.netex.model.JourneysInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.Timetable_VersionFrameStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

class TimeTableFrameParser extends NetexParser<Timetable_VersionFrameStructure> {

    private static final Logger LOG = LoggerFactory.getLogger(TimeTableFrameParser.class);

    private final ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById;

    private final Set<DayTypeRefsToServiceIdAdapter> dayTypeRefs = new HashSet<>();

    private final Multimap<String, ServiceJourney> serviceJourneyByPatternId = ArrayListMultimap.create();
    private final Multimap<String, TimetabledPassingTime> passingTimeByStopPointId = ArrayListMultimap.create();

    private final NoticeParser noticeParser = new NoticeParser();


    TimeTableFrameParser(ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById) {
        this.journeyPatternById = journeyPatternById;
    }

    @Override
    void parse(Timetable_VersionFrameStructure frame) {
        parseJourneys(frame.getVehicleJourneys());

        noticeParser.parseNotices(frame.getNotices());
        noticeParser.parseNoticeAssignments(frame.getNoticeAssignments());

        warnOnMissingMapping(LOG, frame.getNetworkView());
        warnOnMissingMapping(LOG, frame.getLineView());
        warnOnMissingMapping(LOG, frame.getOperatorView());
        warnOnMissingMapping(LOG, frame.getAccessibilityAssessment());

        // Keep list sorted alphabetically
        warnOnMissingMapping(LOG, frame.getBookingTimes());
        warnOnMissingMapping(LOG, frame.getVehicleTypeRef());
        warnOnMissingMapping(LOG, frame.getCoupledJourneys());
        warnOnMissingMapping(LOG, frame.getDefaultInterchanges());
        warnOnMissingMapping(LOG, frame.getFlexibleServiceProperties());
        warnOnMissingMapping(LOG, frame.getFrequencyGroups());
        warnOnMissingMapping(LOG, frame.getGroupsOfServices());
        warnOnMissingMapping(LOG, frame.getInterchangeRules());
        warnOnMissingMapping(LOG, frame.getJourneyAccountingRef());
        warnOnMissingMapping(LOG, frame.getJourneyAccountings());
        warnOnMissingMapping(LOG, frame.getJourneyInterchanges());
        warnOnMissingMapping(LOG, frame.getJourneyMeetings());
        warnOnMissingMapping(LOG, frame.getJourneyPartCouples());
        warnOnMissingMapping(LOG, frame.getNotices());
        warnOnMissingMapping(LOG, frame.getNoticeAssignments());
        warnOnMissingMapping(LOG, frame.getServiceCalendarFrameRef());
        warnOnMissingMapping(LOG, frame.getServiceFacilitySets());
        warnOnMissingMapping(LOG, frame.getTimeDemandTypes());
        warnOnMissingMapping(LOG, frame.getTimeDemandTypeAssignments());
        warnOnMissingMapping(LOG, frame.getTimingLinkGroups());
        warnOnMissingMapping(LOG, frame.getTrainNumbers());
        warnOnMissingMapping(LOG, frame.getTypesOfService());
        warnOnMissingMapping(LOG, frame.getVehicleTypes());

        verifyCommonUnusedPropertiesIsNotSet(LOG, frame);
    }

    @Override
    void setResultOnIndex(NetexEntityIndex netexIndex) {
        netexIndex.dayTypeRefs.addAll(dayTypeRefs);
        netexIndex.serviceJourneyByPatternId.addAll(serviceJourneyByPatternId);
        netexIndex.passingTimeByStopPointId.addAll(passingTimeByStopPointId);

        noticeParser.setResultOnIndex(netexIndex);
    }

    private void parseJourneys(JourneysInFrame_RelStructure element) {

        for (Journey_VersionStructure it : element.getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()) {
            if (it instanceof ServiceJourney) {
                parseServiceJourney((ServiceJourney)it);
            }
            else {
                warnOnMissingMapping(LOG, it);
            }
        }
    }

    private void parseServiceJourney(ServiceJourney sj) {
        DayTypeRefsToServiceIdAdapter serviceAdapter = DayTypeRefsToServiceIdAdapter.create(sj.getDayTypes());
        if(serviceAdapter == null) {
            LOG.warn("Skipping ServiceJourney with empty dayTypes. Service Journey id : {}", sj.getId());
            return;
        }

        dayTypeRefs.add(serviceAdapter);

        String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

        JourneyPattern journeyPattern = journeyPatternById.lookup(journeyPatternId);

        if (journeyPattern != null) {
            int nStopPointsInJourneyPattern = journeyPattern
                    .getPointsInSequence()
                    .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                    .size();
            int nTimetablePassingTimes = sj.getPassingTimes().getTimetabledPassingTime().size();

            // TODO OTP2 - This check belongs to the mapping or as a separate validation
            //           - step. The problem is that we do not want to relay on the
            //           - the order in witch elements are parsed/loaded; hence `journeyPattern`
            //           - is know at this point.
            if (nStopPointsInJourneyPattern == nTimetablePassingTimes) {
                serviceJourneyByPatternId.put(journeyPatternId, sj);

                for (TimetabledPassingTime it : sj.getPassingTimes().getTimetabledPassingTime()) {
                    passingTimeByStopPointId.put(it.getPointInJourneyPatternRef().getValue().getRef(), it);
                }
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
