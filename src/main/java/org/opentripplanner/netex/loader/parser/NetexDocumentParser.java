package org.opentripplanner.netex.loader.parser;

import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBElement;
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
import org.rutebanken.netex.model.VersionFrame_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the root parser for a Netex XML Document. The parser ONLY read the document and populate
 * the index with entities. The parser is only responsible for populating the index, not for
 * validating the document, nor linking of entities or mapping the OTP internal data structures.
 */
public class NetexDocumentParser {

  private static final Logger LOG = LoggerFactory.getLogger(NetexDocumentParser.class);

  private final NetexEntityIndex netexIndex;

  private NetexDocumentParser(NetexEntityIndex netexIndex) {
    this.netexIndex = netexIndex;
  }

  /**
   * This static method create a new parser and parse the document. The result is added to given
   * index for further processing.
   */
  public static void parseAndPopulateIndex(
    NetexEntityIndex index,
    PublicationDeliveryStructure doc
  ) {
    new NetexDocumentParser(index).parse(doc);
  }

  public static void finnishUp() {
    ServiceFrameParser.logSummary();
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
    if (value instanceof ResourceFrame) {
      parse((ResourceFrame) value, new ResourceFrameParser());
    } else if (value instanceof ServiceCalendarFrame) {
      parse((ServiceCalendarFrame) value, new ServiceCalendarFrameParser());
    } else if (value instanceof TimetableFrame) {
      parse((TimetableFrame) value, new TimeTableFrameParser());
    } else if (value instanceof ServiceFrame) {
      parse((ServiceFrame) value, new ServiceFrameParser(netexIndex.flexibleStopPlaceById));
    } else if (value instanceof SiteFrame) {
      parse((SiteFrame) value, new SiteFrameParser());
    } else if (value instanceof CompositeFrame) {
      // We recursively parse composite frames and content until there
      // is no more nested frames - this is accepting documents which
      // are not withing the specification, but we leave this for the
      // document schema validation - not a OTP responsibility
      parseCompositeFrame((CompositeFrame) value);
    } else if (value instanceof GeneralFrame || value instanceof InfrastructureFrame) {
      NetexParser.informOnElementIntentionallySkipped(LOG, value);
    } else {
      NetexParser.warnOnMissingMapping(LOG, value);
    }
  }

  private void parseCompositeFrame(CompositeFrame frame) {
    // Declare some ugly types to prevent obstructing the reading later...
    Collection<JAXBElement<? extends Common_VersionFrameStructure>> frames;

    parseFrameDefaultsLikeTimeZone(frame.getFrameDefaults());

    frames = frame.getFrames().getCommonFrame();

    for (JAXBElement<? extends Common_VersionFrameStructure> it : frames) {
      parseCommonFrame(it.getValue());
    }
  }

  private void parseFrameDefaultsLikeTimeZone(VersionFrameDefaultsStructure frameDefaults) {
    String timeZone = null;
    if (
      frameDefaults != null &&
      frameDefaults.getDefaultLocale() != null &&
      frameDefaults.getDefaultLocale().getTimeZone() != null
    ) {
      timeZone = frameDefaults.getDefaultLocale().getTimeZone();
    }
    if (timeZone == null && netexIndex.timeZone.get() == null) {
      // Set fallback if timezone is not defined in hierarchy
      timeZone = "GMT";
    }

    if (timeZone != null) {
      netexIndex.timeZone.set(timeZone);
    }
  }

  private <T> void parse(T node, NetexParser<T> parser) {
    parser.parse(node);
    parser.setResultOnIndex(netexIndex);

    if (node instanceof VersionFrame_VersionStructure frame) {
      parseFrameDefaultsLikeTimeZone(frame.getFrameDefaults());
    }
  }
}
