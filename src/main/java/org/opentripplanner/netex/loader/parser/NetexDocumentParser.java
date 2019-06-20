package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.ResourceFrame;
import org.rutebanken.netex.model.ServiceCalendarFrame;
import org.rutebanken.netex.model.ServiceFrame;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.TimetableFrame;
import org.rutebanken.netex.model.VersionFrameDefaultsStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;

/**
 * This is the root parser for a Netex XML Document. The parser ONLY read the document and
 * populate the index with entities. The parser is only responsible for populating the
 * index, not for validating the document, nor linking of entities or mapping the OTP
 * internal data structures.
 */
public class NetexDocumentParser {
    private static final Logger LOG = LoggerFactory.getLogger(NetexDocumentParser.class);

    private final NetexImportDataIndex netexIndex;

    private NetexDocumentParser(NetexImportDataIndex netexIndex) {
        this.netexIndex = netexIndex;
    }

    /**
     * This static method create a new parser and parse the document. The result is added
     * to given index for further processing.
     */
    public static void parseAndPopulateIndex(NetexImportDataIndex index, PublicationDeliveryStructure doc) {
        new NetexDocumentParser(index).parse(doc);
    }

    /** Top level parse method - parses the document. */
    private void parse(PublicationDeliveryStructure doc) {
        parseFrameList(doc.getDataObjects().getCompositeFrameOrCommonFrame());
    }

    private void parseFrameList(List<JAXBElement<? extends Common_VersionFrameStructure>> frames) {
        for (JAXBElement<? extends Common_VersionFrameStructure> frame : frames) {
            parseCommonFrame(frame.getValue());
        }
    }

    private void parseCommonFrame(Common_VersionFrameStructure value) {
        if(value instanceof ResourceFrame) {
            parseResourceFrames((ResourceFrame) value);
        } else if(value instanceof ServiceCalendarFrame) {
            parseServiceCalendarFrames((ServiceCalendarFrame) value);
        } else if(value instanceof TimetableFrame) {
            parseTimeTableFrames((TimetableFrame) value);
        } else if(value instanceof ServiceFrame) {
            parseServiceFrames((ServiceFrame) value);
        }  else if (value instanceof SiteFrame) {
            parseSiteFrames((SiteFrame) value);
        } else if (value instanceof CompositeFrame) {
            // We recursively parse composite frames and content until there
            // is no more nested frames - this is accepting documents witch
            // are not withing the specification, but we leave this for the
            // document schema validation - not a OTP responsibility
            parseCompositeFrame((CompositeFrame) value);
        } else {
            LOG.warn("Unhandled frame type: " + value.getClass());
        }
    }

    private void parseCompositeFrame(CompositeFrame frame) {
        // Declare some ugly types to prevent obstructing the reading later...
        Collection<JAXBElement<? extends Common_VersionFrameStructure>> frames;

        // TODO OTP2 - Frame defults can be set on any frame according to the Norwegian
        // TODO OTP2 - profile. This only set it on the composite frame, and further
        // TODO OTP2 - overriding it at a sub-level wil not be acknowledged, or even
        // TODO OTP2 - given any kind of warning. This should be fixed as part of Issue
        // TODO OTP2 - https://github.com/opentripplanner/OpenTripPlanner/issues/2781
        parseFrameDefaultsLikeTimeZone(frame.getFrameDefaults());

        frames = frame.getFrames().getCommonFrame();

        for (JAXBElement<? extends Common_VersionFrameStructure> it : frames) {
            parseCommonFrame(it.getValue());
        }
    }

    private void parseFrameDefaultsLikeTimeZone(VersionFrameDefaultsStructure frameDefaults) {
        String timeZone = "GMT";

        if (frameDefaults != null && frameDefaults.getDefaultLocale() != null
                && frameDefaults.getDefaultLocale().getTimeZone() != null) {
            timeZone = frameDefaults.getDefaultLocale().getTimeZone();
        }

        netexIndex.timeZone.set(timeZone);
    }

    private void parseSiteFrames(SiteFrame siteFrame) {
        SiteFrameParser parser = new SiteFrameParser();
        parser.parse(siteFrame);
        parser.setResultOnIndex(netexIndex);
    }

    private void parseServiceFrames(ServiceFrame serviceFrame) {
        ServiceFrameParser parser = new ServiceFrameParser(netexIndex.quayById);
        parser.parse(serviceFrame);
        parser.setResultOnIndex(netexIndex);
    }

    private void parseTimeTableFrames(TimetableFrame timetableFrame) {
        TimeTableFrameParser parser = new TimeTableFrameParser(netexIndex.journeyPatternsById);
        parser.parse(timetableFrame);
        parser.setResultOnIndex(netexIndex);
    }

    private void parseServiceCalendarFrames(ServiceCalendarFrame serviceCalendarFrame) {
        ServiceCalendarFrameParser parser = new ServiceCalendarFrameParser();
        parser.parse(serviceCalendarFrame);
        parser.setResultOnIndex(netexIndex);
    }

    private void parseResourceFrames(ResourceFrame resourceFrame) {
        ResourceFrameParser parser = new ResourceFrameParser();
        parser.parse(resourceFrame);
        parser.setResultOnIndex(netexIndex);
    }
}
