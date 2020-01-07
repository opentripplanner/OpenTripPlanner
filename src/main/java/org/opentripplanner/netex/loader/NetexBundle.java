package org.opentripplanner.netex.loader;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.loader.mapping.NetexMapper;
import org.opentripplanner.netex.loader.parser.NetexDocumentParser;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.standalone.config.NetexParameters;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;

import static java.util.Collections.singletonList;

/**
 * Loads/reads a NeTEx bundle of files(a zip file) and maps it into the OTP internal transit model.
 * <p>
 * The NeTEx loader will use a file naming convention to load files in a particular order and
 * keeping an index of entities to enable linking. The convention is documented here
 *{@link NetexParameters#sharedFilePattern} and here
 * {@link NetexZipFileHierarchy}.
 * <p>
 * This class is also responsible for logging progress and exception handling.
 */
public class NetexBundle {
    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    /** stack of NeTEx elements needed to link the input to existing data */
    private Deque<NetexImportDataIndex> netexIndex = new LinkedList<>();

    private final NetexZipFileHierarchy fileHierarchy;

    /** maps the NeTEx XML document to OTP transit model. */
    private NetexMapper otpMapper;

    private NetexXmlParser xmlParser;

    private final String netexFeedId;


    public NetexBundle(
            String netexFeedId,
            NetexZipFileHierarchy fileHierarchy

    ) {
        this.netexFeedId = netexFeedId;
        this.fileHierarchy = fileHierarchy;
    }

    /** load the bundle, map it to the OTP transit model and return */
    public OtpTransitServiceBuilder loadBundle(
            Deduplicator deduplicator,
            DataImportIssueStore issueStore
    ) {
        LOG.info("reading {}", fileHierarchy.filename());

        // Store result in a mutable OTP Transit Model
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        // init parser and mapper
        xmlParser = new NetexXmlParser();
        otpMapper = new NetexMapper(transitBuilder, netexFeedId, deduplicator, issueStore);

        // Load data
        fileHierarchy.load(this::loadZipFileEntries);

        return transitBuilder;
    }

    public void checkInputs() {
        fileHierarchy.checkFileExist();
    }


    /* private methods */

    /** Load all files entries in the bundle */
    private void loadZipFileEntries(NetexZipFileHierarchy entries) {

        // Add a global(this zip file) shared NeTEX DAO
        netexIndex.addFirst(new NetexImportDataIndex());

        // Load global shared files
        loadFilesThenMapToOtpTransitModel("shared file", entries.sharedEntries());

        for (GroupEntries group : entries.groups()) {
            LOG.info("reading group {}", group.name());

            newNetexImportDataScope(() -> {
                // Load shared group files
                loadFilesThenMapToOtpTransitModel(
                        "shared group file",
                        group.getSharedEntries()
                );

                for (FileEntry entry : group.getIndependentEntries()) {
                    newNetexImportDataScope(() -> {
                        // Load each independent file in group
                        loadFilesThenMapToOtpTransitModel("group file", singletonList(entry));
                    });
                }
            });
        }
    }

    /**
     * make a new index and pushes it on the index stack, before executing the task and
     * at the end pop of the index.
     */
    private void newNetexImportDataScope(Runnable task) {
        netexIndex.addFirst(new NetexImportDataIndex(index()));
        task.run();
        netexIndex.removeFirst();
    }

    /**
     * Load a set of files and map the entries to OTP Transit model after the loading is
     * complete. It is important to do this in 2 steps to be able to link references.
     * An attempt to map each entry, when read, would lead to missing references, since
     * the order entries are read is not enforced in any way.
     */
    private void loadFilesThenMapToOtpTransitModel(String fileDescription, Iterable<FileEntry> entries) {
        for (FileEntry entry : entries) {
            // Load entry and store it in the index
            loadSingeFileEntry(fileDescription, entry);
        }
        // map current NeTEx objects into the OTP Transit Model
        otpMapper.mapNetexToOtp(index().readOnlyView());
    }

    private NetexImportDataIndex index() {
        return netexIndex.peekFirst();
    }

    /** Load a single entry and store it in the index for later */
    private void loadSingeFileEntry(String fileDescription, FileEntry entry) {
        try {
            LOG.info("reading entity {}: {}", fileDescription, entry.filename());

            PublicationDeliveryStructure doc = xmlParser.parseXmlDoc(entry.toBytes());
            NetexDocumentParser.parseAndPopulateIndex(index(), doc);

        } catch (IOException | JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
