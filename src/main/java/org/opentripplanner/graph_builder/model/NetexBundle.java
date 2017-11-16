package org.opentripplanner.graph_builder.model;


import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.NetexParameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NetexBundle {

    private File file;
    public static final String NETEX_COMMON_FILE_NAME_PREFIX =  "_";

    public static final String NETEX_STOP_PLACE_FILENAME =  "_stops.xml";

    public boolean linkStopsToParentStations = false;

    public boolean parentStationTransfers = false;

    public int subwayAccessTime;

    public int maxInterlineDistance;

    private double maxStopToShapeSnapDistance = 150;

    private NetexParameters netexParameters;

    public NetexBundle(File file) {
        this.file = file;
    }

    public NetexBundle(File netexFile, GraphBuilderParameters builderParams) {
        this.file = netexFile;
        linkStopsToParentStations = builderParams.parentStopLinking;
        parentStationTransfers = builderParams.stationTransfers;
        subwayAccessTime = (int)(builderParams.subwayAccessTime * 60);
        maxInterlineDistance = builderParams.maxInterlineDistance;
        netexParameters = builderParams.netex;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public NetexParameters getNetexParameters() {
        return netexParameters;
    }

    public List<ZipEntry> getFileEntriesInOrder(){
        List<ZipEntry> fileEntriesList = new ArrayList<>();

        try {
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);

            // Add stop place file
            fileEntriesList.add(zipFile.stream().filter(files ->
                    files.getName().equals(NETEX_STOP_PLACE_FILENAME)).findFirst().get());

            List<ZipEntry> commonFiles = zipFile.stream().filter(files -> files.getName().startsWith
                    (NETEX_COMMON_FILE_NAME_PREFIX) && !(files.getName().equals(NETEX_STOP_PLACE_FILENAME))).collect(Collectors.toList());

            for (ZipEntry commonFile : commonFiles) {
                // Add common file for this codespace
                fileEntriesList.add(commonFile);
                String prefix = commonFile.getName().split("_")[1];
                // Add all line files for this codespace
                fileEntriesList.addAll(zipFile.stream().filter(files -> files.getName().startsWith
                        (prefix)).collect(Collectors.toList()));
            }

            return fileEntriesList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public InputStream getFileInputStream(ZipEntry entry){
        try {
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);
            return zipFile.getInputStream(entry);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void checkInputs() {
        if (file != null) {
            if (!file.exists()) {
                throw new RuntimeException("NETEX Path " + file + " does not exist.");
            }
            if (!file.canRead()) {
                throw new RuntimeException("NETEX Path " + file + " cannot be read.");
            }
        }
    }

    public double getMaxStopToShapeSnapDistance() {
        return maxStopToShapeSnapDistance;
    }

    public void setMaxStopToShapeSnapDistance(double maxStopToShapeSnapDistance) {
        this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    }
}
