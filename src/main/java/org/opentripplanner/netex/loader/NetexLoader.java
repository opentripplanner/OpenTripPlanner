package org.opentripplanner.netex.loader;

import org.apache.commons.io.IOUtils;
import org.opentripplanner.graph_builder.module.NetexModule;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.parser.NetexDocumentParser;
import org.opentripplanner.netex.mapping.NetexMapper;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The NeTEx loader loads/reads a NeTEx bundle of files(usually a zip file) and maps it
 * into the OTP internal transit model.
 * <p/>
 * The NeTEx loader will use a file naming convention to load files in a particular order and
 * keeping some entities to enable linking. The convention is documented here:
 *{@link org.opentripplanner.standalone.NetexParameters#sharedFilePattern}
 * <p/>
 * This class is also responsible for logging progress.
 */
public class NetexLoader {
    private static final Logger LOG = LoggerFactory.getLogger(NetexModule.class);

    /** read from the file system. */
    private NetexBundle netexBundle;

    /** used to parse the XML. */
    private Unmarshaller unmarshaller;

    /** maps the NeTEx XML document to OTP transit model. */
    private NetexMapper otpMapper;

    /** stack of NeTEx elements needed to link the input to existing data */
    private Deque<NetexImportDataIndex> netexIndex = new LinkedList<>();

    /** Create a new loader for the given bundle */
    public NetexLoader(NetexBundle netexBundle) {
        this.netexBundle = netexBundle;
    }

    /** load the bundle, map it to the OTP transit model and return */
    public OtpTransitServiceBuilder loadBundle() throws Exception {
        LOG.info("reading {}" + netexBundle.getFilename());
        this.unmarshaller = createUnmarshaller();
        OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder();

        this.otpMapper = new NetexMapper(transitBuilder, netexBundle.netexParameters.netexFeedId);

        loadDao();

        return transitBuilder;
    }


    /* private methods */

    private void loadDao() {
        netexBundle.withZipFile(file -> loadZipFile(file, netexBundle.fileHierarchy()));
    }

    private void loadZipFile(ZipFile zipFile, NetexZipFileHierarchy entries) {

        // Add a global(this zip file) shared NeTEX DAO  
        netexIndex.addFirst(new NetexImportDataIndex());
        
        // Load global shared files
        loadFiles("shared file", entries.sharedEntries(), zipFile);
        mapCurrentNetexObjectsIntoOtpTransitObjects();

        for (GroupEntries group : entries.groups()) {
            LOG.info("reading group {}", group.name());

            newNetexImportDataScope(() -> {
                // Load shared group files
                loadFiles(
                        "shared group file",
                        group.sharedEntries(),
                        zipFile
                );
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

    private Unmarshaller createUnmarshaller() throws JAXBException {
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
            LOG.info("reading entity {}: {}", fileDescription, entry.getName());
            byte[] bytesArray = entryAsBytes(zipFile, entry);

            NetexDocumentParser.parseAndPopulateIndex(index(), parseXmlDoc(bytesArray));

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

}

