package org.webcurator.core.visualization.networkmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webcurator.core.visualization.VisualizationCoordinator;
import org.webcurator.core.visualization.VisualizationStatisticItem;
import org.webcurator.core.visualization.modification.PruneAndImportCoordinator;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapNode;
import org.webcurator.domain.model.core.SeedHistory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

abstract public class ResourceExtractor extends VisualizationCoordinator {
    private static final Logger log = LoggerFactory.getLogger(ResourceExtractor.class);

    protected static final int MAX_URL_LENGTH = 1020;
    protected AtomicLong atomicIdGeneratorDomain = new AtomicLong();
    protected AtomicLong atomicIdGeneratorUrl = new AtomicLong();

    protected Map<String, NetworkMapNode> results;
    protected Map<String, Boolean> seeds = new HashMap<>();

    protected ResourceExtractor(Map<String, NetworkMapNode> results, Set<SeedHistory> seeds) {
        this.results = results;
        seeds.forEach(seed -> {
            this.seeds.put(seed.getSeed(), seed.isPrimary());
        });
    }

    public void init(String logsDir, String reportsDir) throws IOException {
        this.flag = "IDX";
        this.reportTitle = StatisticItem.getPrintTitle();
        super.init(logsDir, reportsDir);
    }

    public void extract(ArchiveReader reader, String fileName) throws IOException {
        StatisticItem item = new StatisticItem();
        item.setFromFileName(reader.getStrippedFileName());
        statisticItems.add(item);

        preProcess();
        for (ArchiveRecord record : reader) {
            extractRecord(record, fileName);
            record.close();
            if (results.size() % 1000 == 0) {
                log.info("Extracting, results.size:{}", results.size());
            }
            item.increaseSucceedRecords();
        }
        postProcess();
    }

    abstract protected void preProcess();

    abstract protected void postProcess();

    abstract protected void extractRecord(ArchiveRecord rec, String fileName) throws IOException;

    public void clear() {
    }

    /**
     * borrowed(copied) from org.archive.io.arc.ARCRecord...
     *
     * @param bytes Array of bytes to examine for an EOL.
     * @return Count of end-of-line characters or zero if none.
     */
    public int getEolCharsCount(byte[] bytes) {
        int count = 0;
        if (bytes != null && bytes.length >= 1 &&
                bytes[bytes.length - 1] == '\n') {
            count++;
            if (bytes.length >= 2 && bytes[bytes.length - 2] == '\r') {
                count++;
            }
        }
        return count;
    }

    public String getJson(Object obj) {
        String json = "{}";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public long getDomainCount() {
        return atomicIdGeneratorDomain.get();
    }

    public long getUrlCount() {
        return atomicIdGeneratorUrl.get();
    }

    static class StatisticItem implements VisualizationStatisticItem {
        private String fromFileName = null;
        private long fromFileLength = -1;
        private int totalRecords = 0;
        private int failedRecords = 0;
        private int succeedRecords = 0;

        public static String getPrintTitle() {
            return "FromFileName\tFromFileLength\tTotalRecords\tSucceedRecords\tFailedRecords";
        }

        @Override
        public String toString() {
            String pFromFileName = this.fromFileName == null ? "--" : this.fromFileName;
            String pFromFileLength = this.fromFileLength < 0 ? "--" : Long.toString(this.fromFileLength);

            return String.format("%s\t%s\t%d\t%d\t%d",
                    pFromFileName,
                    pFromFileLength,
                    this.totalRecords,
                    this.succeedRecords,
                    this.failedRecords);
        }

        public void setFromFileName(String fromFileName) {
            this.fromFileName = fromFileName;
        }

        public void setFromFileLength(long fromFileLength) {
            this.fromFileLength = fromFileLength;
        }

        public void increaseSucceedRecords() {
            this.increaseSucceedRecords(1);
        }

        public void increaseSucceedRecords(int num) {
            this.succeedRecords += num;
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
