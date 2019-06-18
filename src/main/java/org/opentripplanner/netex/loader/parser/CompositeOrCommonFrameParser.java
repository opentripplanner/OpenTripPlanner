package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.List;

public class CompositeOrCommonFrameParser {

    private final NetexImportDataIndex netexIndex;

    public CompositeOrCommonFrameParser(NetexImportDataIndex netexIndex) {
        this.netexIndex = netexIndex;
    }

    public void parse(JAXBElement<? extends Common_VersionFrameStructure> frame) {
        if (frame.getValue() instanceof CompositeFrame) {
            CompositeFrame cf = (CompositeFrame)frame.getValue();
            VersionFrameDefaultsStructure frameDefaults = cf.getFrameDefaults();
            String timeZone = "GMT";
            if (frameDefaults != null && frameDefaults.getDefaultLocale() != null
                    && frameDefaults.getDefaultLocale().getTimeZone() != null) {
                timeZone = frameDefaults.getDefaultLocale().getTimeZone();
            }

            netexIndex.timeZone.set(timeZone);

            List<JAXBElement<? extends Common_VersionFrameStructure>> commonFrames = cf
                    .getFrames().getCommonFrame();
            for (JAXBElement commonFrame : commonFrames) {
                loadResourceFrames(commonFrame);
                loadServiceCalendarFrames(commonFrame);
                loadTimeTableFrames(commonFrame);
                loadServiceFrames(commonFrame);
            }
        } else if (frame.getValue() instanceof SiteFrame) {
            loadSiteFrames(frame);
        }
    }

    private void loadSiteFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof SiteFrame) {
            SiteFrameParser siteFrameParser = new SiteFrameParser();
            siteFrameParser.parse((SiteFrame)commonFrame.getValue());
            netexIndex.stopPlaceById.addAll(siteFrameParser.getStopPlaceById());
            netexIndex.quayById.addAll(siteFrameParser.getQuayById());
        }
    }

    private void loadServiceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrameParser serviceFrameParser = new ServiceFrameParser(
                    netexIndex.quayById,
                    netexIndex.authoritiesById,
                    netexIndex.networkById,
                    netexIndex.groupOfLinesById
            );
            serviceFrameParser.parse((ServiceFrame) commonFrame.getValue());
            netexIndex.routeById.addAll(serviceFrameParser.getRouteById());
            netexIndex.authoritiesByNetworkId.addAll(serviceFrameParser.getAuthorityByNetworkId());
            netexIndex.lineById.addAll(serviceFrameParser.getLineById());
            netexIndex.networkByLineId.addAll(serviceFrameParser.getNetworkByLineId());
            netexIndex.groupOfLinesByLineId.addAll(serviceFrameParser.getGroupOfLinesByLineId());
            netexIndex.journeyPatternsById.addAll(serviceFrameParser.getJourneyPatternById());
            netexIndex.destinationDisplayById.addAll(serviceFrameParser.getDestinationDisplayById());
            netexIndex.authoritiesByGroupOfLinesId.addAll(serviceFrameParser.getAuthorityByGroupOfLinesId());
            netexIndex.quayIdByStopPointRef.addAll((serviceFrameParser.getQuayIdByStopPointRef()));
            netexIndex.groupOfLinesByLineId.addAll(serviceFrameParser.getGroupOfLinesByLineId());
        }
    }

    private void loadTimeTableFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof TimetableFrame) {
            TimeTableFrameParser timeTableFrameParser = new TimeTableFrameParser(netexIndex.journeyPatternsById);
            timeTableFrameParser.parse((TimetableFrame)commonFrame.getValue());
            netexIndex.serviceJourneyByPatternId.addAll(timeTableFrameParser.getServiceJourneyByPatternId());
            netexIndex.dayTypeRefs.addAll(timeTableFrameParser.getDayTypeRefs());
        }
    }

    private void loadServiceCalendarFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame) {
            ServiceCalendarFrameParser serviceCalendarFrameParser = new ServiceCalendarFrameParser();
            serviceCalendarFrameParser.parse((ServiceCalendarFrame)commonFrame.getValue());
            netexIndex.dayTypeById.addAll(serviceCalendarFrameParser.getDayTypeById());
            netexIndex.operatingPeriodById.addAll(serviceCalendarFrameParser.getOperatingPeriodById());
            netexIndex.dayTypeAssignmentByDayTypeId.addAll(serviceCalendarFrameParser.getDayTypeAssignmentByDayTypeId());
        }
    }

    private void loadResourceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ResourceFrame) {
            ResourceFrameParser resourceFrameParser = new ResourceFrameParser();
            resourceFrameParser.parse((ResourceFrame)commonFrame.getValue());
            netexIndex.authoritiesById.addAll(resourceFrameParser.getAuthorityById());
        }
    }
}
