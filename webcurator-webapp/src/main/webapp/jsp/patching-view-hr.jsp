<%@taglib prefix = "c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="org.webcurator.domain.model.core.ArcHarvestResult" %>

<div id="resultsTable">
	<table width="100%" cellpadding="2" cellspacing="0" border="0">
		<tr>
			<td><span class="midtitleGrey">Overall</span></td>
		</tr>
		<!--tr>
			<td colspan="5" class="tableHead">Overall</td>
		</tr-->
		<tr>
		    <td colspan="5">
        		<table width="100%">
                    <tr>
                        <td width="15%"><b>Harvest Number</b></td>
                        <td width="10%">"${hr.harvestNumber}"</td>
                        <td width="10%"></td>
                        <td width="15%"><b>Derived From</b></td>
                        <td width="10%">"${hr.derivedFrom}"</td>
                        <td width="10%"></td>
                        <td width="15%"><b>State</b></td>
                        <td width="15%">"${hr.state}"</td>
                    </tr>
                    <tr>
                        <td width="15%"><b>Progress</b></td>
                        <td width="20%"></td>
                        <td width="20%"></td>
                        <td width="20%"></td>
                        <td width="20%"></td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td><span class="midtitleGrey">Logs</span></td>
        </tr>
        <tr>
            <td  class="tableHead" width="30%">Patching Crawling Logs</td>
            <td width="5%"></td>
            <td  class="tableHead" width="30%">Patching Modifying Logs</td>
            <td width="5%"></td>
            <td  class="tableHead" width="30%">Patching Indexing Logs</td>
        </tr>
        <tr>
            <td width="30%">
                    <c:choose>
                        <c:when test="${empty logsCrawling}">
                        <table cellpadding="3" cellspacing="0" border="0">
                            <tr>
                                <td class="subBoxText" colspan="2">No log files are available.</td>
                            </tr>
                        </table>
                        </c:when>
                        <c:otherwise>
                        <table cellpadding="3" cellspacing="0" border="0">
                            <tr>
                                <th width="20%" class="tableHead">Filename</th>
                                <th width="20%" class="tableHead">Action</th>
                                <th width="60%" class="tableHead">Size</th>
                            </tr>
                            <c:forEach items="${logsCrawling}" var="logFile">
                            <tr>
                                <td class="subBoxText">
                                    <c:out value="${logFile.name}"/>
                                </td>
                                <td class="subBoxText">
                                    <a href="curator/target/${logFile.viewer}?targetInstanceOid=${ti.oid}&logFileName=${logFile.name}&prefix=${ArcHarvestResult.PATCH_STAGE_TYPE_NORMAL}" target="_blank">View</a> |
                                    <a href="curator/target/{logFile.retriever}?targetInstanceOid=${ti.oid}&logFileName=${logFile.name}&prefix=${ArcHarvestResult.PATCH_STAGE_TYPE_NORMAL}">Download</a>
                                </td>
                                <td class="subBoxText">
                                    <c:out value="${logFile.lengthString}"/>
                                </td>
                            </tr>
                            </c:forEach>
                        </table>
                        </c:otherwise>
                    </c:choose>
            </td>
            <td width="5%"></td>
            <td width="30%">
                    <c:choose>
                    	<c:when test="${empty logsModifying}">
                    	<table cellpadding="3" cellspacing="0" border="0">
                      		<tr>
                      			<td class="subBoxText" colspan="2">No log files are available.</td>
                      		</tr>
                      	</table>
                      	</c:when>
                      	<c:otherwise>
                    	<table cellpadding="3" cellspacing="0" border="0">
                    		<tr>
                    			<th width="20%" class="tableHead">Filename</th>
                    			<th width="20%" class="tableHead">Action</th>
                    			<th width="60%" class="tableHead">Size</th>
                    		</tr>
                      		<c:forEach items="${logsModifying}" var="logFile">
                      		<tr>
                      			<td class="subBoxText">
                        			<c:out value="${logFile.name}"/>
                      			</td>
                      			<td class="subBoxText">
                    				<a href="curator/target/${logFile.viewer}?targetInstanceOid=${ti.oid}&logFileName=${logFile.name}&prefix=${ArcHarvestResult.PATCH_STAGE_TYPE_MODIFYING}" target="_blank">View</a> |
                                    <a href="curator/target/{logFile.retriever}?targetInstanceOid=${ti.oid}&logFileName=${logFile.name}&prefix=${ArcHarvestResult.PATCH_STAGE_TYPE_MODIFYING}">Download</a>
                      			</td>
                      			<td class="subBoxText">
                        			<c:out value="${logFile.lengthString}"/>
                      			</td>
                      		</tr>
                      		</c:forEach>
                      	</table>
                      	</c:otherwise>
                    </c:choose>
            </td>
            <td width="5%"></td>
            <td width="30%">
                    <c:choose>
                    	<c:when test="${empty logsIndexing}">
                    	<table cellpadding="3" cellspacing="0" border="0">
                      		<tr>
                      			<td class="subBoxText" colspan="2">No log files are available.</td>
                      		</tr>
                      	</table>
                      	</c:when>
                      	<c:otherwise>
                    	<table cellpadding="3" cellspacing="0" border="0">
                    		<tr>
                    			<th width="20%" class="tableHead">Filename</th>
                    			<th width="20%" class="tableHead">Action</th>
                    			<th width="60%" class="tableHead">Size</th>
                    		</tr>
                      		<c:forEach items="${logsIndexing}" var="logFile">
                      		<tr>
                      			<td class="subBoxText">
                        			<c:out value="${logFile.name}"/>
                      			</td>
                      			<td class="subBoxText">
                    				<a href="curator/target/${logFile.viewer}?targetInstanceOid=${ti.oid}&logFileName=${logFile.name}&prefix=${ArcHarvestResult.PATCH_STAGE_TYPE_INDEXING}" target="_blank">View</a> |
                                    <a href="curator/target/{logFile.retriever}?targetInstanceOid=${ti.oid}&logFileName=${logFile.name}&prefix=${ArcHarvestResult.PATCH_STAGE_TYPE_INDEXING}">Download</a>
                      			</td>
                      			<td class="subBoxText">
                        			<c:out value="${logFile.lengthString}"/>
                      			</td>
                      		</tr>
                      		</c:forEach>
                      	</table>
                      	</c:otherwise>
                    </c:choose>
            </td>
        </tr>
	</table>
</div>