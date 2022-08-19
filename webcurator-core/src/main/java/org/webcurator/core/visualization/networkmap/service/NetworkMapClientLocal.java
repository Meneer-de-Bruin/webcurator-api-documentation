package org.webcurator.core.visualization.networkmap.service;

import com.sleepycat.persist.EntityCursor;
import org.webcurator.common.util.Utils;
import org.webcurator.core.exceptions.DigitalAssetStoreException;
import org.webcurator.core.util.URLResolverFunc;
import org.webcurator.core.visualization.VisualizationAbstractProcessor;
import org.webcurator.core.visualization.VisualizationProcessorManager;
import org.webcurator.core.visualization.VisualizationProgressBar;
import org.webcurator.core.visualization.VisualizationProgressView;
import org.webcurator.core.visualization.networkmap.bdb.BDBNetworkMapPool;
import org.webcurator.core.visualization.networkmap.bdb.BDBRepoHolder;
import org.webcurator.core.visualization.networkmap.metadata.*;
import org.webcurator.core.visualization.networkmap.processor.IndexProcessorWarc;
import org.webcurator.core.visualization.networkmap.processor.TreeViewFolderMgmt;
import org.webcurator.domain.model.core.HarvestResultDTO;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NetworkMapClientLocal implements NetworkMapClient {
    private final BDBNetworkMapPool pool;
    private final VisualizationProcessorManager visualizationProcessorManager;
    private final TreeViewFolderMgmt folderMgmt;

    public NetworkMapClientLocal(BDBNetworkMapPool pool, VisualizationProcessorManager visualizationProcessorManager) {
        this.pool = pool;
        this.folderMgmt = new TreeViewFolderMgmt(this.pool);
        this.visualizationProcessorManager = visualizationProcessorManager;
    }

    @Override
    public NetworkMapResult initialIndex(long job, int harvestResultNumber) {
        VisualizationAbstractProcessor processor = null;
        try {
            processor = new IndexProcessorWarc(pool, job, harvestResultNumber);
            visualizationProcessorManager.startTask(processor);
        } catch (DigitalAssetStoreException | IOException e) {
            return NetworkMapResult.getInitialExtractorFailedResult();
        }
        return NetworkMapResult.getSuccessResult();
    }

    @Override
    public NetworkMapResult getDbVersion(long job, int harvestResultNumber) {
        NetworkDbVersionDTO versionDTO = new NetworkDbVersionDTO();
        versionDTO.setRetrieveResult(NetworkDbVersionDTO.RESULT_SUCCESS);
        versionDTO.setGlobalVersion(pool.getDbVersion());

        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        String currentVersion = "0.0.0";
        if (db == null) {
            versionDTO.setRetrieveResult(NetworkDbVersionDTO.RESULT_DB_NOT_EXIT);
        } else {
            currentVersion = db.getDbVersionStamp();
        }

        if (!currentVersion.equalsIgnoreCase(pool.getDbVersion())) {
            versionDTO.setRetrieveResult(NetworkDbVersionDTO.RESULT_NEED_REINDEX);
        }
        versionDTO.setCurrentVersion(currentVersion);

        String payload = this.obj2Json(versionDTO);

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(payload);

        return result;
    }

//    @Override
//    public NetworkMapResult get(long job, int harvestResultNumber, String key) {
//        BDBNetworkMap db = pool.getInstance(job, harvestResultNumber);
//        if (db == null) {
//            return NetworkMapResult.getDBMissingErrorResult();
//        }
//        NetworkMapResult result = new NetworkMapResult();
//        result.setPayload(db.get(key));
//        return result;
//    }

    @Override
    public NetworkMapResult getNode(long job, int harvestResultNumber, long id) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }
        NetworkMapResult result = new NetworkMapResult();
        NetworkMapNodeUrlEntity networkMapNode = db.getUrlById(id);
        result.setPayload(obj2Json(networkMapNode));
        return result;
    }

    @Override
    public NetworkMapResult getOutlinks(long job, int harvestResultNumber, long parentId) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        NetworkMapNodeUrlEntity parentNode = db.getUrlById(parentId);
        if (parentNode == null) {
            return NetworkMapResult.getDataNotExistResult("Could not find parent node, id: " + parentId);
        }

        NetworkMapResult result = new NetworkMapResult();
        String outlinks = combineUrlResultFromArrayIDs(job, harvestResultNumber, parentNode.getOutlinks());
        parentNode.clear();
        result.setPayload(outlinks);
        return result;
    }

    @Override
    public NetworkMapResult getChildren(long job, int harvestResultNumber, long id) {
        //TODO
        return new NetworkMapResult();
    }


    @Override
    public NetworkMapResult searchUrl2CascadePaths(long job, int harvestResultNumber, String title, NetworkMapServiceSearchCommand searchCommand) {
        List<NetworkMapNodeFolderDTO> listFolderDTO;

        if (searchCommand == null || !searchCommand.isFilterable()) {
            if (Utils.isEmpty(title)) {
                listFolderDTO = this.folderMgmt.queryRootFolderList(job, harvestResultNumber);
            } else {
                title = new String(Base64.getDecoder().decode(title));
                listFolderDTO = this.folderMgmt.queryFolderListWithTitle(job, harvestResultNumber, title);
            }
        } else {
            List<NetworkMapNodeUrlEntity> allNetworkMapNodes = this.searchUrlDTOs(job, harvestResultNumber, searchCommand);
            NetworkMapNodeFolderDTO rootTreeNode = this.folderMgmt.createFolderTreeView(job, harvestResultNumber, allNetworkMapNodes);
            if (rootTreeNode == null) {
                return NetworkMapResult.getDataNotExistResult();
            }
            listFolderDTO = rootTreeNode.getChildren();
        }

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(this.obj2Json(listFolderDTO));
        return result;
    }


    private NetworkMapNodeFolderDTO statisticTreeNodes(List<NetworkMapNodeFolderDTO> treeNodeDTOS) {
        final NetworkMapNodeFolderDTO result = new NetworkMapNodeFolderDTO();
        treeNodeDTOS.forEach(node -> {
            result.accumulate(node.getStatusCode(), node.getContentLength(), node.getContentType());
        });

        return result;
    }

    @Override
    public NetworkMapResult getAllDomains(long job, int harvestResultNumber) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }
        NetworkMapAccessPropertyEntity accProp = db.getAccProp();
        if (accProp == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }
        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(accProp.getRootDomain());
        return result;
    }

    @Override
    public NetworkMapResult getSeedUrls(long job, int harvestResultNumber) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        NetworkMapAccessPropertyEntity accProp = db.getAccProp();
        if (accProp == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        List<Long> ids = accProp.getSeedUrlIDs();
        if (ids == null) {
            return NetworkMapResult.getDataNotExistResult("Could not find seed urls");
        }
        String seedUrls = combineUrlResultFromArrayIDs(job, harvestResultNumber, ids);
        ids.clear();

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(seedUrls);
        return result;
    }

    @Override
    public NetworkMapResult getMalformedUrls(long job, int harvestResultNumber) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        NetworkMapAccessPropertyEntity accProp = db.getAccProp();
        if (accProp == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        List<Long> ids = accProp.getMalformedUrlIDs();
        if (ids == null) {
            return NetworkMapResult.getDataNotExistResult("Could not find malformed urls");
        }
        String malformedUrls = combineUrlResultFromArrayIDs(job, harvestResultNumber, ids);
        ids.clear();

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(malformedUrls);
        return result;
    }

    @Override
    public NetworkMapResult searchUrl(long job, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand) {
        if (searchCommand == null) {
            return NetworkMapResult.getBadRequestResult("Search command could not be null");
        }

        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        List<NetworkMapNodeUrlEntity> urls = searchUrlDTOs(db, job, harvestResultNumber, searchCommand);

        String json = this.obj2Json(urls);
        urls.clear();

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(json);
        return result;
    }


    public List<NetworkMapNodeUrlEntity> searchUrlDTOs(long job, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return null;
        }

        return searchUrlDTOs(db, job, harvestResultNumber, searchCommand);
    }

    public List<NetworkMapNodeUrlEntity> searchUrlDTOs(BDBRepoHolder db, long job, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand) {
        if (searchCommand == null) {
            searchCommand = new NetworkMapServiceSearchCommand();
        }
        CompiledSearchCommand compiledSearchCommand = CompiledSearchCommand.getInstance(searchCommand);

        long startTime = System.currentTimeMillis();

        final List<NetworkMapNodeUrlEntity> urls = new ArrayList<>();
        EntityCursor<NetworkMapNodeUrlEntity> cursor = db.openUrlCursor();
        for (NetworkMapNodeUrlEntity urlEntity : cursor) {
            if (!compiledSearchCommand.isInclude(urlEntity)) {
                continue;
            }
            urls.add(urlEntity);
        }
        cursor.close();
        long endTime = System.currentTimeMillis();
        log.debug("Search URLs time used: " + (endTime - startTime));
        return urls;
    }

    @Override
    public NetworkMapResult searchUrlNames(long job, int harvestResultNumber, String substring) {
        if (substring == null) {
            return NetworkMapResult.getBadRequestResult("substring could not be null");
        }

        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        List<String> urlNameList = new ArrayList<>();

        // Open the cursor.
        EntityCursor<NetworkMapNodeUrlEntity> cursor = db.openUrlCursor();
        for (NetworkMapNodeUrlEntity urlEntity : cursor) {
            if (urlEntity.getUrl().contains(substring)) {
                urlNameList.add(urlEntity.getUrl());
            }
        }
        cursor.close();

        String json = obj2Json(urlNameList);
        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(json);
        return result;
    }

    @Override
    public NetworkMapResult getHopPath(long job, int harvestResultNumber, long id) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        List<NetworkMapNodeUrlEntity> listHopPath = new ArrayList<>();
        NetworkMapNodeUrlEntity curNode = db.getUrlById(id);
        while (curNode != null) {
            listHopPath.add(curNode);
            long parentId = curNode.getParentId();
            if (parentId <= 0) {
                break;
            }
            curNode = db.getUrlById(parentId);
        }
        String json = this.obj2Json(listHopPath);

        listHopPath.forEach(NetworkMapNodeUrlEntity::clear);
        listHopPath.clear();

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(json);
        return result;
    }

    @Override
    public NetworkMapResult getHierarchy(long job, int harvestResultNumber, List<Long> ids) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        List<NetworkMapNodeUrlEntity> hierarchyLinks = new ArrayList<>();
        for (long urlId : ids) {
            NetworkMapNodeUrlEntity node = db.getUrlById(urlId);
            if (node == null) {
                continue;
            }
            hierarchyLinks.add(node);
            for (long outlinkId : node.getOutlinks()) {
                NetworkMapNodeUrlEntity outlink = db.getUrlById(outlinkId);
                node.putChild(outlink);
            }
        }

        String json = this.obj2Json(hierarchyLinks);
        hierarchyLinks.forEach(NetworkMapNodeUrlEntity::clear);
        hierarchyLinks.clear();

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(json);
        return result;
    }

    @Override
    public NetworkMapResult getUrlByName(long job, int harvestResultNumber, NetworkMapUrlCommand url) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        String urlName = url.getUrlName();
        NetworkMapNodeUrlEntity networkMapNode = db.getUrlByName(urlName);
        if (networkMapNode == null) {
            return NetworkMapResult.getDataNotExistResult();
        }

        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(this.obj2Json(networkMapNode));
        return result;
    }

    @Override
    public NetworkMapResult getUrlsByNames(long job, int harvestResultNumber, List<String> urlNameList) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);
        if (db == null) {
            return NetworkMapResult.getDBMissingErrorResult();
        }

        if (urlNameList == null) {
            return NetworkMapResult.getBadRequestResult();
        }

        List<NetworkMapNodeUrlEntity> urlList = new ArrayList<>();
        for (String urlName : urlNameList) {
            NetworkMapNodeUrlEntity node = db.getUrlByName(urlName);
            if (node != null) {
                urlList.add(node);
            }
        }
        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(obj2Json(urlList));
        return result;
    }

    private String combineUrlResultFromArrayIDs(long job, int harvestResultNumber, List<Long> ids) {
        BDBRepoHolder db = pool.getInstance(job, harvestResultNumber);

        if (ids == null || db == null) {
            return null;
        }

        final List<NetworkMapNodeUrlEntity> result = new ArrayList<>();
        ids.forEach(childId -> {
            NetworkMapNodeUrlEntity networkMapNode = db.getUrlById(childId);
            if (networkMapNode != null) {
                result.add(networkMapNode);
            }
        });

        result.sort(Comparator.comparing(NetworkMapNodeUrlEntity::getUrl));

        String json = this.obj2Json(result);
        result.clear();

        return json;
    }

    @Override
    public NetworkMapResult getProgress(long job, int harvestResultNumber) {
        VisualizationProgressBar progressBar = visualizationProcessorManager.getProgress(job, harvestResultNumber);
        if (progressBar == null) {
            return NetworkMapResult.getDataNotExistResult();
        }

        VisualizationProgressView progressView = new VisualizationProgressView(progressBar);
        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(this.obj2Json(progressView));
        return result;
    }

    @Override
    public NetworkMapResult getProcessingHarvestResultDTO(long job, int harvestResultNumber) {
        HarvestResultDTO hrDTO = visualizationProcessorManager.getHarvestResultDTO(job, harvestResultNumber);
        if (hrDTO == null) {
            return NetworkMapResult.getDataNotExistResult();
        }
        NetworkMapResult result = new NetworkMapResult();
        result.setPayload(this.obj2Json(hrDTO));
        return result;
    }
}

class CompiledSearchCommand {
    String domainLevel;
    List<SearchCommandItem> domainNameCondition = new ArrayList<>();
    List<SearchCommandItem> urlNameCondition = new ArrayList<>();
    List<SearchCommandItem> contentTypeCondition = new ArrayList<>();
    List<Integer> statusCodeCondition = new ArrayList<>();

    private CompiledSearchCommand(String domainLevel) {
        this.domainLevel = domainLevel;
    }

    public static CompiledSearchCommand getInstance(NetworkMapServiceSearchCommand searchCommand) {
        CompiledSearchCommand compiledSearchCommand = new CompiledSearchCommand(searchCommand.getDomainLevel());

        if (searchCommand.getDomainNames() != null && searchCommand.getDomainNames().size() > 0) {
            compiledSearchCommand.domainNameCondition = searchCommand.getDomainNames().stream().map(SearchCommandItem::new).collect(Collectors.toList());
        }

        if (searchCommand.getUrlNames() != null && searchCommand.getUrlNames().size() > 0) {
            compiledSearchCommand.urlNameCondition = searchCommand.getUrlNames().stream().map(SearchCommandItem::new).collect(Collectors.toList());
        }

        if (searchCommand.getContentTypes() != null && searchCommand.getContentTypes().size() > 0) {
            compiledSearchCommand.contentTypeCondition = searchCommand.getContentTypes().stream().map(SearchCommandItem::new).collect(Collectors.toList());
        }

        if (searchCommand.getStatusCodes() != null && searchCommand.getStatusCodes().size() > 0) {
            compiledSearchCommand.statusCodeCondition = new ArrayList<>(searchCommand.getStatusCodes());
        }
        return compiledSearchCommand;
    }

    public boolean isInclude(NetworkMapNodeUrlEntity urlEntity) {
        boolean isInclude = isIncludedByUrlName(urlEntity.getUrl()) &&
                isIncludedByContentType(urlEntity.getContentType()) &&
                isIncludedByStatusCode(urlEntity.getStatusCode());

        if (!isInclude) {
            return false;
        }

        String domainName = domainLevel != null && domainLevel.equals("high") ? NetworkMapNodeUrlDTO.getTopDomainName(urlEntity.getUrl()) : URLResolverFunc.url2domain(urlEntity.getUrl());
        return isIncludedByDomainName(domainName);
    }

    private boolean isIncludedByDomainName(String domainName) {
        if (domainNameCondition.size() == 0) {
            return true;
        }

        for (SearchCommandItem e : domainNameCondition) {
            if (e.match(domainName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIncludedByUrlName(String urlName) {
        if (urlNameCondition.size() == 0) {
            return true;
        }

        for (SearchCommandItem e : urlNameCondition) {
            if (e.match(urlName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIncludedByContentType(String contentType) {
        if (contentTypeCondition.size() == 0) {
            return true;
        }

        for (SearchCommandItem e : contentTypeCondition) {
            if (e.match(contentType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIncludedByStatusCode(int statusCode) {
        if (statusCodeCondition.size() == 0) {
            return true;
        }

        for (Integer e : statusCodeCondition) {
            if (e == statusCode) {
                return true;
            }
        }
        return false;
    }

    public String getDomainLevel() {
        return domainLevel;
    }
}

class SearchCommandItem {
    private final static int STAR_TYPE_NONE = 0;
    private final static int STAR_TYPE_LEFT = 1;
    private final static int STAR_TYPE_RIGHT = 2;
    private final static int STAR_TYPE_BOTH = 3;
    private final int starType;
    private final String condition;

    public SearchCommandItem(String condition) {
        if (condition.startsWith("*") && condition.endsWith("*")) {
            starType = STAR_TYPE_BOTH;
        } else if (condition.startsWith("*")) {
            starType = STAR_TYPE_LEFT;
        } else if (condition.endsWith("*")) {
            starType = STAR_TYPE_RIGHT;
        } else {
            starType = STAR_TYPE_NONE;
        }

        this.condition = condition.replaceAll("\\*", "");
    }

    public boolean match(String e) {
        switch (starType) {
            case STAR_TYPE_NONE:
                return e.equals(this.condition);
            case STAR_TYPE_LEFT:
                return e.endsWith(this.condition);
            case STAR_TYPE_RIGHT:
                return e.startsWith(this.condition);
            case STAR_TYPE_BOTH:
                return e.contains(this.condition);
            default:
                return false;
        }
    }
}