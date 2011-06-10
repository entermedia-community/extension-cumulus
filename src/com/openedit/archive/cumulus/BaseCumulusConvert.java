/*
 * Created on Mar 24, 2006
 */
package com.openedit.archive.cumulus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentHelper;
import org.openedit.entermedia.CatalogConverter;
import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.EnterMedia;
import org.openedit.entermedia.MediaArchive;

import com.canto.cumulus.Categories;
import com.canto.cumulus.Field;
import com.canto.cumulus.Fields;
import com.canto.cumulus.ServerCatalogs;
import com.openedit.OpenEditRuntimeException;
import com.openedit.config.Configuration;
import com.openedit.config.XMLConfiguration;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.XmlUtil;


public abstract class BaseCumulusConvert extends CatalogConverter
{
	private static final Log log = LogFactory.getLog(BaseCumulusConvert.class);
	protected PageManager fieldPageManager;
	protected Map fieldCumulusCategories;
	protected EnterMedia fieldEnterMedia;
	protected Map fieldSettingsMap;
	
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	protected MediaArchive getMediaArchive( String inCatalogId)
	{
		return getEnterMedia().getMediaArchive(inCatalogId);
	}
	protected CumulusConnectionPool fieldCumulusConnectionPool;
	
	protected Configuration openSettings(String inCatId, Configuration inConfig)
	{
		for (Iterator iter = inConfig.getChildIterator("catalog"); iter.hasNext();)
		{
			Configuration config = (Configuration) iter.next();
			if ( inCatId.equals( config.getAttribute("externalid")) )
			{
				return config;
			}
		}
//		Configuration catconfig = inConfig.addChild("catalog");		
//		catconfig.setAttribute("id",inCatId);
		
		return null;
	}
	protected com.canto.cumulus.Category getCategory(Categories inSubCategories, String inName)
	{
		for (int i = 0; i < inSubCategories.countCategories(); i++)
		{
			com.canto.cumulus.Category cat = inSubCategories.getCategory(i);
			if( inName.equals(cat.getName()))
			{
				return cat;
			}
		}
		return null;
	}
	
	/*
	 * Will first look in the map for the specified category
	 * If not found, will then look in the tree
	 */
	protected com.canto.cumulus.Category findCategory(Categories inCategories, int inId)
	{
		com.canto.cumulus.Category cat = (com.canto.cumulus.Category)getCumulusCategories().get(new Integer(inId));
		if (cat == null)
		{
			cat = findCategoryInTree(inCategories, inId);
			getCumulusCategories().put(new Integer(inId), cat);
		}
		return cat;
	}
	
	/*
	 * Recursively looks in the tree for the specified category
	 */
	protected com.canto.cumulus.Category findCategoryInTree(Categories inCategories, int inId)
	{
		for (int i = 0; i < inCategories.countCategories(); i++)
		{
			com.canto.cumulus.Category cat = inCategories.getCategory(i);
			if( inId == cat.getID())
			{
				return cat;
			}
			else
			{
				cat = findCategoryInTree(cat.getSubCategories(), inId);
				if (cat != null)
				{
					return cat;
				}
			}
		}
		return null;
	}

	public ServerCatalogs getServerCatalogs() throws Exception
	{
		return getCumulusConnectionPool().getServerCatalogs();
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
	public String getValue( Field inField)
	{
		if ( inField==null || !inField.hasValue())
		{
			return null;
		}
		Object val;
		try
		{
			val = inField.getValue();
			if( val instanceof String)
			{
				String scrub = scrubChars((String)val);
				return scrub;
			}
		}
		catch (Exception ex)
		{
			log.error( ex );
			throw new OpenEditRuntimeException(ex);
		}
		if ( val == null)
		{
			return null;
		}
		if( val instanceof Double)
		{
			Double d = (Double)val;
			double round = Math.round( d.doubleValue() * 100 );
			String found =  String.valueOf( round / 100D );
			return found;
		}
		return val.toString();
	}
	protected String scrubChars(String inVal)
	{
		StringBuffer done = new StringBuffer(inVal.length());
		for (int i = 0; i < inVal.length(); i++)
		{
			char c = inVal.charAt(i);
			switch (c)
			{
				 case '\t':
		         case '\n':
		         case '\r':
		        	 done.append(c); //these are safe
		        	 break;
		         default:
		         {
		 			if (c > 31) //other skip unless over 31
					{
						done.append(c); 
					}
		         }
			}
		}
		return done.toString();
	}
	public CumulusConnectionPool getCumulusConnectionPool()
	{
		return fieldCumulusConnectionPool;
	}
	public void setCumulusConnectionPool(CumulusConnectionPool inCumulusConnectionPool)
	{
		fieldCumulusConnectionPool = inCumulusConnectionPool;
	}

	
	protected void dump(String type , com.canto.cumulus.Category inCat) throws Exception
	{
		// TODO Auto-generated method stub
		log.info("category: "+ type + "|" + inCat.getName());
		dumpFields( type + "|" + inCat.getName(), inCat.getFields());
		for (int i = 0; i < inCat.getSubCategories().countCategories(); i++)
		{
			dump(type + "|" + inCat.getName(), inCat.getSubCategories().getCategory(i));
		}
	}
	protected void dumpFields(String type , Fields inFields) throws Exception
	{
		// TODO Auto-generated method stub
		for (int i = 0; i < inFields.countFields(); i++)
		{
			Field field = inFields.getField(i);
			log.info("field:" + type + ":" + field.getFieldDefinition().getName());
			if (field.getFieldDefinition().getName().equals("Live Filtering Record Query"))
			{
				log.info("field:" + type + ":" + field.getFieldDefinition().getName());
				if( field.hasValue() ) {
					log.info("value:" + type + ":" + String.valueOf( field.getValue() ));
				}
			}
		}
	}
	public Map getCumulusCategories()
	{
		if (fieldCumulusCategories == null)
		{
			fieldCumulusCategories = new HashMap();
		}

		return fieldCumulusCategories;
	}
	
	protected String createCumulusID(String inCategoryID, String inRecordID)
	{
		String catID = inCategoryID.replace("[", "");
		catID = catID.replace("]", "");
		catID = catID.replace(" ", "");
		catID = catID.replace("&", "");
		String cumulusid = catID + "_" + inRecordID;
		return cumulusid;
	}
	
	
	protected void logMessage(ConvertStatus inStatusLog, String inMessage) {
		log.info(inMessage);
		inStatusLog.getLog().add(inMessage);
	}
	public EnterMedia getEnterMedia()
	{
		return fieldEnterMedia;
	}
	public void setEnterMedia(EnterMedia inEnterMedia)
	{
		fieldEnterMedia = inEnterMedia;
	}
	
	public Map getSettingsMap()
	{
		if (fieldSettingsMap == null)
		{
			fieldSettingsMap = new HashMap();
		}
		return fieldSettingsMap;
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
	
	public boolean ignoreCategory(MediaArchive inArchive, String inId)
	{
		for (Iterator iter = getSettings(inArchive).getChildIterator("ignorecategory"); iter.hasNext();)
		{
			Configuration skip = (Configuration) iter.next();
			if (inId.equalsIgnoreCase(skip.getValue()))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean hideCategory(MediaArchive inArchive, String inId)
	{
		for (Iterator iter = getSettings(inArchive).getChildIterator("hidecategory"); iter.hasNext();)
		{
			Configuration skip = (Configuration) iter.next();
			if (inId.equalsIgnoreCase(skip.getValue()))
			{
				return true;
			}
		}
		return false;
	}
}
