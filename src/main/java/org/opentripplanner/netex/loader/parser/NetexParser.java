package org.opentripplanner.netex.loader.parser;

import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.rutebanken.netex.model.DerivedViewStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.RelationshipStructure;
import org.rutebanken.netex.model.VersionFrame_VersionStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;
import org.slf4j.Logger;

import javax.xml.bind.JAXBElement;

/**
 * An abstract parser of given type T. Enforce two steps parsing:
 * <ol>
 *     <li>parse(...)</li>
 *     <li>setResultOnIndex(...)</li>
 * </ol>
 */
@SuppressWarnings("SameParameterValue")
abstract class NetexParser<T> {

    /** Perform parsing and keep the parsed objects internally. */
    abstract void parse(T node);

    /** Add the result - the parsed objects - to the index. */
    abstract void setResultOnIndex(NetexImportDataIndex netexIndex);


    /* static methods for logging unhandled elements - this ensure consistent logging. */

    static void checkCommonProperties(Logger log, VersionFrame_VersionStructure rel) {
        // Direct members of VersionFrame_VersionStructure
        logUnknownObject(log, rel.getTypeOfFrameRef());
        logUnknownObject(log, rel.getBaselineVersionFrameRef());
        logUnknownObject(log, rel.getCodespaces());
        logUnknownObject(log, rel.getFrameDefaults());
        logUnknownObject(log, rel.getVersions());
        logUnknownObject(log, rel.getTraces());
        logUnknownObject(log, rel.getContentValidityConditions());

        // Members of super class DataManagedObjectStructure
        logUnsupportedObject(log, rel.getKeyList());
        logUnsupportedObject(log, rel.getExtensions());
        logUnknownObject(log, rel.getBrandingRef());
    }

    static void logUnknownElement(Logger log, JAXBElement<?> element) {
        logUnknownObject(log, element);
    }
    static void logUnknownElement(Logger log, EntityStructure entity) {
        logUnknownObject(log, entity);
    }
    static void logUnknownElement(Logger log, DerivedViewStructure rel) {
        logUnknownObject(log, rel);
    }
    static void logUnknownElement(Logger log, RelationshipStructure rel) {
        logUnknownObject(log, rel);
    }
    static void logUnknownElement(Logger log, VersionOfObjectRefStructure rel) {
        logUnknownObject(log, rel);
    }

    /**
     * Unknown object are Netex elements witch is not mapped - that might be relevant to
     * the routing. They may even be part of the Nordic profile,
     * see https://enturas.atlassian.net/wiki/spaces/PUBLIC/overview, but not implemented jet.
     * If you get these warnings and need the element to be mapped please feel free to report
     * an issue on OTP GitHub.
     */
    static void logUnknownObject(Logger log, Object rel) {
        if(rel == null) return;
        log.warn("Netex element ignored: {}", rel.getClass().getName());
    }

    /**
     * Unsupported elements are not relevant for Transit Routing, if you realy think they
     * should be used in transit routing feel free to report an issue OTP GitHub.
     */
    static void logUnsupportedObject(Logger log, Object rel) {
        if(rel == null) return;
        log.info("Netex unsupported element: {}", rel.getClass().getName());
    }

}
