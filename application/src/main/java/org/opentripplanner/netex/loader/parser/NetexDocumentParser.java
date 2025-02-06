package org.opentripplanner.netex.loader.parser;

import static org.opentripplanner.netex.config.IgnorableFeature.FARE_FRAME;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.netex.config.IgnorableFeature;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.FareFrame;
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
  private final Set<IgnorableFeature> ignoredFeatures;

  private NetexDocumentParser(NetexEntityIndex netexIndex, Set<IgnorableFeature> ignoredFeatures) {
    this.netexIndex = netexIndex;
    this.ignoredFeatures = ignoredFeatures;
  }

  /**
   * This static method create a new parser and parse the document. The result is added to given
   * index for further processing.
   */
  public static void parseAndPopulateIndex(
    NetexEntityIndex index,
    PublicationDeliveryStructure doc,
    Set<IgnorableFeature> ignoredFeatures
  ) {
    new NetexDocumentParser(index, ignoredFeatures).parse(doc);
  }

  public static void finishUp() {
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
    if (value instanceof ResourceFrame frame) {
      parse(frame, new ResourceFrameParser());
    } else if (value instanceof ServiceCalendarFrame frame) {
      parse(frame, new ServiceCalendarFrameParser());
    } else if (value instanceof TimetableFrame frame) {
      parse(frame, new TimeTableFrameParser());
    } else if (value instanceof ServiceFrame frame) {
      parse(frame, new ServiceFrameParser(netexIndex.flexibleStopPlaceById));
    } else if (value instanceof SiteFrame frame) {
      parse(frame, new SiteFrameParser(ignoredFeatures));
    } else if (!ignoredFeatures.contains(FARE_FRAME) && value instanceof FareFrame fareFrame) {
      parse(fareFrame, new FareFrameParser());
    } else if (value instanceof CompositeFrame frame) {
      // We recursively parse composite frames and content until there
      // is no more nested frames - this is accepting documents which
      // are not withing the specification, but we leave this for the
      // document schema validation - not a OTP responsibility
      parseCompositeFrame(frame);
    } else if (value instanceof GeneralFrame || value instanceof InfrastructureFrame) {
      NetexParser.informOnElementIntentionallySkipped(LOG, value);
    } else {
      NetexParser.warnOnMissingMapping(LOG, value);
    }
  }

  private void parseCompositeFrame(CompositeFrame frame) {
    // Declare some ugly types to prevent obstructing the reading later...
    Collection<JAXBElement<? extends Common_VersionFrameStructure>> frames;

    netexIndex.timeZone.set(resolveTimeZone(frame.getFrameDefaults()));

    frames = frame.getFrames().getCommonFrame();

    for (JAXBElement<? extends Common_VersionFrameStructure> it : frames) {
      parseCommonFrame(it.getValue());
    }
  }

  private <T> void parse(T node, NetexParser<T> parser) {
    parser.parse(node);
    parser.setResultOnIndex(netexIndex);

    if (node instanceof VersionFrame_VersionStructure frame) {
      netexIndex.timeZone.set(resolveTimeZone(frame.getFrameDefaults()));
    }
  }

  private String resolveTimeZone(VersionFrameDefaultsStructure frameDefaults) {
    if (frameDefaults != null) {
      var defaultLocale = frameDefaults.getDefaultLocale();
      if (defaultLocale != null && defaultLocale.getTimeZone() != null) {
        return defaultLocale.getTimeZone();
      }
    }
    // Fallback to previously set time zone in hierarchy
    if (netexIndex.timeZone.get() != null) {
      return netexIndex.timeZone.get();
    }
    // Fallback to GMT if no time zone exists in hierarchy
    return "GMT";
  }
}
