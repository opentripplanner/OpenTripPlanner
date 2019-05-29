package org.opentripplanner.netex.loader;

import org.apache.commons.io.IOUtils;
import org.opentripplanner.graph_builder.module.NetexModule;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.mapping.NetexMapper;
import org.opentripplanner.netex.mapping.ServiceIdMapper;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.DayTypes_RelStructure;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.GroupsOfLinesInFrame_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternsInFrame_RelStructure;
import org.rutebanken.netex.model.Journey_VersionStructure;
import org.rutebanken.netex.model.JourneysInFrame_RelStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LinesInFrame_RelStructure;
import org.rutebanken.netex.model.LinkSequence_VersionStructure;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.ResourceFrame;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RoutesInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceCalendarFrame;
import org.rutebanken.netex.model.ServiceFrame;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopAssignment_VersionStructure;
import org.rutebanken.netex.model.StopAssignmentsInFrame_RelStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.TimetableFrame;
import org.rutebanken.netex.model.VersionFrameDefaultsStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


// TODO OTP2 - JavaDoc
// TODO OTP2 - Integration test
// TODO OTP2 - Cleanup - This class is a bit big, indicating that it does more than one thing.
// TODO OTP2 - Cleanup - It is likely that a few things can be pushed down into the classes
// TODO OTP2 - Cleanup - used by this class, and maybe extract framework integration - making
// TODO OTP2 - Cleanup - the business logic "shine".
// TODO OTP2 - Cleanup - The purpose of this class should prpbebly be to give an outline of
// TODO OTP2 - Cleanup - the Netex loading, delegating to sub modules for details.
// TODO OTP2 - Cleanup - Most of  the code in here is about JAXB, so dealing with
// TODO OTP2 - Cleanup - ZipFile and ZipEntities can be extracted, separating the file container
// TODO OTP2 - Cleanup - from the XML parsing.
//
public class NetexLoader {
    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    private NetexBundle netexBundle;

    private Unmarshaller unmarshaller;

    private NetexMapper otpMapper;

    private Deque<NetexImportDataIndex> netexIndex = new LinkedList<>();

    public NetexLoader(NetexBundle netexBundle) {
        this.netexBundle = netexBundle;
    }

    public OtpTransitServiceBuilder loadBundle() throws Exception {
        LOG.info("Loading bundle " + netexBundle.getFilename());
        this.unmarshaller = createUnmarshaller();
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        this.otpMapper = new NetexMapper(transitBuilder, netexBundle.netexParameters.netexFeedId);

        loadDao();

        return transitBuilder;
    }

    private void loadDao() {
        netexBundle.withZipFile(file -> loadZipFile(file, netexBundle.fileHirarcy()));
    }

    private void loadZipFile(ZipFile zipFile, NetexZipFileHierarchy entries) {

        // Add a global(this zip file) shared NeTEX DAO  
        netexIndex.addFirst(new NetexImportDataIndex());
        
        // Load global shared files
        loadFiles("shared file", entries.sharedEntries(), zipFile);
        mapCurrentNetexObjectsIntoOtpTransitObjects();

        for (GroupEntries group : entries.groups()) {
            newNetexImportDataScope(() -> {
                // Load shared group files
                loadFiles("shared group file", group.sharedEntries(), zipFile);
                mapCurrentNetexObjectsIntoOtpTransitObjects();

                for (ZipEntry entry : group.independentEntries()) {
                    newNetexImportDataScope(() -> {
                        // Load each independent file in group
                        loadFile("group file", entry, zipFile);
                        mapCurrentNetexObjectsIntoOtpTransitObjects();
                    });
                }
            });
        }
    }

    private NetexImportDataIndex index() {
        return netexIndex.peekFirst();
    }

    private void newNetexImportDataScope(Runnable task) {
        netexIndex.addFirst(new NetexImportDataIndex(index()));
        task.run();
        netexIndex.removeFirst();
    }

    private void mapCurrentNetexObjectsIntoOtpTransitObjects() {
        otpMapper.mapNetexToOtp(index());
    }

    private Unmarshaller createUnmarshaller() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
        return jaxbContext.createUnmarshaller();
    }

    private void loadFiles(String fileDescription, Iterable<ZipEntry> entries, ZipFile zipFile) {
        for (ZipEntry entry : entries) {
            loadFile(fileDescription, entry, zipFile);
        }
    }

    private byte[] entryAsBytes(ZipFile zipFile, ZipEntry entry) {
        try {
            return IOUtils.toByteArray(zipFile.getInputStream(entry));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void loadFile(String fileDescription, ZipEntry entry, ZipFile zipFile) {
        try {
            LOG.info("Loading {}: {}", fileDescription, entry.getName());
            byte[] bytesArray = entryAsBytes(zipFile, entry);


            PublicationDeliveryStructure value = parseXmlDoc(bytesArray);
            List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrames = value
                    .getDataObjects().getCompositeFrameOrCommonFrame();
            for (JAXBElement frame : compositeFrameOrCommonFrames) {

                if (frame.getValue() instanceof CompositeFrame) {
                    CompositeFrame cf = (CompositeFrame) frame.getValue();
                    VersionFrameDefaultsStructure frameDefaults = cf.getFrameDefaults();
                    String timeZone = "GMT";
                    if (frameDefaults != null && frameDefaults.getDefaultLocale() != null
                            && frameDefaults.getDefaultLocale().getTimeZone() != null) {
                        timeZone = frameDefaults.getDefaultLocale().getTimeZone();
                    }

                    index().timeZone.set(timeZone);

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
        } catch (JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private PublicationDeliveryStructure parseXmlDoc(byte[] bytesArray) throws JAXBException {
        JAXBElement<PublicationDeliveryStructure> root;
        ByteArrayInputStream stream = new ByteArrayInputStream(bytesArray);
        //noinspection unchecked
        root = (JAXBElement<PublicationDeliveryStructure>) unmarshaller.unmarshal(stream);

        return root.getValue();
    }
    // Stop places and quays

    private void loadSiteFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof SiteFrame) {
            SiteFrame sf = (SiteFrame) commonFrame.getValue();
            StopPlacesInFrame_RelStructure stopPlaces = sf.getStopPlaces();
            List<StopPlace> stopPlaceList = stopPlaces.getStopPlace();
            for (StopPlace stopPlace : stopPlaceList) {
                    index().stopPlaceById.add(stopPlace);
                    if (stopPlace.getQuays() != null) {
                    List<Object> quayRefOrQuay = stopPlace.getQuays().getQuayRefOrQuay();
                    for (Object quayObject : quayRefOrQuay) {
                        if (quayObject instanceof Quay) {
                            Quay quay = (Quay) quayObject;
                            index().quayById.add(quay);
                        }
                    }
                }
            }
        }
    }

    private void loadServiceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //stop assignments
            StopAssignmentsInFrame_RelStructure stopAssignments = sf.getStopAssignments();
            if (stopAssignments != null) {
                List<JAXBElement<? extends StopAssignment_VersionStructure>> assignments = stopAssignments
                        .getStopAssignment();
                for (JAXBElement assignment : assignments) {
                    if (assignment.getValue() instanceof PassengerStopAssignment) {
                        PassengerStopAssignment passengerStopAssignment =
                                (PassengerStopAssignment) assignment.getValue();
                        String quayRef = passengerStopAssignment.getQuayRef().getRef();
                        Quay quay = index().quayById.lookupLastVersionById(quayRef);
                        if (quay != null) {
                            index().quayIdByStopPointRef.add(
                                    passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(),
                                    quay.getId());
                        } else {
                            LOG.warn("Quay " + quayRef + " not found in stop place file.");
                        }
                    }
                }
            }

            //routes
            RoutesInFrame_RelStructure routes = sf.getRoutes();
            if (routes != null) {
                List<JAXBElement<? extends LinkSequence_VersionStructure>> route_ = routes
                        .getRoute_();
                for (JAXBElement element : route_) {
                    if (element.getValue() instanceof Route) {
                        Route route = (Route) element.getValue();
                        index().routeById.add(route);
                    }
                }
            }

            //network
            Network network = sf.getNetwork();
            if(network != null){
                index().networkById.add(network);

                String orgRef = network.getTransportOrganisationRef().getValue().getRef();

                Authority authority = index().authoritiesById.lookup(orgRef);

                if (authority != null) {
                    index().authoritiesByNetworkId.add(network.getId(), authority);
                }

                if (network.getGroupsOfLines() != null) {
                    GroupsOfLinesInFrame_RelStructure groupsOfLines = network.getGroupsOfLines();
                    List<GroupOfLines> groupOfLines = groupsOfLines.getGroupOfLines();
                    for (GroupOfLines group : groupOfLines) {
                        index().groupOfLinesById.add(group);
                        if (authority != null) {
                            index().authoritiesByGroupOfLinesId.add(group.getId(),
                                    authority);
                        }
                    }
                }
            }

            //lines
            LinesInFrame_RelStructure lines = sf.getLines();
            if(lines != null){
                List<JAXBElement<? extends DataManagedObjectStructure>> line_ = lines.getLine_();
                for (JAXBElement element : line_) {
                    if (element.getValue() instanceof Line) {
                        Line line = (Line) element.getValue();
                        index().lineById.add(line);
                        String groupRef = line.getRepresentedByGroupRef().getRef();
                        Network network2 = index().networkById.lookup(groupRef);
                        if (network2 != null) {
                            index().networkByLineId.add(line.getId(), network2);
                        }
                        else {
                            GroupOfLines groupOfLines = index().groupOfLinesById
                                    .lookup(groupRef);
                            if (groupOfLines != null) {
                                index().groupOfLinesByLineId.add(line.getId(),
                                        groupOfLines);
                            }
                        }
                    }
                }
            }

            //journeyPatterns
            JourneyPatternsInFrame_RelStructure journeyPatterns = sf.getJourneyPatterns();
            if (journeyPatterns != null) {
                List<JAXBElement<?>> journeyPattern_orJourneyPatternView = journeyPatterns
                        .getJourneyPattern_OrJourneyPatternView();
                for (JAXBElement pattern : journeyPattern_orJourneyPatternView) {
                    if (pattern.getValue() instanceof JourneyPattern) {
                        index().journeyPatternsById.add(
                                (JourneyPattern) pattern.getValue());
                    }
                }
            }

            //destinationDisplays
            if (sf.getDestinationDisplays() != null) {
                for (DestinationDisplay destinationDisplay : sf.getDestinationDisplays().getDestinationDisplay()) {
                    index().destinationDisplayById.add(destinationDisplay);
                }
            }
        }
    }

    // ServiceJourneys
    private void loadTimeTableFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof TimetableFrame) {
            TimetableFrame timetableFrame = (TimetableFrame) commonFrame.getValue();

            JourneysInFrame_RelStructure vehicleJourneys = timetableFrame.getVehicleJourneys();
            List<Journey_VersionStructure> datedServiceJourneyOrDeadRunOrServiceJourney = vehicleJourneys
                    .getDatedServiceJourneyOrDeadRunOrServiceJourney();
            for (Journey_VersionStructure jStructure : datedServiceJourneyOrDeadRunOrServiceJourney) {
                if (jStructure instanceof ServiceJourney) {
                    loadServiceIds((ServiceJourney) jStructure);
                    ServiceJourney sj = (ServiceJourney) jStructure;
                    String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

                    JourneyPattern journeyPattern = index().journeyPatternsById
                            .lookup(journeyPatternId);

                    if (journeyPattern != null) {
                        if (journeyPattern.getPointsInSequence().
                                getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                .size() == sj.getPassingTimes().getTimetabledPassingTime().size()) {

                            index().serviceJourneyByPatternId.add(journeyPatternId, sj);
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
    }

    private void loadServiceIds(ServiceJourney serviceJourney) {
        DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
        String serviceId = ServiceIdMapper.mapToServiceId(dayTypes);
        // Add all unique service ids to map. Used when mapping calendars later.
        index().addCalendarServiceId(serviceId);
    }

    // ServiceCalendar
    private void loadServiceCalendarFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame) {
            ServiceCalendarFrame scf = (ServiceCalendarFrame) commonFrame.getValue();

            if (scf.getServiceCalendar() != null) {
                DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
                for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        index().dayTypeById.add(dayType);
                    }
                }
            }

            if (scf.getDayTypes() != null) {
                List<JAXBElement<? extends DataManagedObjectStructure>> dayTypes = scf.getDayTypes()
                        .getDayType_();
                for (JAXBElement dt : dayTypes) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        index().dayTypeById.add(dayType);
                    }
                }
            }

            if (scf.getOperatingPeriods() != null) {
                for (OperatingPeriod_VersionStructure operatingPeriodStruct : scf
                        .getOperatingPeriods().getOperatingPeriodOrUicOperatingPeriod()) {
                    OperatingPeriod operatingPeriod = (OperatingPeriod) operatingPeriodStruct;
                    index().operatingPeriodById.add(operatingPeriod);
                }
            }

            List<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments()
                    .getDayTypeAssignment();
            for (DayTypeAssignment dayTypeAssignment : dayTypeAssignments) {
                String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();
                Boolean available = dayTypeAssignment.isIsAvailable() == null ?
                        true :
                        dayTypeAssignment.isIsAvailable();
                index().dayTypeAvailable.add(dayTypeAssignment.getId(), available);

                index().dayTypeAssignmentByDayTypeId.add(ref, dayTypeAssignment);
            }
        }
    }

    // Authorities and operators
    private void loadResourceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ResourceFrame) {
            ResourceFrame resourceFrame = (ResourceFrame) commonFrame.getValue();
            List<JAXBElement<? extends DataManagedObjectStructure>> organisations = resourceFrame
                    .getOrganisations().getOrganisation_();
            for (JAXBElement element : organisations) {
                if(element.getValue() instanceof Authority){
                    Authority authority = (Authority) element.getValue();
                    index().authoritiesById.add(authority);
                }
            }
        }
    }
}

