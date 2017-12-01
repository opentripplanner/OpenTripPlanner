package org.opentripplanner.netex.loader;


import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.NetexParameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NetexBundle {

    private final static double MAX_STOP_TO_SHAPE_SNAP_DISTANCE = 150;

    private final File file;

    public final boolean linkStopsToParentStations;

    public final boolean parentStationTransfers;

    public final int subwayAccessTime;

    public final int maxInterlineDistance;

    public final NetexParameters netexParameters;

    public NetexBundle(File netexZipFile, GraphBuilderParameters builderParams) {
        this.file = netexZipFile;
        this.linkStopsToParentStations = builderParams.parentStopLinking;
        this.parentStationTransfers = builderParams.stationTransfers;
        this.subwayAccessTime = (int)(builderParams.subwayAccessTime * 60);
        this.maxInterlineDistance = builderParams.maxInterlineDistance;
        this.netexParameters = builderParams.netex;
    }

    public String getFilename() {
        return file.getPath();
    }

    NetexZipFileHierarchy fileHirarcy(){
        try {
            return new NetexZipFileHierarchy(file, netexParameters);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void withZipFile(Consumer<ZipFile> zipFileConsumer) {
        try {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(file, ZipFile.OPEN_READ);
                zipFileConsumer.accept(zipFile);
            }
            finally {
                if(zipFile != null) {
                    zipFile.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Deprecated
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
        return MAX_STOP_TO_SHAPE_SNAP_DISTANCE;
    }

}
