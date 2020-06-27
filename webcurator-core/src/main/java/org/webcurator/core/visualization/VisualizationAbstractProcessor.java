package org.webcurator.core.visualization;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webcurator.core.exceptions.DigitalAssetStoreException;
import org.webcurator.domain.model.core.HarvestResult;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

public abstract class VisualizationAbstractProcessor {
    protected static final Logger log = LoggerFactory.getLogger(VisualizationAbstractProcessor.class);
    protected final String processorStage;
    protected final VisualizationProgressBar progressBar;
    protected final String fileDir; //Upload files
    protected final String baseDir; //Harvest WARC files dir
    protected final String logsDir; //log dir
    protected final String reportsDir; //report dir
    protected final long targetInstanceId;
    protected final int harvestResultNumber;
    protected boolean running = true;
    protected Semaphore stopped = new Semaphore(1);

    public VisualizationAbstractProcessor(VisualizationManager visualizationManager, long targetInstanceId, int harvestResultNumber) throws DigitalAssetStoreException {
        this.processorStage = getProcessorStage();
        this.fileDir = visualizationManager.getUploadDir();
        this.baseDir = visualizationManager.getBaseDir();
        this.logsDir = baseDir + File.separator + targetInstanceId + File.separator + visualizationManager.getLogsDir() + File.separator + HarvestResult.DIR_LOGS_EXT + File.separator + HarvestResult.DIR_LOGS_MOD + File.separator + harvestResultNumber;
        this.reportsDir = baseDir + File.separator + targetInstanceId + File.separator + visualizationManager.getReportsDir() + File.separator + HarvestResult.DIR_LOGS_EXT + File.separator + HarvestResult.DIR_LOGS_MOD + File.separator + harvestResultNumber;
        this.progressBar = new VisualizationProgressBar(processorStage, targetInstanceId, harvestResultNumber);
        this.targetInstanceId = targetInstanceId;
        this.harvestResultNumber = harvestResultNumber;
    }

    abstract protected String getProcessorStage();

    public String getKey() {
        return VisualizationProcessorQueue.getKey(processorStage, targetInstanceId, harvestResultNumber);
    }

    public void process() {
        try {
            this.stopped.acquire();
            processInternal();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            this.stopped.release();
            this.progressBar.clear();
        }
    }

    abstract public void processInternal() throws Exception;

    public void pauseTask() {
        try {
            this.wait();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void resumeTask() {
        this.notifyAll();
    }

    public void terminateTask() {
        this.running = false;
        terminateInternal();
        try {
            this.stopped.acquire();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    abstract protected void terminateInternal();

    public void deleteTask() {
        this.terminateTask();
        try {
            this.stopped.acquire(); //wait until process ended
        } catch (InterruptedException e) {
            log.error("Acquire token failed when stop modification task, {}, {}", targetInstanceId, harvestResultNumber);
            e.printStackTrace();
            return;
        }

        //delete logs, reports
        delete(this.logsDir);
        delete(this.reportsDir);

        deleteInternal();
    }


    abstract public void deleteInternal();

    protected void delete(String rootDir, String dir) {
        File toPurge = new File(rootDir, dir);
        delete(toPurge);
    }

    protected void delete(String toPurge) {
        delete(new File(toPurge));
    }

    protected void delete(File toPurge) {
        log.debug("About to purge dir " + toPurge.toString());
        try {
            FileUtils.deleteDirectory(toPurge);
        } catch (IOException e) {
            log.warn("Unable to purge target instance folder: " + toPurge.getAbsolutePath());
        }
    }

    public VisualizationProgressBar getProgress() {
        return this.progressBar;
    }
}
