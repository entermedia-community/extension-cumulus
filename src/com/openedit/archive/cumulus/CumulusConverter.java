/*
 * Created on Sep 15, 2004
 */
package com.openedit.archive.cumulus;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.openedit.data.PropertyDetail;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.CatalogConverter;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.MediaArchive;
import org.openedit.entermedia.SourcePathCreator;

import com.openedit.OpenEditException;
import com.openedit.OpenEditRuntimeException;
import com.openedit.config.Configuration;
import com.openedit.config.XMLConfiguration;
import com.openedit.page.Page;
import com.openedit.util.FileUtils;
import com.openedit.util.OutputFiller;
import com.openedit.util.XmlUtil;


/**
 * A catalog converter that converts Cumulus tab separated values and images into products
 * @author cburkey
 */
public class CumulusConverter extends CatalogConverter
{
    protected OutputFiller fieldOutputFiller;
	protected Map fieldSettingsMap;
    private static final Log log = LogFactory.getLog(CumulusConverter.class);
    
    public CumulusConverter()
	{
	}
    public synchronized void importAssets(MediaArchive inArchive, ConvertStatus inErrorLog) throws Exception
    {
    	//Set the read 
    	List paths = getPageManager().getChildrenPaths(inArchive.getCatalogHome() + "/import/");
    	for (Iterator iterator = paths.iterator(); iterator.hasNext();)
		{
			String path = (String) iterator.next();
			if( path.toLowerCase().endsWith(".cre") )
			{
				Page input = getPageManager().getPage(path);
				if (input.length() == 0)
		        {
		            log.debug("input empty, skipping conversion");
		            continue;
		        }
		        inErrorLog.add("Starting Cumulus inventory import");
	            processFile(input,inErrorLog.getLog(), inArchive);
			}
		}
    }


	private void processFile(Page input, List inLog, MediaArchive inArchive) throws Exception
	{
		inLog.add("Loading " + input.getName());
        log.info("processing... " + input);
        
       // Set toDelete = listAssetsToDelete(input.getName(), inArchive);

        ImportFile inputf = new ImportFile();
        
        inputf.load(input.getReader());
        int total = 0;
        Row row = null;
        try
        {
	        row = inputf.getNextRow();
	        int count = 0;
	        while ( row != null)
	        {
	            Asset prod = createAsset(row, inArchive);
	           // toDelete.remove(prod.getId());
	            row = inputf.getNextRow();
	            count++;
	            total++;
	            if ( count > 1000)
	            {
	            	log.info("Imported " + total);
	            	count = 0;
	            	inArchive.getCategoryArchive().saveAll();
	            }
	        }
        }
	    catch( Throwable ex )
	    {
	    	log.error("Problem processing row on row " +  total + " " + row.getData(),ex);
	    	if( ex instanceof OpenEditRuntimeException )
	    	{
	    		throw (OpenEditRuntimeException)ex;
	    	}
	    	throw new OpenEditException("On row: " + total , ex);
        }
	    finally
        {
        	inputf.close();
        }
        inArchive.getCategoryArchive().saveAll();
        Page backup = getPageManager().getPage(inArchive.getCatalogHome() + "/import/completed/" + input.getName() );
        getPageManager().movePage(input, backup);
        
      //  deleteAssetsById(toDelete, inArchive);
	}

//	protected void deleteAssetsById(Set inToDelete, MediaArchive inArchive) throws Exception
//	{
//		for (Iterator iter = inToDelete.iterator(); iter.hasNext();)
//		{
//			String id = (String) iter.next();
//			Asset prod = inArchive.getAsset(id);
//			if ( prod != null)
//			{
//				inArchive.getAssetArchive().deleteAsset(prod);
//			}
//		}
//	}
	
//	protected Set listAssetsToDelete(String inName, MediaArchive inArchive) throws Exception
//	{		
//        String catid = extractId( PathUtilities.extractPageName( inName ),true );
//        SearchQuery query = inArchive.getAssetSearcher().createSearchQuery();
//        
//        query.addMatches("catalogs", catid);
//        HitTracker hits = inArchive.getSearcherManager().getSearcher(inArchive.getCatalogId(), "product").search(query); //this would be the top level catalog
//        HashSet set = new HashSet();
//        for (int i = 0; i < hits.size(); i++)
//		{
//			String pid = hits.getValue(hits.get(0), "id");
//			set.add(pid);
//		}
//		return set;
//	}
	protected Asset createAsset(Row row, MediaArchive inArchive) throws Exception
	{
		String name = row.getData(getFieldName(inArchive,"Asset Name") );
		if ( name.equalsIgnoreCase("thumbs.db") || name.equalsIgnoreCase("desktop.ini"))
		{
			return null;
		}
		Asset newproduct = new Asset();

		String assid = row.getData(getFieldName(inArchive,"Asset Identifier"));

		//need to get the second to top catalog
		String id = extractAssetId(name ) + assid;		
		newproduct.setId(id);		
		newproduct.setProperty("assetidentifier", assid);

		convertOriginalLink(inArchive,newproduct,row);
		//newproduct.setOrdering(i); //natual ordering

		newproduct.setName(name);
		convertCatalogs(row, newproduct, inArchive);

		//misc fields
		for (Iterator iter = inArchive.getAssetPropertyDetails().getDetails().iterator(); iter.hasNext();)
		{
			PropertyDetail  detail = (PropertyDetail ) iter.next();
			if( detail.isKeyword() || detail.isStored() )
			{
				int index = row.getHeader().getIndex(detail.getExternalId());
				if( index == -1 || index >  (row.getData().length -1 ))
				{
					//log.debug("No such colum" + detail.getExternalId());
					continue;
				}
				String val  = row.getData(index);
				if ( val != null)
				{
//						if ( detail.isDate() && val.length() > 5)
//						{
//							Date date = fieldCumulusSyncFormat.parse(val);
//							val = detail.getDateFormat().format( date );
//						}
					if( detail.isStored() )
					{
						newproduct.setProperty(detail.getId(),val);
					}
				}
			}
		}
		
	//	createImages(row, newproduct, inArchive);
		
		inArchive.getAssetArchive().saveAsset( newproduct );

		inArchive.getAssetArchive().clearAsset(newproduct);
		return newproduct;
	}

	private void convertOriginalLink(MediaArchive inArchive, Asset inNewproduct, Row inRow)
	{
		String server = inRow.getData(getFieldName(inArchive,"Windows File Server Name") );
		if ( server == null)
		{
			server = inRow.getData(getFieldName(inArchive,"Server Name") );
		}
		
		String volume = inRow.getData(getFieldName(inArchive,"Windows Volume Name") );
		if ( volume == null)
		{
			volume = inRow.getData(getFieldName(inArchive,"Volume Name") );
		}
		String folderName = inRow.getData(getFieldName(inArchive,"Windows Folder Names"));
		if ( folderName == null)
		{
			folderName = inRow.getData(getFieldName(inArchive,"Folder Name"));
		}
		String displaypath =  "";
		if ( server != null && server.length() > 0)  //we do not support local G:\ imports
		{
			displaypath = "\\" + server;
		}
		if( volume != null && volume.length() > 0)
		{
			displaypath = displaypath + "\\" + volume;
		}
		if( folderName != null && folderName.length() > 0)
		{
				displaypath = displaypath + "\\" + folderName;
		}
		/*	if ( File.separatorChar != '\\') //only change in Linux
		 {
		 	folderName = folderName.replaceAll( "\\\\", File.separator );
	     }
	    */
		String fileName = inRow.getData(getFieldName(inArchive,"Asset Name"));
		displaypath = displaypath + "\\" + fileName;
		//inNewproduct.addKeyword(fileName);
		inNewproduct.setProperty("originalpath",displaypath );
		String sourcepath = getAssetSourcePathCreator(inArchive).createSourcePath(inNewproduct, displaypath);

		inNewproduct.setSourcePath(sourcepath);
		
	}
	public SourcePathCreator getAssetSourcePathCreator(MediaArchive inMediaArchive)
	{
		return (SourcePathCreator) inMediaArchive.getModuleManager().getBean(inMediaArchive.getCatalogId(), "sourcepathcreator");
	}

	private void createImages(Row row, Asset newproduct, MediaArchive inArchive) throws Exception
	{
		Page medium = inArchive.getCreatorManager().getMediumImageFile(newproduct.getSourcePath());
		Page thumb = inArchive.getCreatorManager().getThumbImageFile(newproduct.getSourcePath());
		//thumbnails
		if ( !medium.exists() )
		{
			String code = row.getData(getFieldName(inArchive,"Thumbnail"));
			String junk = "5444617442797465"; //some hex junk at the begining of the files
			int cut = code.indexOf(junk);
			code = code.substring(cut+junk.length());
			if ( code.startsWith("FFD8FFE0")) //jpg only
			{	//other formats are unknown	
				byte[] bytes = new HexToBinaryConverter().hexToBinary( code );
				OutputStream fout = getPageManager().saveToStream(medium);
				try
				{
					fout.write(bytes);
				}
				finally
				{
					FileUtils.safeClose(fout);
				}
			}
			if (medium.exists() && !thumb.exists() )
			{
				getPageManager().copyPage(medium,thumb);
			}
		}
	}

	protected void convertCatalogs(Row row, Asset newproduct, MediaArchive inArchive)
	{
		//take everything off the end and update the catalog XML tree
		String[] categories = row.getRemainder();
		newproduct.clearCategories();
		for (int j = 0; j < categories.length; j++)
		{
			String[] pairs = categories[j].split(":");
			Category parent = null;

			Category oldParent = null;
			for (int i = 0; i < pairs.length; i++)
			{
				String catalogParent = pairs[i].trim();
				
				if ( catalogParent.equalsIgnoreCase(getFieldName(inArchive,"$Keywords" )) )
				{
					i++; //SKIP to next
					newproduct.addKeyword(pairs[i] );
					continue;
				}
				if ( catalogParent.startsWith("$")) //we can include $Keywords tho
				{
					catalogParent = catalogParent.substring(1);
				}
				if (catalogParent.length() == 0 || 
					catalogParent.equalsIgnoreCase(getFieldName(inArchive,"Sources")) 
					//||
//					catalogParent.equalsIgnoreCase("PDRKOPCML01") ||  //TODO: Remove these checks 
//					catalogParent.equalsIgnoreCase("Pdrmakopcml01") || 
//					catalogParent.equalsIgnoreCase("YB Image Library")
				)
				{
					continue; //lets start a little lower and skip some large groups
				}
				//TODO: Try out http://www.koders.com/java/fidF83F5AC43A0CF80F83664D3FB590701A62EEC550.aspx for performance
				//vs http://www.regular-expressions.info/java.html

				newproduct.addKeyword(catalogParent );

				//name must be lucene and url friendly
				String catalogParentId = extractId(catalogParent, true);
				
				String 	pid = null;
				if ( oldParent != null )
				{
					pid = oldParent.getId() + catalogParentId; //give us a unique id					
				}
				else
				{
					pid = catalogParentId; //give us a unique id					
				}
				//pid = pid.replaceAll("__","_"); //undo having so many __ in there

				parent = inArchive.getCategory(pid);
				if ( parent == null)
				{
					parent = new Category();
					parent.setId(pid);
					parent.setName(catalogParent);
					if ( oldParent == null)
					{
						oldParent = inArchive.getCategoryArchive().getRootCategory(); //top level each time?
					}
					oldParent.addChild(parent);
				}
				newproduct.addCategory(parent);
				inArchive.getCategoryArchive().cacheCategory(parent); //put it into the cache
				oldParent = parent;
			}
		}
	}
	
	public String getFieldName(MediaArchive inArchive, String inField)
	{
		Configuration config = getSettings(inArchive);
		if( config == null)
		{
			return inField;
		}
		Configuration child = config.getChild("fields");
		if( child == null)
		{
			return inField;
		}
		for (Iterator iterator = child.getChildren().iterator(); iterator.hasNext();)
		{
			Configuration field = (Configuration) iterator.next();
			String name = field.getAttribute("id");
			if( inField.equals(name ) )
			{
				return field.getValue();
			}
		}
		String val = child.getChildValue(inField);
		if( val == null)
		{
			return inField;
		}
		return val;
	}
	public Configuration getSettings(MediaArchive inArchive)
	{
		Configuration settings = (Configuration)getSettingsMap().get(inArchive.getCatalogId());
		if (settings == null)
		{
			Page settingsFile = getPageManager().getPage(inArchive.getCatalogHome() + "/configuration/sync.xml");
			
			if( settingsFile.exists())
			{
				settings = new XMLConfiguration(new XmlUtil().getXml(settingsFile.getReader(),
				"UTF-8"));
			}
			else
			{
				settings = new XMLConfiguration(DocumentHelper.createElement("cumulus"));
			}
			getSettingsMap().put(inArchive.getCatalogId(), settings);
		}
		
		return settings;
	}

	public Map getSettingsMap()
	{
		if (fieldSettingsMap == null)
		{
			fieldSettingsMap = new HashMap();
		}
		return fieldSettingsMap;
	}
	

}