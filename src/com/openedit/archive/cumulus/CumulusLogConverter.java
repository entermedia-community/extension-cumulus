/*
 * Created on Mar 28, 2006
 */
package com.openedit.archive.cumulus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.MediaArchive;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.page.Page;
import com.openedit.util.FileUtils;

public class CumulusLogConverter extends BaseCumulusConvert
{
	private static final Log log = LogFactory.getLog(CumulusLogConverter.class);
	public void importAssets(MediaArchive inStore, ConvertStatus inStatus) throws Exception
	{
		deleteFromLogs(inStore, inStatus);
	}	
	protected void deleteFromLogs(MediaArchive inArchive, ConvertStatus inStatus) throws Exception
	{
		//fieldArchive.setSetting(null); ///causes a reload

		//list all the files in the logs dir
		String dir = getSettings(inArchive).getChildValue("deletelogs-directory");
		if ( dir != null)
		{			
			File[] all = new File( dir ).listFiles(new FilenameFilter()
				{
				public boolean accept(File inDir, String inName)
				{
					return inName.toLowerCase().endsWith(".log");
				}
			});
			
			if ( all == null)
			{
				logMessage(inStatus, "No log files found in " + dir);
				return;
			}
			for (int i = 0; i < all.length; i++)
			{
				File filelog = all[i];
				logMessage(inStatus, "Processing " + filelog);

				processDeleteLog(filelog, inArchive, inStatus);
				
				Calendar limit = GregorianCalendar.getInstance();
				limit.add(Calendar.DATE, -1);
				Calendar lastModified = new GregorianCalendar();
				lastModified.setTime(new Date(filelog.lastModified()));
				
				if (lastModified.before(limit))
				{
					File copy = new File( filelog.getParentFile(), "old/" + filelog.getName() );
					new FileUtils().copyFiles(filelog, copy);
					if (!filelog.delete()) //this may not delete it if it is still open for writing
					{
						logMessage(inStatus, "Could not delete file " + filelog.getAbsolutePath());
					}
				}
			}
		}
		
	}
	protected void processDeleteLog(File inLog, MediaArchive inArchive, ConvertStatus inStatus) throws FileNotFoundException, IOException, Exception
	{
		BufferedReader reader = new BufferedReader(new FileReader(inLog));
		try
		{
			String line = reader.readLine();
			int count = 0;
			while ( line != null)
			{
				String[] tabs = line.split("\t");
				if ( tabs.length > 3)
				{
					if ( "Record deleted".equals( tabs[3] ) )
					{
						String catName = inLog.getName();
						if(catName.indexOf('-')!=-1)
						{
								    catName = catName.substring(0,catName.indexOf('-'));
						}
						else if (catName.indexOf('.')!=-1)
						{
							catName = catName.substring(0,catName.lastIndexOf('.'));
						}
						String recordId = tabs[4];
						String cumulusid = catName + "_" + recordId; ///createCumulusID(catName, recordId);
						
						//catName = extractId(catName);
						//Category root = inArchive.getStore().getCatalogArchive().getRootCatalog().getChildByName(catName);
						SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
						query.addExact("cumulusid", cumulusid);
						HitTracker hits = inArchive.getAssetSearcher().search(query);
						if( hits.getTotal() > 0)
						{
							count++;
							String id = hits.get(0).get("id");
							deleteAsset(inArchive, id);
						}
						else
						{
							log.debug("No record found " + catName + "dash" + recordId);
						}
					}
				}
				line = reader.readLine();
			}
			if( count > 0)
			{
				inArchive.getAssetSearcher().flush();
				logMessage(inStatus, "Removed " + count + " records");
			}
		}
		finally
		{
			FileUtils.safeClose( reader );
		}
	}
	/**
	 * @param inArchive
	 * @param inAssetID
	 * @throws StoreException
	 */
	protected void deleteAsset(MediaArchive inArchive, String inAssetID)
	{
		//String id = findAssetId(tabs[5],tabs[4],catName);
		
		Asset product = inArchive.getAsset(inAssetID);
		if ( product == null)
		{
			inArchive.getAssetSearcher().deleteFromIndex(inAssetID); //Just in case index is out of date
		}
		else
		{
			String sourcePath = product.getSourcePath();
			Page thumb = inArchive.getCreatorManager().getThumbImageFile(sourcePath);
			Page medium = inArchive.getCreatorManager().getMediumImageFile(sourcePath);
			getPageManager().removePage(thumb);
			getPageManager().removePage(medium);
		
			inArchive.getAssetSearcher().deleteFromIndex(inAssetID);
			inArchive.getAssetArchive().deleteAsset(product);
			inArchive.getAssetArchive().clearAsset(product);
		}
	}

	/*
	protected String findAssetId(String inName, String inRecordId, String inRootCatName) throws Exception
	{
		ServerCatalog cat = getServerCatalogs().getServerCatalog(inRootCatName);
		CatalogCollection col = cat.open();
		Record record = col.getRecords().getRecordByID(Integer.parseInt( inRecordId ) );
		Fields field = record.getFields();
		String name = getValue(field.getField("Asset Name") );
		String assid = getValue(field.getField("Asset Identifier"));

		
		return extractAssetId(name, assid);
	}
*/
	

}
