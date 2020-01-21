/*
 *  Copyright 2006 The National Library of New Zealand
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.webcurator.domain.model.core;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.persistence.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.webcurator.core.exceptions.WCTRuntimeException;
import org.webcurator.core.profiles.*;

import java.util.LinkedList;

/**
 * Represents a set of overrides that can be applied to a Heritrix profile.
 * Each overridable setting has a value and an on/off setting, allowing 
 * the overrides to override something in the default profile with an empty
 * setting.
 * 
 */
// lazy="false"
@Entity
@Table(name = "PROFILE_OVERRIDES")
public class ProfileOverrides {
	/** The logger for this class */
	private static Log log = LogFactory.getLog(ProfileOverrides.class);

	/** The Heritrix x-path to the max-time-sec element */
	public static final String ELEM_MAX_TIME_SEC = "/crawl-order/max-time-sec";
	/** The Heritrix x-path to the max-bytes-download element */
	public static final String ELEM_MAX_BYTES_DOWNLOAD = "/crawl-order/max-bytes-download";
	/** The Heritrix x-path to the max-document-download element */
	public static final String ELEM_MAX_DOCUMENT_DOWNLOAD = "/crawl-order/max-document-download";
	/** The Heritrix x-path to the robots-honoring-policy element */
	public static final String ELEM_ROBOTS_HONOURING_POLICY = "/crawl-order/robots-honoring-policy/type";
	/** The Heritrix x-path to the max-links-hops element */
	public static final String ELEM_MAX_LINK_HOPS = "/crawl-order/scope/max-link-hops";
	/** The Heritrix x-path to the max-trans-hops element */
	public static final String ELEM_MAX_TRANSITIVE_HOPS = "/crawl-order/scope/max-trans-hops";
	/** The Heritrix x-path to the exclude-filter element */
	public static final String ELEM_SCOPE_EXCLUDE_FILTER = "/crawl-order/scope/exclude-filter/filters";
	/** The Heritrix x-path to the force-accept-filter element */
	public static final String ELEM_SCOPE_FORCE_ACCEPT_FILTER = "/crawl-order/scope/force-accept-filter/filters";
	/** The Heritrix x-path to the write-processor filters element */
	public static final String ELEM_WRITE_PROCESSORS_FILTER = "/crawl-order/write-processors/Archiver/filters";
	/** The Heritrix x-path to the http fetch exclude filters element */
	public static final String ELEM_FETCH_HTTP_EXCLUDE_FILTER = "/crawl-order/fetch-processors/HTTP/filters";
	/** The Heritrix x-path to the scope decide rules element */
	public static final String ELEM_SCOPE_DECIDE_RULES = "/crawl-order/scope/decide-rules/rules";
	/** The Heritrix x-path to the write-processor decide rules element */
	public static final String ELEM_WRITE_PROCESSORS_DECIDE_RULES = "/crawl-order/write-processors/Archiver/Archiver#decide-rules/rules";
	
	/** The unique database ID of the profile. */
	@Id
	@Column(name="PO_OID", nullable =  false)
	// Note: From the Hibernate 4.2 documentation:
	// The Hibernate team has always felt such a construct as fundamentally wrong.
	// Try hard to fix your data model before using this feature.
	@TableGenerator(name = "SharedTableIdGenerator",
			table = "ID_GENERATOR",
			pkColumnName = "IG_TYPE",
			valueColumnName = "IG_VALUE",
			pkColumnValue = "PROFILE_OVERRIDE",
			allocationSize = 1) // 50 is the default
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "SharedTableIdGenerator")
	private Long oid;
	
	/** The robots honouring policy override */
	@Column(name = "PO_ROBOTS_POLICY", length = 10)
	private String robotsHonouringPolicy = null;
	/** True to override the robots policy; otherwise false */
	@Column(name = "PO_OR_ROBOTS_POLICY")
	private boolean overrideRobotsHonouringPolicy = false;
	
	/** The maximum seconds to run the harvest */
	@Column(name = "PO_MAX_TIME_SEC")
	private Long maxTimeSec = null;
	/** True to override the maximum seconds; otherwise false */
	@Column(name = "PO_OR_MAX_TIME_SEC")
	private boolean overrideMaxTimeSec = false;
	
	/** The maximum numbers of bytes to download */
	@Column(name = "PO_MAX_BYES")
	private Long maxBytesDownload = null;
	/** True to override the maximum download size; otherwise false */
	@Column(name = "PO_OR_MAX_BYTES")
	private boolean overrideMaxBytesDownload = false;
	
	/** The maximum number of documents to harvest */
	@Column(name = "PO_MAX_DOCS")
	private Long maxHarvestDocuments = null;
	/** True to override the maximum number of documents; otherwise false */
	@Column(name = "PO_OR_MAX_DOCS")
	private boolean overrideMaxHarvestDocuments = false;
	
	/** The maximum path depth to consider for the harvest */
	@Column(name = "PO_MAX_PATH_DEPTH")
	private Integer maxPathDepth = null;
	/** True to override the maximum path depth setting; otherwise false */
	@Column(name = "PO_OR_MAX_PATH_DEPTH")
	private boolean overrideMaxPathDepth = false;
	
	/** The maximum hops to travel in the harvest */
	@Column(name = "PO_MAX_HOPS")
	private Integer maxLinkHops = null;
	/** True of override the hops maximum; otherwise false */
	@Column(name = "PO_OR_MAX_HOPS")
	private boolean overrideMaxLinkHops = false;
	
	/** The list of URIs to exclude */
	@ElementCollection()
	@CollectionTable(name = "PO_EXCLUSION_URI", joinColumns = @JoinColumn(name = "PEU_PROF_OVER_OID"))
	@Column(name = "PEU_FILTER")
	@OrderColumn(name = "PEU_IX")
	private List<String> excludeUriFilters = new LinkedList<String>();
	/** True to override the exclude filters; otherwise false */
	@Column(name = "PO_OR_EXCLUSION_URI")
	private boolean overrideExcludeUriFilters = false;
	
	/** The list of URIs to forcefully include */
	@ElementCollection()
	@CollectionTable(name = "PO_INCLUSION_URI", joinColumns = @JoinColumn(name = "PEU_PROF_OVER_OID"))
	@Column(name = "PEU_FILTER")
	@OrderColumn(name = "PEU_IX")
	private List<String> includeUriFilters = new LinkedList<String>();
	/** True to override the include filters; otherwise false */
	@Column(name = "PO_OR_INCLUSION_URI")
	private boolean overrideIncludeUriFilters = false;
	
	/** The list of MIME types to exclude */
	@Column(name = "PO_EXCL_MIME_TYPES")
	private String excludedMimeTypes = null;
	/** True to override the MIME type filters; otherwise false */
	@Column(name = "PO_OR_EXCL_MIME_TYPES")
	private boolean overrideExcludedMimeTypes = false;
	
	/** The list of credentials to add to the profile */
	@OneToMany(orphanRemoval = true, cascade = {CascadeType.ALL}) // default fetch type is LAZY
	// TODO verify correct: @hibernate.collection-key column="PC_PROFILE_OVERIDE_OID"
	@JoinColumn(name = "PC_PROFILE_OVERIDE_OID")
	// TODO @hibernate.collection-index column="PC_INDEX"
	private List<ProfileCredentials> credentials = new LinkedList<ProfileCredentials>();
	/** True to override the credentials; otherwise false */
	@Column(name = "PO_OR_CREDENTIALS")
	private boolean overrideCredentials = false;

	/** The H3 document limit */
	@Column(name = "PO_H3_DOC_LIMIT")
	private Long h3DocumentLimit = null;
	/** True to override the H3 document limit; otherwise false */
	@Column(name = "PO_H3_OR_DOC_LIMIT")
	private boolean overrideH3DocumentLimit = false;

	/** The H3 data limit */
	@Column(name = "PO_H3_DATA_LIMIT")
	private Double h3DataLimit = null;
	/** True to override the H3 data limit; otherwise false */
	@Column(name = "PO_H3_OR_DATA_LIMIT")
	private boolean overrideH3DataLimit = false;

	/** The H3 data limit unit */
	@Column(name = "PO_H3_DATA_LIMIT_UNIT")
	private String h3DataLimitUnit = null;

	/** The H3 time limit */
	@Column(name = "PO_H3_TIME_LIMIT")
	private Double h3TimeLimit = null;
	/** True to override the H3 time limit; otherwise false */
	@Column(name = "PO_H3_OR_TIME_LIMIT")
	private boolean overrideH3TimeLimit = false;

	/** The H3 time limit unit */
	@Column(name = "PO_H3_TIME_LIMIT_UNIT")
	private String h3TimeLimitUnit = null;

	/** The H3 max path depth */
	@Column(name = "PO_H3_MAX_PATH_DEPTH")
	private Long h3MaxPathDepth = null;
	/** True to override the H3 max path depth; otherwise false */
	@Column(name = "PO_H3_OR_MAX_PATH_DEPTH")
	private boolean overrideH3MaxPathDepth = false;

	/** The H3 max hops */
	@Column(name = "PO_H3_MAX_HOPS")
	private Long h3MaxHops = null;
	/** True to override the H3 max hops; otherwise false */
	@Column(name = "PO_H3_OR_MAX_HOPS")
	private boolean overrideH3MaxHops = false;

	/** The H3 max transitive hops */
	@Column(name = "PO_H3_MAX_TRANS_HOPS")
	private Long h3MaxTransitiveHops = null;
	/** True to override the H3 max transitive hops; otherwise false */
	@Column(name = "PO_H3_OR_MAX_TRANS_HOPS")
	private boolean overrideH3MaxTransitiveHops = false;

	/** The H3 ignore robots */
	@Column(name = "PO_H3_IGNORE_ROBOTS")
	private boolean h3IgnoreRobots = false;
	/** True to override the H3 ignore robots; otherwise false */
	@Column(name = "PO_H3_OR_IGNORE_ROBOTS")
	private boolean overrideH3IgnoreRobots = false;

	/** The H3 extract js */
	@Column(name = "PO_H3_EXTRACT_JS")
	private boolean h3ExtractJs = false;
	/** True to override the H3 extract js; otherwise false */
	@Column(name = "PO_H3_OR_EXTRACT_JS")
	private boolean overrideH3ExtractJs = false;

	/** The H3 ignore cookies */
	@Column(name = "PO_H3_IGNORE_COOKIES")
	private boolean h3IgnoreCookies = false;
	/** True to override the H3 ignore cookies; otherwise false */
	@Column(name = "PO_H3_OR_IGNORE_COOKIES")
	private boolean overrideH3IgnoreCookies = false;

	/** The list of blocked H3 URLs */
	@ElementCollection()
	@CollectionTable(name = "PO_H3_BLOCK_URL", joinColumns = @JoinColumn(name = "PBU_PROF_OVER_OID"))
	@Column(name = "PBU_FILTER")
	@OrderColumn(name = "PBU_IX")
	private List<String> h3BlockedUrls = new LinkedList<String>();
	/** True to override the blocked urls; otherwise false */
	@Column(name = "PO_H3_OR_BLOCK_URL")
	private boolean overrideH3BlockedUrls = false;

	/** The list of included H3 URLs */
	@ElementCollection()
	@CollectionTable(name = "PO_H3_INCLUDE_URL", joinColumns = @JoinColumn(name = "PIU_PROF_OVER_OID"))
	@Column(name = "PIU_FILTER")
	@OrderColumn(name = "PIU_IX")
	private List<String> h3IncludedUrls = new LinkedList<String>();
	/** True to override the included urls; otherwise false */
	@Column(name = "PO_H3_OR_INCL_URL")
	private boolean overrideH3IncludedUrls = false;

	@Column(name = "PO_H3_RAW_PROFILE")
	@Lob // type="materialized_clob"
	private String h3RawProfile;
	@Column(name = "PO_H3_OR_RAW_PROFILE")
	private boolean overrideH3RawProfile = false;

	/**
     * Gets the database OID of the object.
     * @return Returns the oid.
     */
	public Long getOid() {
		return oid;
	}
	
	/**
	 * Sets the OID of the object.
	 * @param oid The oid to set.
	 */
	public void setOid(Long oid) {
		this.oid = oid;
	}		
	
	/**
	 * Create a deep copy of the profile overrides that can be used on another
	 * AbstractTarget. 
	 * @return A copy of the ProfileOverrides.
	 */
	public ProfileOverrides copy() {
		ProfileOverrides copy = new ProfileOverrides();
		
		copy.robotsHonouringPolicy = robotsHonouringPolicy;
		copy.overrideRobotsHonouringPolicy = overrideRobotsHonouringPolicy;
		copy.maxTimeSec = maxTimeSec;
		copy.overrideMaxTimeSec = overrideMaxTimeSec;
		copy.maxBytesDownload = maxBytesDownload;
		copy.overrideMaxBytesDownload = overrideMaxBytesDownload;
		copy.maxHarvestDocuments = maxHarvestDocuments;
		copy.overrideMaxHarvestDocuments = overrideMaxHarvestDocuments;
		copy.maxPathDepth = maxPathDepth;
		copy.overrideMaxPathDepth = overrideMaxPathDepth;
		copy.maxLinkHops = maxLinkHops;
		copy.overrideMaxLinkHops = overrideMaxLinkHops;
		
		copy.excludeUriFilters = new LinkedList<String>();
		copy.excludeUriFilters.addAll(excludeUriFilters);
		copy.overrideExcludeUriFilters = overrideExcludeUriFilters;
		copy.includeUriFilters = new LinkedList<String>();
		copy.includeUriFilters.addAll(includeUriFilters);
		copy.overrideIncludeUriFilters = overrideIncludeUriFilters;
		copy.excludedMimeTypes = excludedMimeTypes;
		copy.overrideExcludedMimeTypes = overrideExcludedMimeTypes;

		copy.h3DocumentLimit = h3DocumentLimit;
		copy.overrideH3DocumentLimit = overrideH3DocumentLimit;
		copy.h3DataLimit = h3DataLimit;
		copy.overrideH3DataLimit = overrideH3DataLimit;
		copy.h3DataLimitUnit = h3DataLimitUnit;
		copy.h3TimeLimit = h3TimeLimit;
		copy.overrideH3TimeLimit = overrideH3TimeLimit;
		copy.h3TimeLimitUnit = h3TimeLimitUnit;
		copy.h3MaxPathDepth = h3MaxPathDepth;
		copy.overrideH3MaxPathDepth = overrideH3MaxPathDepth;
		copy.h3MaxHops = h3MaxHops;
		copy.overrideH3MaxHops = overrideH3MaxHops;
		copy.h3MaxTransitiveHops = h3MaxTransitiveHops;
		copy.overrideH3MaxTransitiveHops = overrideH3MaxTransitiveHops;
		copy.h3IgnoreRobots = h3IgnoreRobots;
		copy.overrideH3IgnoreRobots = overrideH3IgnoreRobots;
		copy.h3ExtractJs = h3ExtractJs;
		copy.overrideH3ExtractJs = overrideH3ExtractJs;
		copy.h3IgnoreCookies = h3IgnoreCookies;
		copy.overrideH3IgnoreCookies = overrideH3IgnoreCookies;
		copy.h3BlockedUrls = new LinkedList<String>();
		copy.h3BlockedUrls.addAll(h3BlockedUrls);
		copy.overrideH3BlockedUrls = overrideH3BlockedUrls;
		copy.h3IncludedUrls = new LinkedList<String>();
		copy.h3IncludedUrls.addAll(h3IncludedUrls);
		copy.overrideH3IncludedUrls = overrideH3IncludedUrls;
		copy.overrideH3RawProfile = overrideH3RawProfile;
		copy.h3RawProfile = h3RawProfile;

		copy.overrideCredentials = overrideCredentials;
		for(ProfileCredentials creds : credentials) {
			copy.credentials.add(creds.copy());
		}

		return copy;
	}

	/**
	 * Apply the profile overrides to a Heritrix 3 profile. For each element in
	 * the profile that is overriden (the flag set to true), this method
	 * deletes the value from the base profile and replaces it with the value
	 * from the overrides.
	 *
	 * @param profile The Heritrix 3 Profile to override.
	 */
	public void apply(Heritrix3Profile profile) {
		Heritrix3ProfileOptions profileOptions = profile.getHeritrix3ProfileOptions();
		if (overrideH3DocumentLimit) {
			profileOptions.setDocumentLimit(h3DocumentLimit);
		}
		if (overrideH3DataLimit) {
			profileOptions.setDataLimitUnit(ProfileDataUnit.valueOf(h3DataLimitUnit));
			profileOptions.setDataLimit(new BigDecimal(h3DataLimit).setScale(8, BigDecimal.ROUND_HALF_UP));
		}
		if (overrideH3TimeLimit) {
			profileOptions.setTimeLimitUnit(ProfileTimeUnit.valueOf(h3TimeLimitUnit));
			profileOptions.setTimeLimit(new BigDecimal(h3TimeLimit).setScale(8, BigDecimal.ROUND_HALF_UP));
		}
		if (overrideH3MaxPathDepth) {
			profileOptions.setMaxPathDepth(h3MaxPathDepth);
		}
		if (overrideH3MaxHops) {
			profileOptions.setMaxHops(h3MaxHops);
		}
		if (overrideH3MaxTransitiveHops) {
			profileOptions.setMaxTransitiveHops(h3MaxTransitiveHops);
		}
		if (overrideH3IgnoreRobots) {
			profileOptions.setIgnoreRobotsTxt(h3IgnoreRobots);
		}
		if (overrideH3ExtractJs) {
			profileOptions.setExtractJs(h3ExtractJs);
		}
		if (overrideH3IgnoreCookies) {
			profileOptions.setIgnoreCookies(h3IgnoreCookies);
		}
		if (overrideH3BlockedUrls) {
			profileOptions.setBlockURLsAsList(h3BlockedUrls);
		}
		if (overrideH3IncludedUrls) {
			profileOptions.setIncludeURLsAsList(h3IncludedUrls);
		}
	}
	
	/**
	 * Apply the profile overrides to a Heritrix profile. For each element in
	 * the profile that is overriden (the flag set to true), this method 
	 * deletes the value from the base profile and replaces it with the value
	 * from the overrides.
	 * 
	 * @param profile The Heritrix Profile to override.
	 */
	public void apply(HeritrixProfile profile) {
		try {
			// Elements on the base.
			if(overrideMaxTimeSec) {
				profile.setSimpleType(ELEM_MAX_TIME_SEC, maxTimeSec);
			}
			if(overrideMaxBytesDownload) {
				profile.setSimpleType(ELEM_MAX_BYTES_DOWNLOAD, maxBytesDownload);
			}
			if(overrideMaxHarvestDocuments) {
				profile.setSimpleType(ELEM_MAX_DOCUMENT_DOWNLOAD, maxHarvestDocuments);
			}
			
			if(profile.elementExists(ELEM_SCOPE_DECIDE_RULES))
			{
				if(overrideMaxLinkHops) {
					profile.removeFromMapByType(ELEM_SCOPE_DECIDE_RULES, "org.archive.crawler.deciderules.TooManyHopsDecideRule");
					profile.addMapElement(ELEM_SCOPE_DECIDE_RULES, "_wct_max_hops", "org.archive.crawler.deciderules.TooManyHopsDecideRule");
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_max_hops/max-hops", maxLinkHops);

					profile.removeFromMapByType(ELEM_SCOPE_DECIDE_RULES, "org.archive.crawler.deciderules.TransclusionDecideRule");
					profile.addMapElement(ELEM_SCOPE_DECIDE_RULES, "_wct_max_trans_hops", "org.archive.crawler.deciderules.TransclusionDecideRule");
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_max_trans_hops/max-trans-hops", maxLinkHops);
				}
				
				if(overrideMaxPathDepth) {
					profile.removeFromMapByType(ELEM_SCOPE_DECIDE_RULES, "org.archive.crawler.deciderules.TooManyPathSegmentsDecideRule");
					profile.addMapElement(ELEM_SCOPE_DECIDE_RULES, "_wct_max_depth", "org.archive.crawler.deciderules.TooManyPathSegmentsDecideRule");
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_max_depth/max-path-depth", maxPathDepth);
				}
	
				if(overrideExcludeUriFilters || overrideIncludeUriFilters)
				{
					profile.removeFromMapByType(ELEM_SCOPE_DECIDE_RULES, "org.archive.crawler.deciderules.MatchesListRegExpDecideRule");
				}
				
				if(overrideExcludeUriFilters) {
					profile.addMapElement(ELEM_SCOPE_DECIDE_RULES, "_wct_excl_uris", "org.archive.crawler.deciderules.MatchesListRegExpDecideRule");
					profile.setListType(ELEM_SCOPE_DECIDE_RULES + "/_wct_excl_uris/regexp-list", excludeUriFilters);
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_excl_uris/list-logic", "OR");
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_excl_uris/decision", "REJECT");
				}
				
				if(overrideIncludeUriFilters) {
					profile.addMapElement(ELEM_SCOPE_DECIDE_RULES, "_wct_incl_uris", "org.archive.crawler.deciderules.MatchesListRegExpDecideRule");
					profile.setListType(ELEM_SCOPE_DECIDE_RULES + "/_wct_incl_uris/regexp-list", includeUriFilters);
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_incl_uris/list-logic", "OR");
					profile.setSimpleType(ELEM_SCOPE_DECIDE_RULES + "/_wct_incl_uris/decision", "ACCEPT");
				}	
			}
			else
			{
				//this is an old profile
				// Set the hops filter
				if(overrideMaxLinkHops) {
					profile.removeFromMapByType(ELEM_SCOPE_EXCLUDE_FILTER, "org.archive.crawler.filter.HopsFilter");
					profile.setSimpleType(ELEM_MAX_LINK_HOPS, maxLinkHops);
					profile.setSimpleType(ELEM_MAX_TRANSITIVE_HOPS, maxLinkHops);
					
					profile.addMapElement(ELEM_SCOPE_EXCLUDE_FILTER, "_wct_max_links", "org.archive.crawler.filter.HopsFilter");
				}
				
				if(overrideMaxPathDepth) {
					profile.removeFromMapByType(ELEM_SCOPE_EXCLUDE_FILTER, "org.archive.crawler.filter.PathDepthFilter");
					profile.addMapElement(ELEM_SCOPE_EXCLUDE_FILTER, "_wct_max_depth", "org.archive.crawler.filter.PathDepthFilter");
					profile.setSimpleType(ELEM_SCOPE_EXCLUDE_FILTER + "/_wct_max_depth/max-path-depth", maxPathDepth);
				}
	
				if(overrideExcludeUriFilters) {
					profile.removeFromMapByType(ELEM_SCOPE_EXCLUDE_FILTER, "org.archive.crawler.filter.URIListRegExpFilter");
					profile.addMapElement(ELEM_SCOPE_EXCLUDE_FILTER, "_wct_excl_uris", "org.archive.crawler.filter.URIListRegExpFilter");
					profile.setListType(ELEM_SCOPE_EXCLUDE_FILTER + "/_wct_excl_uris/regexp-list", excludeUriFilters);
				}
				
				if(overrideIncludeUriFilters) {
					if(log.isWarnEnabled())
					{
						log.warn(ELEM_SCOPE_FORCE_ACCEPT_FILTER + " is no longer valid and has been ignored.");
					}
				}	
			}

			
			if(overrideExcludedMimeTypes) {
				if(profile.elementExists(ELEM_WRITE_PROCESSORS_DECIDE_RULES))
				{
					profile.removeFromMapByType(ELEM_WRITE_PROCESSORS_DECIDE_RULES, "org.archive.crawler.deciderules.ContentTypeMatchesRegExpDecideRule");
					profile.addMapElement(ELEM_WRITE_PROCESSORS_DECIDE_RULES, "_wct_content_type", "org.archive.crawler.deciderules.ContentTypeMatchesRegExpDecideRule");
					profile.setSimpleType(ELEM_WRITE_PROCESSORS_DECIDE_RULES + "/_wct_content_type/regexp", excludedMimeTypes);
					profile.setSimpleType(ELEM_WRITE_PROCESSORS_DECIDE_RULES + "/_wct_content_type/decision", "REJECT");
				}
				else
				{
					//this is an old profile
					profile.removeFromMapByType(ELEM_WRITE_PROCESSORS_FILTER, "org.archive.crawler.filter.ContentTypeRegExpFilter");
					profile.addMapElement(ELEM_WRITE_PROCESSORS_FILTER, "_wct_content_type", "org.archive.crawler.filter.ContentTypeRegExpFilter");
					profile.setSimpleType(ELEM_WRITE_PROCESSORS_FILTER + "/" + "_wct_content_type" + "/regexp", excludedMimeTypes);
				}
			}
			
			
			if(overrideRobotsHonouringPolicy) {
				profile.setSimpleType(ELEM_ROBOTS_HONOURING_POLICY, robotsHonouringPolicy);
			}
			
			if(overrideCredentials) {
				profile.clearMap(ProfileCredentials.ELEM_CREDENTIALS);
				
				Iterator<ProfileCredentials> it = credentials.iterator();
				for(int i=0; it.hasNext(); i++) {
					it.next().addToProfile(profile, "_creds_" + i);
				}
			}
		}
		catch(AttributeNotFoundException ex) {
			throw new WCTRuntimeException("Failed to find correct profile elements", ex);
		}
		catch(DuplicateNameException ex) {
			log.error("Duplicate name for Profile Override", ex);
			throw new WCTRuntimeException("Duplicate name for auto defined exclude filter", ex);
		} 
		catch (InvalidAttributeValueException e) {
			log.error("Invalid Attribute Value for Profile Override", e);
			throw new WCTRuntimeException("Invalid Attribute Exception", e);
		}
	}
	
	/** 
	 * Return flag to indicate that there are overrides set in this profile
	 * overrides object
	 * @return flag to indicat there are overrides
	 */
	public boolean hasOverrides() {
		if (isOverrideCredentials() || 
		    isOverrideExcludedMimeTypes() ||
		    isOverrideExcludeUriFilters() ||
		    isOverrideIncludeUriFilters() ||
		    isOverrideMaxBytesDownload() ||
		    isOverrideMaxHarvestDocuments() ||
		    isOverrideMaxLinkHops() ||
		    isOverrideMaxPathDepth() ||
		    isOverrideMaxTimeSec() ||
		    isOverrideRobotsHonouringPolicy()) {
			return true;
		}
		
		return false;
	}

	/**
	 * Return flag to indicate that there are H3 overrides set in this profile
	 * overrides object
	 * @return flag to indicat there are overrides
	 */
	public boolean hasH3Overrides() {
		if (isOverrideCredentials() ||
				isOverrideH3DocumentLimit() ||
				isOverrideH3DataLimit() ||
				isOverrideH3TimeLimit() ||
				isOverrideH3MaxPathDepth() ||
				isOverrideH3MaxHops() ||
				isOverrideH3MaxTransitiveHops() ||
				isOverrideH3IgnoreRobots() ||
				isOverrideH3ExtractJs() ||
				isOverrideH3IgnoreCookies() ||
				isOverrideH3BlockedUrls() ||
				isOverrideH3IncludedUrls() ||
				isOverrideH3RawProfile()) {
			return true;
		}

		return false;
	}


	/**
	 * @return Returns the contentTypeRegexp.
	 */
	public String getExcludedMimeTypes() {
		return excludedMimeTypes;
	}

	/**
	 * @param contentTypeRegexp The contentTypeRegexp to set.
	 */
	public void setExcludedMimeTypes(String contentTypeRegexp) {
		this.excludedMimeTypes = contentTypeRegexp;
	}


	/**
	 * @return Returns the maxBytesDownload.
	 */
	public Long getMaxBytesDownload() {
		return maxBytesDownload;
	}

	/**
	 * @param maxBytesDownload The maxBytesDownload to set.
	 */
	public void setMaxBytesDownload(Long maxBytesDownload) {
		this.maxBytesDownload = maxBytesDownload;
	}

	/**
	 * @return Returns the maxHarvestDocuments.
	 */
	public Long getMaxHarvestDocuments() {
		return maxHarvestDocuments;
	}

	/**
	 * @param maxHarvestDocuments The maxHarvestDocuments to set.
	 */
	public void setMaxHarvestDocuments(Long maxHarvestDocuments) {
		this.maxHarvestDocuments = maxHarvestDocuments;
	}

	/**
	 * @return Returns the maxLinkHops.
	 */
	public Integer getMaxLinkHops() {
		return maxLinkHops;
	}

	/**
	 * @param maxLinkHops The maxLinkHops to set.
	 */
	public void setMaxLinkHops(Integer maxLinkHops) {
		this.maxLinkHops = maxLinkHops;
	}

	/**
	 * @return Returns the maxPathDepth.
	 */
	public Integer getMaxPathDepth() {
		return maxPathDepth;
	}

	/**
	 * @param maxPathDepth The maxPathDepth to set.
	 */
	public void setMaxPathDepth(Integer maxPathDepth) {
		this.maxPathDepth = maxPathDepth;
	}

	/**
	 * @return Returns the maxTimeSec.
	 */
	public Long getMaxTimeSec() {
		return maxTimeSec;
	}

	/**
	 * @param maxTimeSec The maxTimeSec to set.
	 */
	public void setMaxTimeSec(Long maxTimeSec) {
		this.maxTimeSec = maxTimeSec;
	}

	/**
	 * @return Returns the uriFilters.
	 */
	public List<String> getExcludeUriFilters() {
		return excludeUriFilters;
	}

	/**
	 * @param uriFilters The uriFilters to set.
	 */
	public void setExcludeUriFilters(List<String> uriFilters) {
		this.excludeUriFilters = uriFilters;
	}
	/**
	 * @return Returns the includeUriFilters.
	 */
	public List<String> getIncludeUriFilters() {
		return includeUriFilters;
	}

	/**
	 * @param includeUriFilters The includeUriFilters to set.
	 */
	public void setIncludeUriFilters(List<String> includeUriFilters) {
		this.includeUriFilters = includeUriFilters;
	}

	/**
	 * @return Returns the robotsHonouringPolicy.
	 */
	public String getRobotsHonouringPolicy() {
		return robotsHonouringPolicy;
	}

	/**
	 * @param robotsHonouringPolicy The robotsHonouringPolicy to set.
	 */
	public void setRobotsHonouringPolicy(String robotsHonouringPolicy) {
		this.robotsHonouringPolicy = robotsHonouringPolicy;
	}

	/**
	 * @return Returns the credentials.
	 */
	public List<ProfileCredentials> getCredentials() {
		return credentials;
	}

	/**
	 * @param credentials The credentials to set.
	 */
	public void setCredentials(List<ProfileCredentials> credentials) {
		this.credentials = credentials;
	}
	



	/**
	 * @return Returns the overrideCredentials.
	 */
	public boolean isOverrideCredentials() {
		return overrideCredentials;
	}

	/**
	 * @param overrideCredentials The overrideCredentials to set.
	 */
	public void setOverrideCredentials(boolean overrideCredentials) {
		this.overrideCredentials = overrideCredentials;
	}

	/**
	 * @return Returns the overrideExcludedMimeTypes.
	 */
	public boolean isOverrideExcludedMimeTypes() {
		return overrideExcludedMimeTypes;
	}

	/**
	 * @param overrideExcludedMimeTypes The overrideExcludedMimeTypes to set.
	 */
	public void setOverrideExcludedMimeTypes(boolean overrideExcludedMimeTypes) {
		this.overrideExcludedMimeTypes = overrideExcludedMimeTypes;
	}

	/**
	 * @return Returns the overrideExcludeUriFilters.
	 */
	public boolean isOverrideExcludeUriFilters() {
		return overrideExcludeUriFilters;
	}

	/**
	 * @param overrideExcludeUriFilters The overrideExcludeUriFilters to set.
	 */
	public void setOverrideExcludeUriFilters(boolean overrideExcludeUriFilters) {
		this.overrideExcludeUriFilters = overrideExcludeUriFilters;
	}

	/**
	 * @return Returns the overrideIncludeUriFilters.
	 */
	public boolean isOverrideIncludeUriFilters() {
		return overrideIncludeUriFilters;
	}

	/**
	 * @param overrideIncludeUriFilters The overrideIncludeUriFilters to set.
	 */
	public void setOverrideIncludeUriFilters(boolean overrideIncludeUriFilters) {
		this.overrideIncludeUriFilters = overrideIncludeUriFilters;
	}

	/**
	 * @return Returns the overrideMaxBytesDownload.
	 */
	public boolean isOverrideMaxBytesDownload() {
		return overrideMaxBytesDownload;
	}

	/**
	 * @param overrideMaxBytesDownload The overrideMaxBytesDownload to set.
	 */
	public void setOverrideMaxBytesDownload(boolean overrideMaxBytesDownload) {
		this.overrideMaxBytesDownload = overrideMaxBytesDownload;
	}

	/**
	 * @return Returns the overrideMaxHarvestDocuments.
	 */
	public boolean isOverrideMaxHarvestDocuments() {
		return overrideMaxHarvestDocuments;
	}

	/**
	 * @param overrideMaxHarvestDocuments The overrideMaxHarvestDocuments to set.
	 */
	public void setOverrideMaxHarvestDocuments(boolean overrideMaxHarvestDocuments) {
		this.overrideMaxHarvestDocuments = overrideMaxHarvestDocuments;
	}

	/**
	 * @return Returns the overrideMaxLinkHops.
	 */
	public boolean isOverrideMaxLinkHops() {
		return overrideMaxLinkHops;
	}

	/**
	 * @param overrideMaxLinkHops The overrideMaxLinkHops to set.
	 */
	public void setOverrideMaxLinkHops(boolean overrideMaxLinkHops) {
		this.overrideMaxLinkHops = overrideMaxLinkHops;
	}

	/**
	 * @return Returns the overrideMaxPathDepth.
	 */
	public boolean isOverrideMaxPathDepth() {
		return overrideMaxPathDepth;
	}

	/**
	 * @param overrideMaxPathDepth The overrideMaxPathDepth to set.
	 */
	public void setOverrideMaxPathDepth(boolean overrideMaxPathDepth) {
		this.overrideMaxPathDepth = overrideMaxPathDepth;
	}

	/**
	 * @return Returns the overrideMaxTimeSec.
	 */
	public boolean isOverrideMaxTimeSec() {
		return overrideMaxTimeSec;
	}

	/**
	 * @param overrideMaxTimeSec The overrideMaxTimeSec to set.
	 */
	public void setOverrideMaxTimeSec(boolean overrideMaxTimeSec) {
		this.overrideMaxTimeSec = overrideMaxTimeSec;
	}

	/**
	 * @return Returns the overrideRobotsHonouringPolicy.
	 */
	public boolean isOverrideRobotsHonouringPolicy() {
		return overrideRobotsHonouringPolicy;
	}

	/**
	 * @param overrideRobotsHonouringPolicy The overrideRobotsHonouringPolicy to set.
	 */
	public void setOverrideRobotsHonouringPolicy(
			boolean overrideRobotsHonouringPolicy) {
		this.overrideRobotsHonouringPolicy = overrideRobotsHonouringPolicy;
	}

	/**
	 * @return Returns the h3DocumentLimit.
	 */
	public Long getH3DocumentLimit() {
		return h3DocumentLimit;
	}

	/**
	 * @param h3DocumentLimit The h3DocumentLimit to set.
	 */
	public void setH3DocumentLimit(Long h3DocumentLimit) {
		this.h3DocumentLimit = h3DocumentLimit;
	}

	/**
	 * @return Returns the overrideH3DocumentLimit.
	 */
	public boolean isOverrideH3DocumentLimit() {
		return overrideH3DocumentLimit;
	}

	/**
	 * @param overrideH3DocumentLimit The overrideH3DocumentLimit to set.
	 */
	public void setOverrideH3DocumentLimit(boolean overrideH3DocumentLimit) {
		this.overrideH3DocumentLimit = overrideH3DocumentLimit;
	}

	/**
	 * @return Returns the h3DataLimit.
	 */
	public Double getH3DataLimit() {
		return h3DataLimit;
	}

	/**
	 * @param h3DataLimit The h3DataLimit to set.
	 */
	public void setH3DataLimit(Double h3DataLimit) {
		this.h3DataLimit = h3DataLimit;
	}

	/**
	 * @return Returns the overrideH3DataLimit.
	 */
	public boolean isOverrideH3DataLimit() {
		return overrideH3DataLimit;
	}

	/**
	 * @param overrideH3DataLimit The overrideH3DataLimit to set.
	 */
	public void setOverrideH3DataLimit(boolean overrideH3DataLimit) {
		this.overrideH3DataLimit = overrideH3DataLimit;
	}

	/**
	 * @return Returns the h3DataLimitUnit.
	 */
	public String getH3DataLimitUnit() {
		return h3DataLimitUnit;
	}

	/**
	 * @param h3DataLimitUnit The h3DataLimitUnit to set.
	 */
	public void setH3DataLimitUnit(String h3DataLimitUnit) {
		this.h3DataLimitUnit = h3DataLimitUnit;
	}

	/**
	 * @return Returns the h3TimeLimit.
	 */
	public Double getH3TimeLimit() {
		return h3TimeLimit;
	}

	/**
	 * @param h3TimeLimit The h3TimeLimit to set.
	 */
	public void setH3TimeLimit(Double h3TimeLimit) {
		this.h3TimeLimit = h3TimeLimit;
	}

	/**
	 * @return Returns the overrideH3TimeLimit.
	 */
	public boolean isOverrideH3TimeLimit() {
		return overrideH3TimeLimit;
	}

	/**
	 * @param overrideH3TimeLimit The overrideH3TimeLimit to set.
	 */
	public void setOverrideH3TimeLimit(boolean overrideH3TimeLimit) {
		this.overrideH3TimeLimit = overrideH3TimeLimit;
	}

	/**
	 * @return Returns the h3TimeLimitUnit.
	 */
	public String getH3TimeLimitUnit() {
		return h3TimeLimitUnit;
	}

	/**
	 * @param h3TimeLimitUnit The h3TimeLimitUnit to set.
	 */
	public void setH3TimeLimitUnit(String h3TimeLimitUnit) {
		this.h3TimeLimitUnit = h3TimeLimitUnit;
	}

	/**
	 * @return Returns the h3MaxPathDepth.
	 */
	public Long getH3MaxPathDepth() {
		return h3MaxPathDepth;
	}

	/**
	 * @param h3MaxPathDepth The h3MaxPathDepth to set.
	 */
	public void setH3MaxPathDepth(Long h3MaxPathDepth) {
		this.h3MaxPathDepth = h3MaxPathDepth;
	}

	/**
	 * @return Returns the overrideH3MaxPathDepth.
	 */
	public boolean isOverrideH3MaxPathDepth() {
		return overrideH3MaxPathDepth;
	}

	/**
	 * @param overrideH3MaxPathDepth The overrideH3MaxPathDepth to set.
	 */
	public void setOverrideH3MaxPathDepth(boolean overrideH3MaxPathDepth) {
		this.overrideH3MaxPathDepth = overrideH3MaxPathDepth;
	}

	/**
	 * @return Returns the h3MaxHops.
	 */
	public Long getH3MaxHops() {
		return h3MaxHops;
	}

	/**
	 * @param h3MaxHops The h3MaxHops to set.
	 */
	public void setH3MaxHops(Long h3MaxHops) {
		this.h3MaxHops = h3MaxHops;
	}

	/**
	 * @return Returns the overrideH3MaxHops.
	 */
	public boolean isOverrideH3MaxHops() {
		return overrideH3MaxHops;
	}

	/**
	 * @param overrideH3MaxHops The overrideH3MaxHops to set.
	 */
	public void setOverrideH3MaxHops(boolean overrideH3MaxHops) {
		this.overrideH3MaxHops = overrideH3MaxHops;
	}

	/**
	 * @return Returns the h3MaxTransitiveHops.
	 */
	public Long getH3MaxTransitiveHops() {
		return h3MaxTransitiveHops;
	}

	/**
	 * @param h3MaxTransitiveHops The h3MaxTransitiveHops to set.
	 */
	public void setH3MaxTransitiveHops(Long h3MaxTransitiveHops) {
		this.h3MaxTransitiveHops = h3MaxTransitiveHops;
	}

	/**
	 * @return Returns the overrideH3MaxTransitiveHops.
	 */
	public boolean isOverrideH3MaxTransitiveHops() {
		return overrideH3MaxTransitiveHops;
	}

	/**
	 * @param overrideH3MaxTransitiveHops The overrideH3MaxTransitiveHops to set.
	 */
	public void setOverrideH3MaxTransitiveHops(boolean overrideH3MaxTransitiveHops) {
		this.overrideH3MaxTransitiveHops = overrideH3MaxTransitiveHops;
	}

	/**
	 * @return Returns the h3IgnoreRobots.
	 */
	public boolean isH3IgnoreRobots() {
		return h3IgnoreRobots;
	}

	/**
	 * @param h3IgnoreRobots The h3IgnoreRobots to set.
	 */
	public void setH3IgnoreRobots(boolean h3IgnoreRobots) {
		this.h3IgnoreRobots = h3IgnoreRobots;
	}

	/**
	 * @return Returns the overrideH3IgnoreRobots.
	 */
	public boolean isOverrideH3IgnoreRobots() {
		return overrideH3IgnoreRobots;
	}

	/**
	 * @param overrideH3IgnoreRobots The overrideH3IgnoreRobots to set.
	 */
	public void setOverrideH3IgnoreRobots(boolean overrideH3IgnoreRobots) {
		this.overrideH3IgnoreRobots = overrideH3IgnoreRobots;
	}

	/**
	 */
	public boolean isH3ExtractJs() {
		return h3ExtractJs;
	}

	public void setH3ExtractJs(boolean h3ExtractJs) {
		this.h3ExtractJs = h3ExtractJs;
	}

	/**
	 */
	public boolean isOverrideH3ExtractJs() {
		return overrideH3ExtractJs;
	}

	public void setOverrideH3ExtractJs(boolean overrideH3ExtractJs) {
		this.overrideH3ExtractJs = overrideH3ExtractJs;
	}

	/**
	 * @return Returns the h3IgnoreCookies.
	 */
	public boolean isH3IgnoreCookies() {
		return h3IgnoreCookies;
	}

	/**
	 * @param h3IgnoreCookies The h3IgnoreCookies to set.
	 */
	public void setH3IgnoreCookies(boolean h3IgnoreCookies) {
		this.h3IgnoreCookies = h3IgnoreCookies;
	}

	/**
	 * @return Returns the overrideH3IgnoreCookies.
	 */
	public boolean isOverrideH3IgnoreCookies() {
		return overrideH3IgnoreCookies;
	}

	/**
	 * @param overrideH3IgnoreCookies The overrideH3IgnoreCookies to set.
	 */
	public void setOverrideH3IgnoreCookies(boolean overrideH3IgnoreCookies) {
		this.overrideH3IgnoreCookies = overrideH3IgnoreCookies;
	}

	/**
	 * @return Returns the h3BlockedUrls.
	 */
	public List<String> getH3BlockedUrls() {
		return h3BlockedUrls;
	}

	/**
	 * @param h3BlockedUrls The h3BlockedUrls to set.
	 */
	public void setH3BlockedUrls(List<String> h3BlockedUrls) {
		this.h3BlockedUrls = h3BlockedUrls;
	}

	/**
	 * @return Returns the overrideH3BlockedUrls.
	 */
	public boolean isOverrideH3BlockedUrls() {
		return overrideH3BlockedUrls;
	}

	/**
	 * @param overrideH3BlockedUrls The overrideH3BlockedUrls to set.
	 */
	public void setOverrideH3BlockedUrls(boolean overrideH3BlockedUrls) {
		this.overrideH3BlockedUrls = overrideH3BlockedUrls;
	}

	/**
	 * @return Returns the h3IncludedUrls.
	 */
	public List<String> getH3IncludedUrls() {
		return h3IncludedUrls;
	}

	/**
	 * @param h3IncludedUrls The h3IncludedUrls to set.
	 */
	public void setH3IncludedUrls(List<String> h3IncludedUrls) {
		this.h3IncludedUrls = h3IncludedUrls;
	}

	/**
	 * @return Returns the overrideH3IncludedUrls.
	 */
	public boolean isOverrideH3IncludedUrls() {
		return overrideH3IncludedUrls;
	}

	/**
	 * @param overrideH3IncludedUrls The overrideH3IncludedUrls to set.
	 */
	public void setOverrideH3IncludedUrls(boolean overrideH3IncludedUrls) {
		this.overrideH3IncludedUrls = overrideH3IncludedUrls;
	}

	public String getH3RawProfile() {
		return h3RawProfile;
	}

	public void setH3RawProfile(String h3RawProfile) {
		this.h3RawProfile = h3RawProfile;
	}

	public boolean isOverrideH3RawProfile() {
		return overrideH3RawProfile;
	}

	public void setOverrideH3RawProfile(boolean overrideH3RawProfile) {
		this.overrideH3RawProfile = overrideH3RawProfile;
	}
}
