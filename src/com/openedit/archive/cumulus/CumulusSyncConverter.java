/*
 * Created on Mar 24, 2006
 */
package com.openedit.archive.cumulus;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.Data;
import org.openedit.data.PropertyDetail;
import org.openedit.data.PropertyDetails;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.SourcePathCreator;
import org.openedit.entermedia.creator.CreatorManager;
import org.openedit.repository.Repository;
import org.openedit.repository.filesystem.FileRepository;
import org.openedit.util.DateStorageUtil;
import org.openedit.xml.XmlArchive;
import org.openedit.xml.XmlFile;

import com.canto.cumulus.AssetReference;
import com.canto.cumulus.AssetReferencePart;
import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.Categories;
import com.canto.cumulus.Cumulus;
import com.canto.cumulus.CumulusException;
import com.canto.cumulus.Field;
import com.canto.cumulus.Fields;
import com.canto.cumulus.GUID;
import com.canto.cumulus.Pixmap;
import com.canto.cumulus.Record;
import com.canto.cumulus.Records;
import com.canto.cumulus.ServerCatalog;
import com.canto.cumulus.StringEnum;
import com.canto.cumulus.StringEnumList;
import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.WebServer;
import com.openedit.config.Configuration;
import com.openedit.page.Page;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;
import com.openedit.util.TaskRunner;

public class CumulusSyncConverter extends BaseCumulusConvert {
	private static final Log log = LogFactory
			.getLog(CumulusSyncConverter.class);

	protected SimpleDateFormat fieldCumulusFormat = new SimpleDateFormat(
			"M/d/yyyy");// H:mm:ss a");
	protected SimpleDateFormat fieldIntDateFormat = new SimpleDateFormat(
			"yyyymmdd");// H:mm:ss a");
	// EEE MMM dd HH:mm:ss z yyyy Wed Dec 31 18:00:00 EST 1997
	// EEE, d MMM yyyy HH:mm:ss Wed, 4 Jul 2001 12:08:56 -0700T
	protected SimpleDateFormat fieldCumulusSyncFormat = new SimpleDateFormat(
			"EEE MMM dd HH:mm:ss z yyyy");
	// protected ImageConvertQueue fieldImageConvertQueue;
	protected TaskRunner fieldOddRunner;
	protected TaskRunner fieldEvenRunner;
	protected XmlArchive fieldXmlArchive;
	protected Map fieldFileFormats;
	protected WebServer fieldWebServer;

	public WebServer getWebServer() {
		return fieldWebServer;
	}

	public void setWebServer(WebServer webServer) {
		fieldWebServer = webServer;
	}

	public SourcePathCreator getAssetSourcePathCreator(
			MediaArchive inMediaArchive) {
		return (SourcePathCreator) inMediaArchive.getModuleManager().getBean(
				inMediaArchive.getCatalogId(), "sourcepathcreator");

	}

	public CumulusSyncConverter() {

	}

	public synchronized void importAssets(MediaArchive inArchive,
			ConvertStatus status) throws Exception {
		Page server = getPageManager().getPage("/WEB-INF/cumulus.xml");
		if (server.exists()) {
			if (fieldFileFormats != null) {
				fieldFileFormats.clear();
			}
			log.info("sync to cumulus " + server);
			if (status.getInputs().size() > 0) {
				inputNewFiles(status, inArchive);
			}
			syncToCumulus(inArchive, status);
			return;
		} else {
			log.error("No such file /WEB-INF/cumulus.xml");
		}
	}

	public com.canto.cumulus.Category getCumulusCategory(Categories categories,
			String inId) throws CumulusException, OpenEditException {
		Categories root = categories.getCategory("$Categories")
				.getSubCategories();
		int id = Integer.parseInt(inId);
		com.canto.cumulus.Category cat = findCategoryInTree(root, id);
		if (cat == null) {
			throw new OpenEditException("Cannot find category with id " + inId);
		}
		return cat;
	}

	protected void inputNewFiles(ConvertStatus status, MediaArchive inArchive)
			throws Exception {
		// String catalogname = status.getCategory().get("externalid"); //name
		// of the catalog
		// String categoryid = status.getCategory().get("cumulusid"); //id of
		// the category
		//
		// if( catalogname != null)
		// {
		//
		// for (Iterator iter = status.getInputs().iterator(); iter.hasNext();)
		// {
		// String path = (String ) iter.next();
		// Record record = catalogFileIntoCumulus(catalogname, categoryid,
		// path);
		// Map guidToname = ListOrderedMap.decorate(new HashMap());
		// PropertyDetails details =
		// inArchive.getStore().getAssetArchive().getPropertyDetails();
		// Asset newproduct = updateAsset(status.getCategory(),record ,
		// guidToname, inArchive, status, details);
		// inArchive.getStore().saveAsset(newproduct);
		// status.addConvertedAsset(newproduct);
		// }
		// status.setInputProcessed(true);
		// }
	}

	public Record catalogFileIntoCumulus(String inCatalogName,
			String inCategoryID, String path) throws Exception {
		CatalogCollection collection = getCumulusConnectionPool()
				.getCatalogCollection(inCatalogName);
		com.canto.cumulus.Category cat = getCumulusCategory(
				collection.getCategories(), inCategoryID);
		return catalogFileIntoCumulus(inCatalogName, cat, path);
	}

	public Record catalogFileIntoCumulus(String inCatalogName,
			com.canto.cumulus.Category inCategory, String path)
			throws Exception {

		CatalogCollection collection = getCumulusConnectionPool()
				.getCatalogCollection(inCatalogName);
		File input = new File(path);
		// Asset asset = new Asset(collection.getCumulusSession(), input);
		collection.catalogAsset(input);
		// collection.catalogAsset(asset);

		Records records = collection.getRecords();
		Record record = records.getRecord(input.getName());

		Categories categories = record.getCategories();
		categories.addCategory(inCategory);
		record.save();
		return record;
	}

	protected void syncToCumulus(final MediaArchive inArchive,
			ConvertStatus inStatus) throws Exception {
		int total = getServerCatalogs().countServerCatalogs();
		logMessage(inStatus, "Found " + total + " catalogs ");
		for (int i = 0; i < total; i++) {
			// TODO: Handle catalogs getting deleted or hidden
			ServerCatalog cumulusCat = getServerCatalogs().getServerCatalog(i);
			boolean published = false;
			try {
				published = cumulusCat.getIsPublishedToInternet();

			} catch (Exception ex) {
				// may have lost connection
				log.error("Cumulus connection failed");
				getCumulusConnectionPool().reconnect();
				return;
			}
			if (published) {
				// inStatus.add("Found published" );
				Category rootCat = inArchive.getCategoryArchive()
						.getRootCategory();
				boolean success = false;
				for (Iterator iterator = rootCat.getChildren().iterator(); iterator
						.hasNext();) {
					Category child = (Category) iterator.next();
					String exId = child.get("externalid");
					String skip = child.get("skip");
					if (exId != null && exId.equals(cumulusCat.getName())
							&& !"false".equals(skip)) {
						beginCatalogImport(inArchive, inStatus, cumulusCat,
								child);
						success = true;
					}
				}
				if (!success) {
					inStatus.add("catalog name: " + cumulusCat.getName()
							+ " did not match any externalid property in"
							+ inArchive.getCatalogId());
				}
			} else {
				log.info("Skip unpublished catalog: " + cumulusCat.getName());
				inStatus.add("Skipped unpublished catalog "
						+ cumulusCat.getName());
			}
		}
		log.info("Completed Cumulus Sync");
	}

	protected void beginCatalogImport(final MediaArchive inArchive,
			ConvertStatus inStatus, ServerCatalog cumulusCat, Category child)
			throws CumulusException, Exception {
		String lastchecked = getCurrentTime(inArchive);
		Category fresh = new Category(child.getId(), child.getName());
		fresh.setParentCategory(new Category("index", "Index"));
		fresh.setProperties(child.getProperties());

		CatalogCollection col = cumulusCat.open();
		if (!inStatus.isForcedConvert()) {
			filterResults(col, child, inArchive);
		}
		// loop over each record found
		Records records = col.getRecords();
		int c = records.countRecords();
		if (c == 0) {
			logMessage(inStatus,
					"No records changed in " + cumulusCat.getName());
			return;
		} else {
			// if (c > 1000) //TODO: Build up a list of asset and save 100 at a
			// time. Also, check that the index is not already open
			// {
			// inStatusLog.setReindex(true);
			// }

			// Update the time

			com.canto.cumulus.Category startCat = getTopCategory(inArchive, col);
			if (startCat == null) {
				logMessage(inStatus, "No valid category. Skipping catalog "
						+ cumulusCat.getName());
				return;
			}

			// syncOneCatalog( fresh, cumulusCat, inArchive, inStatus);
			logMessage(inStatus, "Adding " + cumulusCat.getName() + " to "
					+ inArchive.getCatalogHome());
			syncCatalogTree(startCat, fresh, inArchive);

			child.setChildren(fresh.getChildren());
			child.setName(fresh.getName());

			inArchive.getCategoryArchive().saveCategory(child);
			// inArchive.getStore().getCategoryArchive().reloadCategories();

			String exportfilters = getSettings(inArchive).getChildValue(
					"exportfilters");
			if (Boolean.parseBoolean(exportfilters)) {
				exportFilterListData(col.getCatalogRootCategory(), inArchive);
			}

		}

		logMessage(inStatus, "Begin processing " + c + " records in "
				+ cumulusCat.getName());

		syncCatalogRecords(inArchive, inStatus, child, records);

		col.close();

		// Update the last check time
		child.setProperty("lastchecked", lastchecked);
		inArchive.getCategoryArchive().saveCategory(child);
	}

	protected void syncCatalogRecords(MediaArchive inArchive,
			ConvertStatus inStatusLog, Category inOECategory, Records inRecords)
			throws Exception {
		long errortotal = 0;
		long maxerrors = 0;
		int count = 0; // A message is logged when this reaches 600
		int numSuccessful = 0;

		long start = System.currentTimeMillis();

		String errorLimit = getSettings(inArchive).getChildValue("errorlimit");
		if (errorLimit != null) {
			maxerrors = Long.parseLong(errorLimit);
		}
		int totalRecords = inRecords.countRecords();

		List unsavedassets = new ArrayList(600);
		Map guidToname = ListOrderedMap.decorate(new HashMap());
		PropertyDetails details = inArchive.getPropertyDetailsArchive()
				.getPropertyDetails("asset");

		Page root = getPageManager().getPage(
				inArchive.getCatalogHome() + "/assets/" + inOECategory.getId(),
				true);

		for (int currentNum = 0; currentNum < totalRecords; currentNum++) {
			Record record = null;
			try {
				record = inRecords.getRecord(currentNum);
				Asset newasset = updateAsset(inOECategory, record, guidToname,
						inArchive, inStatusLog, details, root);
				if (newasset != null) {
					unsavedassets.add(newasset);
				}
			} catch (Exception e) {
				errortotal++;
				if (errortotal > maxerrors) {
					if (e instanceof OpenEditException) {
						throw (OpenEditException) e;
					} else {
						String value = getValue(record.getFields()
								.getFieldByID(GUID.UID_REC_ASSET_NAME));
						String error = "Exception thrown from updateProduct: "
								+ e + " count: " + currentNum + " " + value
								+ " record ID:"
								+ record.getAssetReference().getAsXML();
						inStatusLog.add(error);
						// e.printStackTrace();
						log.error(error);
						throw new OpenEditException(e);
					}
				} else {
					logMessage(inStatusLog,
							"Exception thrown from updateAsset: " + e);
				}
			}
			numSuccessful++;
			count++;
			if (count == 600) {
				flushIndex(unsavedassets, inArchive, inStatusLog, false);
				double time = (double) (System.currentTimeMillis() - start) / 1000D / 60D;
				log.info("Populated " + numSuccessful + " records from "
						+ inOECategory.get("externalid") + " in "
						+ numSuccessful / time + " records/minute");
				count = 0;
				checkMounts(inArchive, inOECategory.getId(), unsavedassets);
				unsavedassets.clear();
			}
		}

		flushIndex(unsavedassets, inArchive, inStatusLog, true);

		checkMounts(inArchive, inOECategory.getId(), unsavedassets);
		unsavedassets.clear();
		double time = (double) (System.currentTimeMillis() - start) / 1000D / 60D;
		logMessage(inStatusLog, "Finished " + numSuccessful + " records from "
				+ inOECategory.get("externalid") + " in " + numSuccessful
				/ time + " records/minute");
	}

	protected void checkMounts(MediaArchive inArchive, String inRootCategoryId,
			List inAssets) {
		String automount = getSettings(inArchive).getChildValue("automount");
		if ("false".equals(automount)) {
			return;
		}
		String inCatalogId = inArchive.getCatalogId();
		Set<String> servers = new HashSet<String>();
		List finallist = new ArrayList();
		List existing = getPageManager().getRepository().getRepositories();
		finallist.addAll(existing);
		for (Iterator iterator = inAssets.iterator(); iterator.hasNext();) {
			Asset asset = (Asset) iterator.next();
			String url = asset.getProperty("originalpath");
			String serverroot = null;
			if (url.startsWith("\\\\")) {
				int count = url.indexOf("\\", 2);

				// servername/sharename/restofsourcepath

				count = url.indexOf("\\", count + 1);
				serverroot = url.substring(0, count);
			}
			if (url.charAt(1) == ':') // Looking for Z: drive
			{
				serverroot = url.substring(0, 2); // Include the :
			}
			if (serverroot == null) // Looking for Z: drive from SERVER (Z:)
			{
				int param = url.indexOf("(");
				if (param > -1) {
					int slash = url.indexOf("\\");
					if (slash != -1 && param > slash) {
						serverroot = url;
						int start = serverroot.indexOf("(");
						int end = serverroot.indexOf(")");
						serverroot = serverroot.substring(start + 1, end);
					}
				}
				// serverroot = url.substring(1,3);
			}
			if (serverroot == null) {
				log.info("unable to determine appropriate mount for: " + url);
				continue;
			}
			if (!servers.contains(serverroot)) {
				String cleanserver = null;
				if (serverroot.startsWith("\\\\")) {
					cleanserver = serverroot.substring(2);
					// get the server name and shorten it if it has periods in
					// it
					int servernameindex = cleanserver.indexOf('.');
					if (servernameindex != -1) {
						cleanserver = cleanserver.substring(0, servernameindex);
					}
				} else if (serverroot.endsWith(":")) {
					cleanserver = serverroot.substring(0, 1);
				}
				cleanserver = cleanserver.replace("\\", "/");
				String path = "/WEB-INF/data/" + inCatalogId + "/originals/"
						+ inRootCategoryId + "/" + cleanserver;

				// Check for duplicate internal paths
				boolean missing = true;
				for (Iterator iterator2 = finallist.iterator(); iterator2
						.hasNext();) {
					Repository config = (Repository) iterator2.next();
					// This needs to be equals, not equalsIgnoreCase
					if (path.equals(config.getPath())) {
						missing = false;
						break;
					}
				}
				if (missing) {
					Repository config = new FileRepository();
					config.setExternalPath(serverroot);
					config.setPath(path);
					config.setRepositoryType("fileRepository");
					finallist.add(config);
					getWebServer().saveMounts(finallist);

				}
			} else {
				servers.add(serverroot);
			}
		}
	}

	protected com.canto.cumulus.Category getTopCategory(MediaArchive inArchive,
			CatalogCollection col) throws CumulusException {
		com.canto.cumulus.Category startCat = null;

		String treeroot = getSettings(inArchive).getChildValue("treeroot");
		if (treeroot != null) {
			com.canto.cumulus.Category root = col.getCategories().getCategory(
					treeroot);
			startCat = findFirstValidCategory(root, inArchive);
		} else {
			startCat = findFirstValidCategory(col.getCatalogRootCategory(),
					inArchive);
		}
		return startCat;
	}

	protected String getCurrentTime(MediaArchive inArchive) {
		Calendar calendar = new GregorianCalendar();

		// yyyy-MM-dd HH:mm:ss
		// 2007-02-20 10:54:36 AM
		Configuration config = getSettings(inArchive);
		String format = config.getChildValue("lastmodifiedformat");
		String legacy = config.getChildValue("cumulus6");
		if (Boolean.parseBoolean(legacy)) {
			calendar.add(Calendar.HOUR, -2);
		}
		if (format == null) {
			// "yyyy-MM-dd HH:mm:ss" is the format Cumulus specifies in its
			// docs. should we use it?
			format = "MM/dd/yyyy hh:mm:ss a";
			// "yyyy-MM-dd hh:mm:ss a";
		}
		Date newdate = calendar.getTime();
		DateFormat formater = new SimpleDateFormat(format);
		String lastchecked = formater.format(newdate);
		return lastchecked;
	}

	public void exportFilterListData(
			com.canto.cumulus.Category inCatalogRootCategory,
			MediaArchive inArchive) throws Exception {
		XmlArchive xmlArchive = getXmlArchive();
		// use $Filters instead of categories
		// use Filters as well
		// match file names (Delete time stamp)
		com.canto.cumulus.Category filters = getCategory(
				inCatalogRootCategory.getSubCategories(), "$Filters");
		if (filters == null) {
			filters = getCategory(inCatalogRootCategory.getSubCategories(),
					"Filters");
		}
		if (filters != null) {

			for (int i = 0; i < filters.getSubCategories().countCategories(); i++) {
				com.canto.cumulus.Category filter = filters.getSubCategories()
						.getCategory(i);
				String id = extractId(filter.getName(), true);
				// String catalogName = inCatalogRootCategory.getName();

				String path = "/" + inArchive.getCatalogId()
						+ "/configuration/lists/" + id + ".xml";

				XmlFile file = xmlArchive.getXml(id, path, "filter");
				file.clear();
				int it = filter.getSubCategories().countCategories();
				for (int j = 0; j < it; j++) {
					com.canto.cumulus.Category entry = filter
							.getSubCategories().getCategory(j);
					String cid = extractId(entry.getName(), true);
					file.add(cid, entry.getName());
				}
				xmlArchive.saveXml(file, null);
			}
		}

	}

	/*
	 * com.canto.cumulus.Category keywords =
	 * inCatalogRootCategory.getSubCategories().getCategory("$Categories"); if(
	 * keywords != null) { com.canto.cumulus.Category filters =
	 * keywords.getSubCategories().getCategory("Filters");
	 * com.canto.cumulus.Category filters2 =
	 * keywords.getSubCategories().getCategory("$Filters"); if( filters != null)
	 * { for (int i = 0; i < filters.getSubCategories().countCategories(); i++)
	 * { com.canto.cumulus.Category filter =
	 * filters.getSubCategories().getCategory(i); String id =
	 * extractId(filter.getName(), true ); Types types =
	 * inArchive.getStore().getProperties(id); types.clear(); int it =
	 * filter.getSubCategories().countCategories(); for (int j = 0; j < it; j++)
	 * { com.canto.cumulus.Category entry =
	 * filter.getSubCategories().getCategory(j); String cid = extractId(id + "_"
	 * + entry.getName(), true); types.add(cid,entry.getName()); }
	 * inArchive.getStore().saveProperties(id,types, inArchive.getUser()); } } }
	 */

	protected void flushIndex(List inUnsavedassets, MediaArchive inArchive,
			ConvertStatus inStatusLog, boolean inOptimize)
			throws OpenEditException {
		for (Iterator iterator = inUnsavedassets.iterator(); iterator.hasNext();) {
			Asset newasset = (Asset) iterator.next();
			inArchive.getAssetArchive().saveAsset(newasset);
		}
		if (!inStatusLog.isReindex()) // Only update the index if we are not
										// reindexing later
		{
			inArchive.getAssetSearcher().updateIndex(inUnsavedassets,
					inOptimize);
			// inArchive.getStore().getStoreSearcher().clearIndex();
		}
		inArchive.getAssetArchive().clearAssets();
		// inUnsavedassets.clear();
	}

	protected void syncCatalogTree(com.canto.cumulus.Category inCategory,
			Category inParent, MediaArchive inArchive) throws Exception {
		copyOldData(inCategory, inParent, inArchive);
		inParent.setProperty("cumulusid", String.valueOf(inCategory.getID()));
		Categories subs = inCategory.getSubCategories();

		log.debug("reading in: " + inCategory.getName());

		for (int i = 0; i < subs.countCategories(); i++) {
			com.canto.cumulus.Category sub = subs.getCategory(i);

			String name = sub.getName();
			if (name != null) {
				name = name.replace('\n', ' ');
				name = name.replace('\r', ' ');
			}
			if ("$Filters".equals(name)) {
				name = "Filters";
			}
			if (ignoreCategory(inArchive, name)) {
				// syncCatalogTree(sub, inParent );
				continue;
			}
			if (hideCategory(inArchive, name)) {
				syncCatalogTree(sub, inParent, inArchive);
				continue;
			}
			String id = extractId(name, true);
			String fullId = inParent.getId() + "_" + id;
			Category child = inParent.getChild(fullId);
			if (child == null) {
				child = inParent.addChild(new Category(fullId, name));

			}
			// add the subs to the parent collection
			if (sub.getType() == com.canto.cumulus.Category.RelatedCategory) {
				com.canto.cumulus.Category orig = sub.getOriginalCategory();
				String catId = findOECategory(orig, inArchive);
				child.setLinkedToCategoryId(catId);
			}
			// log.info("Finished: " + fullId);
			// look for more children
			syncCatalogTree(sub, child, inArchive);
		}
	}

	protected com.canto.cumulus.Category findFirstValidCategory(
			com.canto.cumulus.Category inCategory, MediaArchive inArchive) {
		if (ignoreCategory(inArchive, inCategory.getName())) {
			return null;
		}
		if (hideCategory(inArchive, inCategory.getName())) {
			Categories subs = inCategory.getSubCategories();
			for (int i = 0; i < subs.countCategories(); i++) {
				com.canto.cumulus.Category sub = subs.getCategory(i);
				com.canto.cumulus.Category cat = findFirstValidCategory(sub,
						inArchive);
				if (cat != null) {
					return cat;
				}
			}
			return null; // if we skip inCategory and all of its children,
							// return null
		}
		return inCategory;
	}

	/*
	 * This copies category data over during the sync e.g. if someone has
	 * properties for the categories, we don't want to replace them during the
	 * sync, we want to add to them, so this copies the old data over.
	 */
	protected void copyOldData(com.canto.cumulus.Category inCat,
			Category inParent, MediaArchive inArchive) throws Exception {
		PropertyDetails details = inArchive.getPropertyDetailsArchive()
				.getPropertyDetailsCached("category");
		if (details.getDetails().size() > 0) {
			// here is is slower to loop over all the fields in a category
			// long start = System.currentTimeMillis();
			Fields all = inCat.getFields();
			if (all == null) {
				return;
			}
			// mysterious performance optimization if there are more than 3
			// details
			if (details.getDetails().size() > 3) {
				int count = 0;
				int total = all.countFields();
				for (int i = 0; i < total; i++) {
					Field field = all.getField(i);
					PropertyDetail detail = details.getDetailByExternalId(field
							.getFieldDefinition().getName());
					if (detail != null) {
						count++;
						if (detail.isStored()) {
							copyCategoryField(inParent, inArchive, field,
									detail);
							if (count > details.getDetails().size()) {
								break; // Done with all possible details
							}
						}
					}
				}
			} else {
				for (Iterator iter = details.getDetails().iterator(); iter
						.hasNext();) {
					PropertyDetail detail = (PropertyDetail) iter.next();
					if (detail.isStored() && detail.getExternalId() != null) {
						try {
							Field field = all.getField(detail.getExternalId());
							copyCategoryField(inParent, inArchive, field,
									detail);
						} catch (Exception e) {
							log.info("error getting field from cumulus.  External id was: "
									+ detail.getExternalId());
							throw (new OpenEditRuntimeException(
									"error getting field from cumulus.  External id was: "
											+ detail.getExternalId(), e));
						}

					}
				}
			}
			// long end = System.currentTimeMillis();
			// log.info("Completed copy in " + (end-start) + "mills ");
		}
		Category oldCopy = inArchive.getCategory(inParent.getId());
		if (oldCopy != null) {
			inParent.setProperties(oldCopy.getProperties()); // has existing
																// sort,
																// permissions,
																// etc.
		}
	}

	protected void copyCategoryField(Category inParent, MediaArchive inArchive,
			Field field, PropertyDetail detail) throws CumulusException,
			Exception {

		if (detail.isDataType("image")) {
			// This is just for categories
			Page img = getPageManager().getPage(
					inArchive.getCatalogHome() + "/categories/images/thumb/"
							+ inParent.getId() + "-100.jpg");
			if (!img.exists() && detail.getExternalId() != null) {
				if (field.hasValue()) {
					saveThumbnail(field, img);
				}
			}
		} else {
			String id = detail.getExternalId();
			if (id != null) {
				try {
					if (field.hasValue()
							&& detail.getId().equalsIgnoreCase("description")) {
						inParent.setShortDescription(getValue(field));
					}
				} catch (Exception ex) {
					throw new OpenEditException(ex);
				}
			}
		}
	}

	protected void saveThumbnail(Field inField, Page inPath) throws Exception {
		if (inField.hasValue()) {
			byte[] raw = (byte[]) inField.getValue();
			Pixmap nuPixmap = new Pixmap(raw);
			byte[] jpegData = nuPixmap.getAsJPEG(1500);
			// inPath.getParentFile().mkdirs();

			ByteArrayInputStream in = new ByteArrayInputStream(jpegData);
			BufferedImage image = ImageIO.read(in);

			double change = 1;

			int big = Math.max(image.getWidth(), image.getHeight());
			if (big > 150) {
				change = (double) 150 / (double) big;
				int width = (int) Math
						.round((double) image.getWidth() * change);
				int height = (int) Math.round((double) image.getHeight()
						* change);

				ImageToConvert convert = new ImageToConvert();
				convert.setWidth(width);
				convert.setHeight(height);
				convert.setImage(image);

				// getImageConvertQueue().add(convert);
				OutputStream out = getPageManager().saveToStream(inPath);
				convert.saveTo(out);
			} else {
				OutputStream out = null;
				try {
					out = getPageManager().saveToStream(inPath);
					out.write(jpegData);
				} finally {
					FileUtils.safeClose(out);
				}

			}
		}
	}

	protected void filterResults(CatalogCollection inCol,
			Category inOECategory, MediaArchive inArchive) throws Exception {
		// String topId = String.valueOf( cat.getID() );
		// Category subroot = inArchive.getStore().getCatalog(topId);
		// if(subroot == null)
		// {
		// subroot = inArchive.getStore().getCatalogArchive().addCatalog(
		// topId,cat.getName() );
		// }

		String lastModified = getSettings(inArchive).getChildValue(
				"lastmodifiedfield");
		if (lastModified == null) {
			lastModified = "Record Modification Date";
		}

		String lastChecked = inOECategory.getProperty("lastchecked");
		if (lastChecked != null) {
			// last = DateFormat.getDateTimeInstance().parse(lastChecked);
			// String formatted = DateFormat.getDateTimeInstance().format(last);
			// String formated = getCumulusFormat().format(last);
			String sQuery = lastModified + "\tafter\t" + lastChecked;
			// String sQuery =
			// "Record Modification Date\tafter\t10/17/2006 7:40:41 AM"; This
			// query gave the same results as cumulus

			log.info(sQuery);
			inCol.find(sQuery, Cumulus.FindNew);
		}
	}

	protected Asset updateAsset(Category inCategory, Record inRecord,
			Map inGuidToName, MediaArchive inArchive, ConvertStatus inStatus,
			PropertyDetails inDetails, Page inDestination) throws Exception {
		Fields fields = inRecord.getFields();
		Asset tempasset = new Asset();// inArchive.getStore().getAsset(id);
										// //might just want to create the asset

		// misc fields
		if (inGuidToName.size() == 0) {
			// We are going to assume the order of record fields will stay the
			// same
			// This allows us to avoid calling the very slow method
			// getField(String)
			int total = fields.countFields();
			// int total = fields.countFields();
			for (int i = 0; i < total; i++) // add all 160 fields in a map
			{
				Field field = fields.getField(i);
				String name = field.getFieldDefinition().getName();
				inGuidToName.put(name, new Integer(i));
				inGuidToName
						.put(field.getFieldUID().toString(), new Integer(i));
				//
				PropertyDetail detail = inDetails.getDetailByExternalId(name);
				if (detail != null && detail.isList()) {
					StringEnumList stringEnumList = null;
					int count = 0;
					try {
						stringEnumList = field.getFieldDefinition()
								.getStringEnumList();
						count = stringEnumList.countStrings();
					} catch (Throwable ex) {
						// Cumulus may not have a list for this type
						continue;
					}

					org.openedit.data.Searcher searcher = inArchive
							.getSearcherManager().getSearcher(
									inArchive.getCatalogId(), detail.getId());
					searcher.deleteAll(null);
					List all = new ArrayList();
					for (int j = 0; j < count; j++) {
						StringEnum val = stringEnumList.getString(j);
						Data data = searcher.createNewData();
						data.setId(String.valueOf(val.getID()));
						data.setProperty("name", val.getString());
						all.add(data);
					}
					searcher.saveAllData(all, null);
				}
			}
		}
		Integer i = (Integer) inGuidToName.get("PII site");
		if (i != null) {
			String pii = getValue(inRecord.getFields().getField(i.intValue()));
			if ("false".equals(pii)) {
				return null;
			}

		}
		Integer loc = (Integer) inGuidToName.get(GUID.UID_REC_ASSET_NAME
				.toString());
		String aname = getValue(fields.getField(loc.intValue()));
		if (aname == null) {
			log.info("Skipping file import: No name in Cumulus.");
			return null;
		}

		tempasset.setName(aname);
		String recid = null;
		boolean useAssetId = Boolean.parseBoolean(getSettings(inArchive)
				.getChildValue("legacyassetid"));
		if (useAssetId) {
			loc = (Integer) inGuidToName.get(GUID.UID_REC_ASSET_IDENTIFIER
					.toString());
			recid = getValue(fields.getField(loc.intValue()));
		} else {
			recid = String.valueOf(inRecord.getID());
		}

		// add the CatalogId to the assid?

		for (Iterator iterator = inDetails.getDetails().iterator(); iterator
				.hasNext();) {
			PropertyDetail detail = (PropertyDetail) iterator.next();
			String external = detail.getExternalId();
			if (external == null) { // This needs to be lower in the list of
									// details TODO: move to reindex code
				if ("orientation".equalsIgnoreCase(detail.getId())) {
					String height = tempasset.getProperty("imageheight"); // TODO:
																			// Externalize
																			// this
					String width = tempasset.getProperty("imagewidth");
					if (height != null && width != null) {
						double y = Double.parseDouble(height);
						double x = Double.parseDouble(width);
						if (y >= x) {
							tempasset.setProperty(detail.getId(), "Vertical");
						} else {
							tempasset.setProperty(detail.getId(), "Horizontal");
						}
					}
				}
				continue;
			}

			if (detail.isStored() || detail.isIndex() || detail.isKeyword()) {
				Field field = null;
				Integer locin = (Integer) inGuidToName.get(external);
				if (locin == null) {
					// log.error("No such field in Cumulus " + external);
					continue;
				}
				field = fields.getField(locin.intValue());
				// String name = field.getFieldUID().toString();
				copyAssetField(tempasset, detail, inRecord, field,
						inArchive.getCatalogId());
			}

		}
		convertCategories(inRecord, tempasset, inArchive);
		pullKeywords(inRecord, tempasset, inArchive);

		if (tempasset.getCategories().size() == 0) {
			tempasset.addCategory(inCategory);
		}

		// this is called after convertCategories because setAssetID may use the
		// asset's
		// default category to create a unique id
		setAssetID(inArchive, inCategory, tempasset, recid);

		// newasset.addProperty("assetidentifier", assid);

		// TODO: Fix it so we support & and [ ] in catalog names
		// String cumulusid = createCumulusID(inCategory.get("externalid"),
		// String.valueOf(inRecord.getID()));
		String cumulusid = inCategory.get("externalid") + "_"
				+ inRecord.getID();
		tempasset.setProperty("cumulusid", cumulusid);

		setAssetReferences(inArchive, inRecord, tempasset, inDestination,
				inStatus);

		String sourcepath = tempasset.getSourcePath();
		if (sourcepath == null) {
			// has an original path //server/drive/etc
			String url = tempasset.get("originalpath");
			sourcepath = getAssetSourcePathCreator(inArchive).createSourcePath(
					tempasset, url);
			sourcepath = inCategory.getId() + "/" + sourcepath;

			if (tempasset.getProperty("Series") != null) {
				String seriesnum = tempasset.getProperty("Series");
				// needs to be folder based
				String filename = PathUtilities.extractFileName(sourcepath);
				String directorypath = PathUtilities
						.extractDirectoryPath(sourcepath);

				if (directorypath.endsWith(seriesnum)) {
					sourcepath = directorypath;
					tempasset.setPrimaryFile(filename);
				}

			}

			tempasset.setSourcePath(sourcepath);
		}

		// Check if File Format has been set. If not, try to figure it out.
		tempasset.setProperty("fileformat", tempasset.getFileFormat());

		// Search for bad characters in the sourcepath. Log and ignore bad
		// files.
		FileUtils util = new FileUtils();
		if (!util.isLegalFilename(sourcepath)) {
			inStatus.add("Skipping file import due to bad sourcepath:"
					+ sourcepath);
			log.info("Skipping file import due to bad sourcepath:" + sourcepath);
			return null;
		}

		// TODO: handle the $Keywords field
		createImages(fields, tempasset, inGuidToName, inArchive);

		Asset existing = inArchive.getAssetBySourcePath(tempasset
				.getSourcePath());
		if (existing != null) {
			log.info("asset already existing - checking for changes");
			if (hasChanged(tempasset, existing)) {
				for (Iterator iterator = tempasset.getProperties().keySet()
						.iterator(); iterator.hasNext();) {
					String id = (String) iterator.next();
					String value = tempasset.get(id);
					existing.setProperty(id, value);
				}
				return existing;
			} else {
				return null;
			}
		}
		return tempasset;
	}

	private boolean hasChanged(Asset inTempasset, Asset inExisting) {
		for (Iterator iterator = inTempasset.getProperties().keySet().iterator(); iterator
				.hasNext();) {
			String id = (String) iterator.next();
			String value = inTempasset.get(id);
			String existing = inExisting.get(id);
			if(value != null && !value.equals(existing)){
				log.info("asset changed - old" + value + "new" + existing);
				
				return true;
			}
		}
		return false;
	}

	/**
	 * @param inArchive
	 * @param inRecord
	 * @param newasset
	 * @throws OpenEditException
	 */
	private void setAssetReferences(MediaArchive inArchive, Record inRecord,
			Asset newasset, Page inDestination, ConvertStatus inStatus)
			throws OpenEditException {
		AssetReference assetRef = inRecord.getAssetReference();
		for (int i = 0; i < assetRef.countParts(); i++) {
			AssetReferencePart part = assetRef.getPart(i);
			if (part.getName().equals("Windows")) {
				String path = part.getDisplayString();
				path = scrubChars(path); // Why do we scrub?
				newasset.setProperty("originalpath", path);
			} else if (part.getName().equals("Vault")) {
				String originalpath = part.getDisplayString();
				// this is the prefix for the file. Now we need to find the most
				// recent version
				String[] parts = originalpath.split(", ");
				// first part is the server name. Assume localhost
				// second part is the date
				// last part is the folder name. It is also the file name
				// then we need to find the most recent version
				log.info(originalpath + " is " + parts.length);

				String realpath = parts[1] + "/" + parts[2];
				String inPrefix = inDestination.get("vaultlocation"); // this
																		// would
																		// be a
																		// pointer
																		// to
																		// the
																		// vault
				if (inPrefix == null) {
					throw new OpenEditException(
							"Vault root path must be specified as the vaultlocation property in here: "
									+ inDestination + "/_site.xconf");
				}

				File dir = new File(inPrefix + "/" + realpath);

				String[] children = dir.list(new FilenameFilter() {
					public boolean accept(File inDir, String inName) {
						return inName.endsWith(".data");
					}
				});

				if (children != null && children.length > 0) {
					List files = Arrays.asList(children);
					Collections.sort(files);

					// might be in order already
					realpath = (String) files.get(files.size() - 1);
					// realpath = realpath.substring(inPrefix.length());
					try {
						realpath = new File(dir, realpath).getCanonicalPath();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					log.error("Vault based file missing from Vault "
							+ dir.getAbsolutePath());
					inStatus.add("Vault based file missing from Vault "
							+ dir.getAbsolutePath() + " on asset id "
							+ newasset.getId());
				}
				// TODO: Set the type of image this is for Image Magick
				newasset.setProperty("originalpath", realpath);
				String cumuluscatname = newasset.get("externalid");
				cumuluscatname = extractId(cumuluscatname, true);
				newasset.setSourcePath(cumuluscatname + "/" + parts[1].trim()
						+ "/" + newasset.getName().trim());

			} else if (part.getName().equals("PDF")) {
				String num = part.getDisplayString();
				num = num.substring("Page ".length());
				newasset.setProperty("pagenumber", num);
			}
		}
	}

	protected void setAssetID(MediaArchive inArchive, Category inCategory,
			Asset inAsset, String inRecordID) throws OpenEditException {
		String id = extractAssetId(inAsset.getName());
		String include = getSettings(inArchive).getChildValue(
				"includecatalogid");
		if (include == null) {
			throw new OpenEditException(
					"includecatalogid is a requires true or false in sync.xml");
		}
		boolean includecatalogid = Boolean.parseBoolean(include);
		if (includecatalogid) {
			// Used to make record really unique. Not needed by most people
			//
			id = id + "_" + inRecordID + "_"
					+ inCategory.getId().replace("_", "");
		} else {
			// Legacy
			id = id.replace("_", "und");
			id = id + inRecordID;
		}
		inAsset.setId(id);

	}

	protected void pullKeywords(Record inRecord, Asset inNewasset,
			MediaArchive inArchive) throws Exception {
		Categories cats = inRecord.getCategories();
		for (int i = 0; i < cats.countCategories(); i++) {
			com.canto.cumulus.Category c = cats.getCategory(i);
			boolean foundfilter = false;
			boolean keyword = false;
			// String filterGroup = null;
			// String value = null;
			List keywords = new ArrayList();
			while (c != null) // Start at Outdoor|Setting|$Filter
			{
				String name = c.getName();
				if ("$Filters".equals(name) || "Filters".equals(name)) {
					foundfilter = true;
					break;
				}
				// value = filterGroup; //Track filter last two values
				// filterGroup = name;

				if ("$Keywords".equals(name)) {
					keyword = true;
					break;
				}
				keywords.add(name);
				c = c.getParentCategory();
			}
			if (foundfilter || keyword) {
				if (foundfilter && keywords.size() > 1) // We found a pair of
														// filters
				{
					String value = (String) keywords.get(0); // Adult
					value = extractId(value, true);
					String group = (String) keywords.get(1); // Age range
					String id = extractId(group, true);
					String old = inNewasset.getProperty(id);
					if (old != null) {
						id = old + " " + value;
					}
					inNewasset.setProperty(id, value);
				}

				if (keyword) {
					for (Iterator iter = keywords.iterator(); iter.hasNext();) {
						String key = (String) iter.next();
						inNewasset.addKeyword(key);
					}
				}
			}
		}
	}

	private void copyAssetField(Asset newasset, PropertyDetail detail,
			Record inRecord, Field field, String inCatalogId)
			throws CumulusException {
		if (detail.isStored() || detail.isIndex() || detail.isKeyword()) {
			if (!field.hasValue()) {
				return;
				// continue;
			}
			String externaltype = detail.getExternalType();
			String val = null;
			if (detail.isDate()) {
				Object date = (Object) field.getValue();

				if ("intdate".equals(externaltype)) {
					String datestring = String.valueOf(date);
					try {
						date = (Date) fieldIntDateFormat.parse(datestring);
					} catch (ParseException e) {
						log.error("Invalid date format " + detail.getId() + " "
								+ date + " in format intdate");
					}

				}
				if (date instanceof Date) {
					val = DateStorageUtil.getStorageUtil().formatForStorage(
							(Date) date);
				} else {
					val = date.toString();
				}
			} else if ("linkcategory".equals(externaltype)) {
				// handle a link category
				String catlink = getValue(field);
				// look up category..
				int cid = Integer.parseInt(catlink);
				com.canto.cumulus.Category cat = inRecord.getCategories()
						.getCategoryByID(cid);
				if (cat != null) {
					val = cat.getName();
				} else {
					log.error("linkcategory not found by id " + cid);
				}
			} else if ("fileformat".equals(detail.getId())) {
				String check = getValue(field);
				if (check != null) {
					val = null;
					XmlFile file = getFileFormats(inCatalogId);
					for (Iterator iterator = file.getRoot().elementIterator(); iterator
							.hasNext();) {
						Element format = (Element) iterator.next();
						String text = format.getTextTrim();
						if (check.toLowerCase().startsWith(text.toLowerCase())) // TIFF
																				// Image
						{
							val = format.attributeValue("id"); // Found it
						} else {
							String external = format
									.attributeValue("externalid");
							if (external != null && external.equals(val)) {
								val = format.attributeValue("id");
							} else {
								if (check
										.equalsIgnoreCase("Illustrator Drawing")) {
									val = "ai";
								} else if (check
										.equalsIgnoreCase("Encapsulated PostScript")) {
									val = "eps";
								} else if (check
										.equalsIgnoreCase("Portable Network Graphic")) {
									val = "png";
								} else if (check
										.equalsIgnoreCase("Portable Document Format")) {
									val = "pdf";
								} else if (check
										.equalsIgnoreCase("Bitmap Image")) {
									val = "bmp";
								} else if (check.toLowerCase().contains(
										"photoshop")) {
									val = "psd";
								} else if (check.toLowerCase().contains(
										"quarkxpress")) {
									val = "qxd";
								}
							}
						}
						if (val != null) {
							break;
						}
					}
				}
			} else {
				val = getValue(field);
			}
			if (val != null && val.length() > 0) {
				newasset.setProperty(detail.getId(), val);
			}
		}
	}

	protected void createImages(Fields inFields, Asset inNewasset, Map inIndex,
			MediaArchive inArchive) throws Exception {
		CreatorManager imageCreator = inArchive.getCreatorManager();

		Page thumb = imageCreator.getThumbImageFile(inNewasset.getSourcePath());

		if (!thumb.exists() && !getCumulusConnectionPool().isSkipThumbnails()) {
			// Fields nuFields = inRecord.getFields();
			Integer indexloc = (Integer) inIndex.get(GUID.UID_REC_THUMBNAIL
					.toString()); // Performance trick
			Field nuField = inFields.getField(indexloc.intValue());
			saveThumbnail(nuField, thumb);
		}

		Page med = imageCreator.getMediumImageFile(inNewasset.getSourcePath());
		if (!med.exists() && !getCumulusConnectionPool().isSkipMediums()) {
			Integer indexloc = (Integer) inIndex.get(GUID.UID_REC_THUMBNAIL
					.toString());
			Field nuField = inFields.getField(indexloc.intValue());
			if (nuField.hasValue()) {
				Pixmap nuPixmap = new Pixmap((byte[]) nuField.getValue());
				byte[] jpegData = nuPixmap.getAsJPEG();
				OutputStream out = getPageManager().saveToStream(med);
				try {
					out.write(jpegData);
				} finally {
					FileUtils.safeClose(out);
				}
			}
		}
	}

	protected void convertCategories(Record inRecord, Asset inNewasset,
			MediaArchive inArchive) throws Exception {
		Categories cats = inRecord.getCategories();
		for (int i = 0; i < cats.countCategories(); i++) {
			com.canto.cumulus.Category c = cats.getCategory(i);

			String catId = findOECategory(c, inArchive);
			if (catId == null) {
				continue;
			}
			Category cat = inArchive.getCategory(catId);

			if (cat == null) {
				log.error("Skipping catalog " + catId);
				// throw new OpenEditException("Invalid category specified " +
				// catId);
			} else {
				inNewasset.addCategory(cat);
			}
		}
	}

	protected String findOECategory(com.canto.cumulus.Category c,
			MediaArchive inArchive) {
		StringBuffer fullId = new StringBuffer();
		while (c != null) {
			String name = c.getName();
			if ("$Filters".equals(name)) {
				name = "Filters";
			}
			// log.info(name + " Level" + c.getLevel() );
			// make sure no parent is being ignored i.e. $Keywords
			if (ignoreCategory(inArchive, name)) {
				return null;
			}
			if (!hideCategory(inArchive, name)) {
				name = name.replace('\n', ' ');
				name = name.replace('\r', ' ');
				String fid = extractId(name, true);
				// log.info( "Catid: " + c.getID());
				if (fullId.length() > 0) {
					fullId.insert(0, "_");
				}
				fullId.insert(0, fid);
			}
			c = c.getParentCategory();
		}
		String catId = fullId.toString();
		return catId;

	}

	public SimpleDateFormat getCumulusFormat() {
		return fieldCumulusFormat;
	}

	public void setCumulusFormat(SimpleDateFormat inCumulusFormat) {
		fieldCumulusFormat = inCumulusFormat;
	}

	protected boolean hasParent(com.canto.cumulus.Category inCat,
			String parentName) {
		com.canto.cumulus.Category cat = inCat;
		while (cat != null) {
			String name = cat.getName();
			if (name.equals(parentName)) {
				return true;
			}

			cat = cat.getParentCategory();
		}
		return false;
	}

	public TaskRunner getOddRunner() {
		if (fieldOddRunner == null) {
			fieldOddRunner = new TaskRunner();
		}
		return fieldOddRunner;
	}

	public TaskRunner getEvenRunner() {
		if (fieldEvenRunner == null) {
			fieldEvenRunner = new TaskRunner();
		}
		return fieldEvenRunner;
	}

	public XmlArchive getXmlArchive() {
		return fieldXmlArchive;
	}

	public void setXmlArchive(XmlArchive inXmlArchive) {
		fieldXmlArchive = inXmlArchive;
	}

	protected XmlFile getFileFormats(String inCatalogId) {
		if (fieldFileFormats == null) {
			fieldFileFormats = new HashMap();
		}
		XmlFile format = (XmlFile) fieldFileFormats.get(inCatalogId);
		if (format == null) {
			format = getXmlArchive().getXml(
					"/" + inCatalogId + "/configuration/lists/fileformat.xml");
			fieldFileFormats.put(inCatalogId, format);
		}
		return format;
	}

}