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
        netexBundle.withZipFile(file -> loadZipFile(file, netexBundle.fileHierarchy()));
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
            LOG.info("Loading {}: {}", fileDescription, entry.getName());
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

