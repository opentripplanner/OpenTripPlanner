package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.InfrastructureFrame;
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

    private final NetexEntityIndex netexIndex;

    private NetexDocumentParser(NetexEntityIndex netexIndex) {
        this.netexIndex = netexIndex;
    }

    /**
     * This static method create a new parser and parse the document. The result is added
     * to given index for further processing.
     */
    public static void parseAndPopulateIndex(NetexEntityIndex index, PublicationDeliveryStructure doc) {
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
            parse((ResourceFrame) value, new ResourceFrameParser());
        } else if(value instanceof ServiceCalendarFrame) {
            parse((ServiceCalendarFrame) value, new ServiceCalendarFrameParser());
        } else if(value instanceof TimetableFrame) {
            parse((TimetableFrame) value, new TimeTableFrameParser());
        } else if(value instanceof ServiceFrame) {
            parse((ServiceFrame) value, new ServiceFrameParser(
                netexIndex.flexibleStopPlaceById
            ));
        }  else if (value instanceof SiteFrame) {
            parse((SiteFrame) value, new SiteFrameParser());
        } else if (value instanceof CompositeFrame) {
            // We recursively parse composite frames and content until there
            // is no more nested frames - this is accepting documents witch
            // are not withing the specification, but we leave this for the
            // document schema validation - not a OTP responsibility
            parseCompositeFrame((CompositeFrame) value);
        } else if (
                value instanceof GeneralFrame ||
                value instanceof InfrastructureFrame
        ) {
            NetexParser.informOnElementIntentionallySkipped(LOG, value);
        } else {
            NetexParser.warnOnMissingMapping(LOG, value);
        }
    }

    private void parseCompositeFrame(CompositeFrame frame) {
        // Declare some ugly types to prevent obstructing the reading later...
        Collection<JAXBElement<? extends Common_VersionFrameStructure>> frames;

        // TODO OTP2 #2781 - Frame defaults can be set on any frame according to the Norwegian
        //                 - profile. This only set it on the composite frame, and further
        //                 - overriding it at a sub-level will not be acknowledged, or even
        //                 - given any kind of warning. This should be fixed as part of Issue
        //                 - https://github.com/opentripplanner/OpenTripPlanner/issues/2781
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

    private <T> void parse(T node, NetexParser<T> parser) {
        parser.parse(node);
        parser.setResultOnIndex(netexIndex);
    }
}
