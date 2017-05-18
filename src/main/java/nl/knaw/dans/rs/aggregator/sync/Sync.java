package nl.knaw.dans.rs.aggregator.sync;

/**
 * Created on 2017-05-18 10:57.
 */
public interface Sync {
  String CL = "cl."; // properties set by SitemapCollector
  String PROP_CL_AS_OF_DATE_TIME = CL + "a1.as.of.date.time";
  String PROP_CL_CONVERTER = CL + "a2.converter";
  String PROP_CL_COUNT_INVALID_URIS = CL + "cr.invalid.uris";
  String PROP_CL_COUNT_ERROR_RESULTS = CL + "cr.error.results";
  String PROP_CL_COUNT_UNHNDLED_RESULTS = CL + "cr.unhandled.results";
  String PROP_CL_COUNT_CAPABILITY_LISTS = CL + "cl.capability.lists";
  String PROP_CL_COUNT_RESOURCELIST_INDEXES = CL + "cl.resource.list.indexes";
  String PROP_CL_COUNT_CHANGELIST_INDEXES = CL + "cl.change.list.indexes";
  String PROP_CL_COUNT_RESOURCELISTS = CL + "cl.resource.lists";
  String PROP_CL_COUNT_CHANGELISTS = CL + "cl.change.lists";
  String PROP_CL_DATE_LATEST_RESOURCELIST = CL + "date.latest.resource.list";
  String PROP_CL_DATE_LATEST_CHANGELIST = CL + "date.latest.change.list";
  String PROP_CL_FOUND_NEW_RESOURCELIST = CL + "found.new.resourcelist";
  String PROP_CL_ITEMS_RECENT = CL + "items.recent";
  String PROP_CL_ITEMS_REMAINING = CL + "items.remaining";
  String PROP_CL_ITEMS_CREATED = CL + "items.created";
  String PROP_CL_ITEMS_UPDATED = CL + "items.updated";
  String PROP_CL_ITEMS_DELETED = CL + "items.deleted";

  String SW = "sw."; // properties set by SyncWorker
  String PROP_SW_SYNC_START = SW + "z1.sync.start";
  String PROP_SW_SYNC_END = SW + "z2.sync.end";
  String PROP_SW_FULLY_SYNCHRONIZED = SW + "z3.fully.synchronized";
  String PROP_SW_MAX_DOWNLOADS = SW + "a1.max.downloads";
  String PROP_SW_MAX_DOWNLOAD_RETRY = SW + "a2.max.download.retry";
  String PROP_SW_TRIAL_RUN = SW + "a3.trial.run";
  String PROP_SW_SITEMAP_COLLECTOR = SW + "class.sitemap.collector";
  String PROP_SW_RESOURCE_MANAGER = SW + "class.resource.manager";
  String PROP_SW_VERIFICATION_POLICY = SW + "class.verification.policy";
  String PROP_SW_TOTAL_ITEMS = SW + "total.items";
  String PROP_SW_ITEMS_VERIFIED = SW + "items.verified";
  String PROP_SW_ITEMS_DELETED = SW + "items.deleted";
  String PROP_SW_ITEMS_CREATED = SW + "items.created";
  String PROP_SW_ITEMS_UPDATED = SW + "items.updated";
  String PROP_SW_ITEMS_REMAIN = SW + "items.remain";
  String PROP_SW_ITEMS_NO_ACTION = SW + "items.no.action";
  String PROP_SW_TOTAL_FAILED_ITEMS = SW + "total.failed.items";
  String PROP_SW_FAILED_DELETIONS = SW + "failed.deletions";
  String PROP_SW_FAILED_CREATIONS = SW + "failed.creations";
  String PROP_SW_FAILED_UPDATES = SW + "failed.updates";
  String PROP_SW_FAILED_REMAINS = SW + "failed.remains";
  String PROP_SW_TOTAL_DOWNLOAD_COUNT = SW + "total.download.count";
}
