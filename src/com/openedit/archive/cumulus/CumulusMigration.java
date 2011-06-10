package com.openedit.archive.cumulus;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.MediaArchive;

import com.canto.cumulus.AssetReference;
import com.canto.cumulus.AssetReferencePart;
import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.Field;
import com.canto.cumulus.Fields;
import com.canto.cumulus.Record;
import com.canto.cumulus.Records;
import com.canto.cumulus.ServerCatalog;
import com.openedit.util.FileUtils;
import com.openedit.util.PathUtilities;

public class CumulusMigration extends BaseCumulusConvert
{
	private static final Log log = LogFactory.getLog(CumulusMigration.class);
	
	public void migrate(MediaArchive inStore, ConvertStatus inLog, String inCumulusCatalog, String inStart, String inMax) throws Exception
	{
		long start = System.currentTimeMillis();
		ServerCatalog cumulusCat = getServerCatalogs().getServerCatalog(inCumulusCatalog);
		CatalogCollection col = cumulusCat.open();
		
		//String sQuery = "Record Modification Date" + "\tafter\t" + "2009-10-26 00:00:00";
		
		//col.find(sQuery, Cumulus.FindNew);
		
		
		//loop over each record found
		Records records = col.getRecords();
		int c = records.countRecords();
		
		if( inMax != null)
		{
			c = Math.min(c,  Integer.parseInt(inMax) );
		}
		
		int startIndex = 0;
		if(inStart != null)
		{
			startIndex = Integer.parseInt(inStart);
			if(startIndex < 0)
			{
				startIndex = 0;
			}
		}
		
		FileUtils futil = new FileUtils();
		int noassetref =0;
		int noseries = 0;
		int filenamesame = 0;
		int seriesok = 0;
		int nofileexist = 0;
		int movedok = 0;
		
		for (int i = startIndex; i < c; i++)
		{
			if(i%200 == 0)
			{
				double time = (double)(System.currentTimeMillis() - start)/1000D/60D;
				log.info("Processed " + i + "/" + c + " records in " + time + " minutes.  noassetref: " + 
						noassetref + 
						" noseries: " + noseries +
						" filenamessame: " + filenamesame +
						" seriesok: " + seriesok +
						" nofileexist: " + nofileexist +
						" Moved " + movedok + " files.");
			}
			Record record = records.getRecord(i);

			AssetReference assetRef = record.getAssetReference();
			String fullpath = null;
			for (int a = 0; a < assetRef.countParts(); a++)
			{
				AssetReferencePart part = assetRef.getPart(a);
				if ( part.getName().equals("Windows"))
				{
					String path = part.getDisplayString();
					path = scrubChars(path); //Why do we scrub?
					fullpath = path;	
					break;
				}
			}
			if( fullpath == null)
			{
				noassetref++;
				continue;
			}
			Fields fields = record.getFields();
			Field field = fields.getField("Series");
			String series = getValue(field);
			if( series == null)
			{
				noseries++;
				continue;
			}
			series = series.trim();
			
			String path = fullpath.replace('\\', '/');
			String filename = PathUtilities.extractFileName(path);
			if( series.equalsIgnoreCase(filename))
			{
				filenamesame++;
				continue;
			}

			String folder = PathUtilities.extractDirectoryName(path);
			if( folder.equals(series))
			{
				seriesok++;
				continue;
			}
			File file = new File( fullpath);
			if( !file.exists())
			{
				nofileexist++;
				continue;
			}
			String root = PathUtilities.extractDirectoryPath(path);
			File dest = new File( root + "/" + series + "/");
			log.info("Making a dir for row " + i + ":" + dest);
			dest.mkdir();
			if( dest.exists() )
			{
				log.info("Moving " + i + " " + file.getAbsolutePath());
				try
				{
					record.moveAssetTo(dest);									
				}
				catch( Exception ex)
				{
					log.info("Could not moveAsset on: " + file.getAbsolutePath() + " " + ex);
					nofileexist++;
				}
			}
			else
			{
				log.info("Could not create folder: " + dest );
				nofileexist++;
			}
			//futil.move(file,dest);
			//log.info("Update " + dest.getAbsolutePath());
			//record.setAssetReference(dest);
			//log.info("Save " + i);
			//record.updateAssetReference(); //I hope this not needed
			//record.save();
			movedok++;
		}
		inLog.add(" No asset reference:" + noassetref );
		inLog.add(" No Series name set:" + noseries );
		inLog.add(" Filename is same as series name:" + filenamesame );
		inLog.add(" Series aleady same as folder name:" + seriesok );
		inLog.add(" No asset exists:" + nofileexist );
		inLog.add(" Migrated Ok:" + movedok );
		
	}
	

	public void importAssets(MediaArchive inStore, ConvertStatus inErrorLog) throws Exception
	{
		// TODO Auto-generated method stub

	}

}
