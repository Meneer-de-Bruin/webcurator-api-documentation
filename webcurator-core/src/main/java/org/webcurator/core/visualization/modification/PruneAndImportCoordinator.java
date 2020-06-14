package org.webcurator.core.visualization.modification;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.webcurator.core.rest.AbstractRestClient;
import org.webcurator.core.store.WCTIndexer;
import org.webcurator.core.util.ApplicationContextFactory;
import org.webcurator.core.visualization.VisualizationCoordinator;
import org.webcurator.core.visualization.VisualizationStatisticItem;
import org.webcurator.core.visualization.modification.metadata.PruneAndImportCommandRowMetadata;
import org.webcurator.core.visualization.VisualizationConstants;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class PruneAndImportCoordinator extends VisualizationCoordinator {
    protected static final Logger log = LoggerFactory.getLogger(PruneAndImportCoordinator.class);
    protected static final int BYTE_BUFF_SIZE = 1024;
    /**
     * Arc files meta data date format.
     */
    protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    protected static final SimpleDateFormat writerDF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    protected static final DateTimeFormatter fTimestamp17 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    protected String fileDir; //Upload files
    protected String baseDir; //Harvest WARC files dir
    protected boolean running = true;

    public void init(String logsDir, String reportsDir) throws IOException {
        this.flag = "MOD";
        this.reportTitle = StatisticItem.getPrintTitle();
        super.init(logsDir, reportsDir);
    }

    public String getArchiveType() {
        return archiveType();
    }

    abstract protected String archiveType();

    abstract protected void copyArchiveRecords(File fileFrom, List<String> urisToDelete, Map<String, PruneAndImportCommandRowMetadata> hrsToImport, int newHarvestResultNumber) throws Exception;

    abstract protected void importFromFile(long job, int harvestResultNumber, int newHarvestResultNumber, Map<String, PruneAndImportCommandRowMetadata> hrsToImport) throws IOException;

    abstract protected void importFromRecorder(File fileFrom, List<String> urisToDelete, int newHarvestResultNumber) throws IOException, URISyntaxException;

    protected File modificationDownloadFile(long job, int harvestResultNumber, PruneAndImportCommandRowMetadata metadata) {
        AbstractRestClient digitalAssetStoreClient = ApplicationContextFactory.getApplicationContext().getBean(WCTIndexer.class);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(digitalAssetStoreClient.getUrl(VisualizationConstants.PATH_DOWNLOAD_FILE))
                .queryParam("job", job)
                .queryParam("harvestResultNumber", harvestResultNumber)
                .queryParam("fileName", metadata.getName());
        URI uri = uriComponentsBuilder.build().toUri();


        String tempFileName = UUID.randomUUID().toString();
        File tempFile = new File(fileDir, tempFileName);
        try {
            URL url = uri.toURL();
            URLConnection conn = url.openConnection();

            OutputStream fos = Files.newOutputStream(tempFile.toPath());

            StringBuilder buf = new StringBuilder();
            buf.append("HTTP/1.1 200 OK\n");
            buf.append("Content-Type: ");
            buf.append(metadata.getContentType()).append("\n");
            buf.append("Content-Length: ");
            buf.append(metadata.getLength()).append("\n");
//            LocalDateTime ldt = LocalDateTime.ofEpochSecond(metadata.getLastModified() / 1000, 0, ZoneOffset.UTC);
//            OffsetDateTime odt = ldt.atOffset(ZoneOffset.UTC);
//            buf.append("Date: ");
//            buf.append(odt.format(DateTimeFormatter.RFC_1123_DATE_TIME)).append("\n");
            buf.append("Connection: close\n");

            fos.write(buf.toString().getBytes());
            fos.write("\n".getBytes());

            IOUtils.copy(conn.getInputStream(), fos);

            fos.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }

        return tempFile;
    }

    public String getFileDir() {
        return fileDir;
    }

    public void setFileDir(String fileDir) {
        this.fileDir = fileDir;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public void stop() {
        this.running = false;
    }

    static class StatisticItem implements VisualizationStatisticItem {
        private String fromFileName = null;
        private long fromFileLength = -1;
        private String toFileName = null;
        private long toFileLength = -1;
        private int totalRecords = 0;
        private int skippedRecords = 0;
        private int copiedRecords = 0;
        private int prunedRecords = 0;
        private int failedRecords = 0;

        public static String getPrintTitle() {
            return "FromFileName\tFromFileLength\tToFileName\tToFileLength\tTotalRecords\tSkippedRecords\tPrunedRecords\tCopiedRecords\tFailedRecords";
        }

        @Override
        public String toString() {
            String pFromFileName = this.fromFileName == null ? "--" : this.fromFileName;
            String pFromFileLength = this.fromFileLength < 0 ? "--" : Long.toString(this.fromFileLength);
            String pToFileName = this.toFileName == null ? "--" : this.toFileName;
            String pToFileLength = this.toFileLength < 0 ? "--" : Long.toString(this.toFileLength);

            return String.format("%s\t%s\t%s\t%s\t%d\t%d\t%d\t%d\t%d",
                    pFromFileName,
                    pFromFileLength,
                    pToFileName,
                    pToFileLength,
                    this.totalRecords,
                    this.skippedRecords,
                    this.prunedRecords,
                    this.copiedRecords,
                    this.failedRecords);
        }

        public void setFromFileName(String fromFileName) {
            this.fromFileName = fromFileName;
        }

        public void setFromFileLength(long fromFileLength) {
            this.fromFileLength = fromFileLength;
        }

        public void setToFileName(String toFileName) {
            this.toFileName = toFileName;
        }

        public void setToFileLength(long toFileLength) {
            this.toFileLength = toFileLength;
        }

        public void increaseSkippedRecords() {
            this.increaseSkippedRecords(1);
        }

        public void increaseSkippedRecords(int num) {
            this.skippedRecords += num;
            this.totalRecords += num;
        }

        public void increasePrunedRecords() {
            this.increasePrunedRecords(1);
        }

        public void increasePrunedRecords(int num) {
            this.prunedRecords += num;
            this.totalRecords += num;
        }

        public void increaseCopiedRecords() {
            this.increaseCopiedRecords(1);
        }

        public void increaseCopiedRecords(int num) {
            this.copiedRecords += num;
            this.totalRecords += num;
        }

        public void increaseFailedRecords() {
            this.increaseFailedRecords(1);
        }

        public void increaseFailedRecords(int num) {
            this.failedRecords += num;
            this.totalRecords += num;
        }
    }
}
