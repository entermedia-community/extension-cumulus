/*
 * Created on Jul 13, 2006
 */
package com.openedit.archive.cumulus;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.openedit.xml.XmlFile;

import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.Categories;
import com.canto.cumulus.Cumulus;
import com.canto.cumulus.Server;
import com.canto.cumulus.ServerCatalog;
import com.canto.cumulus.ServerCatalogs;
import com.openedit.OpenEditException;
import com.openedit.ShutdownList;
import com.openedit.Shutdownable;
import com.openedit.page.Page;
import com.openedit.page.manage.PageManager;
import com.openedit.util.XmlUtil;

public class CumulusConnectionPool implements Shutdownable
{
	private static final Log log = LogFactory.getLog(CumulusConnectionPool.class);
	
	protected XmlFile fieldXmlFile;
	protected PageManager fieldPageManager;
	protected long fieldLastUsed;
	protected Server fieldServerConnection;
	protected static boolean hasStarted = false;
	protected Map fieldCatalogCollections;
	protected boolean fieldSkipThumbnails;
	protected boolean fieldSkipMediums;
	
	public Server getAvailableServer() throws OpenEditException
	{
		if( fieldServerConnection == null)
		{
			reconnect();
		}
		return fieldServerConnection;
	}
	public ServerCatalogs getServerCatalogs() 
	{
		ServerCatalogs catalogs = null;
		try
		{
			catalogs = getAvailableServer().getServerCatalogs();
			long now = new Date().getTime();
			//if iddle longer then 5 minutes make sure its still working
			if( now >  getLastUsed() + (1000 * 60 * 5) )
			{
				log.info("Cumulus connection being tested");
				catalogs.getServerCatalog("$Users").getIsPublishedToInternet();				
			}
		}
		catch ( Exception ex)
		{
			log.error("Could not connect to $Users catalog. Make sure it is shared and public",ex);
			ex.printStackTrace();
			
			reconnect();
			catalogs = getAvailableServer().getServerCatalogs();
		}
		setLastUsed(System.currentTimeMillis());
		return catalogs;
	}
	
	public void reconnect() throws OpenEditException
	{
		
		String username = elementText("username");
		String password = elementText("password");
		String server = elementText("server");
		String readonly = elementText("readonly");
		String skipThumbs =  elementText("skipthumbs");
		String skipMeds = elementText("skipmeds");
		setSkipThumbnails(Boolean.parseBoolean(skipThumbs));
		setSkipMediums(Boolean.parseBoolean(skipMeds));
		try
		{
			boolean read = Boolean.parseBoolean(readonly);
			read = !read;
			if( !hasStarted)
			{
				log.info("Starting Cumulus");
				Cumulus.CumulusStart();
				hasStarted = true;
			}
			log.info("Connecting to cumulus " + server);
			fieldServerConnection = Server.connectToServer(read,server, username,password);
			getCatalogCollections().clear();
		}
		catch ( Exception ex )
		{
			log.error(ex);
			throw new OpenEditException(ex);
		}
	}
	protected String elementText(String key)
	{
		return getFile().getRoot().elementText(key);
	}

	public void expireConnection()
	{
		long now = new Date().getTime();

		//expire after 15 minutes of iddle
		if( now >  getLastUsed() + (1000 * 60 * 15) )
		{
			fieldServerConnection = null;
		}
	}
	protected XmlFile getFile()
	{
		if( fieldXmlFile == null)
		{
			fieldXmlFile = new XmlFile();
			String path = "/WEB-INF/cumulus.xml";
			fieldXmlFile.setPath(path);
		}
		Page config = getPageManager().getPage(fieldXmlFile.getPath());
		if( config.exists() && config.getLastModified().getTime() != fieldXmlFile.getLastModified() )
		{
			Element root = new XmlUtil().getXml(config.getReader(), config.getCharacterEncoding());
			fieldXmlFile.setRoot(root);
			fieldXmlFile.setLastModified(config.getLastModified().getTime());
		}
		return fieldXmlFile;
	}
	public PageManager getPageManager()
	{
		return fieldPageManager;
	}
	public void setPageManager(PageManager inPageManager)
	{
		fieldPageManager = inPageManager;
	}
	protected long getLastUsed()
	{
		return fieldLastUsed;
	}
	protected void setLastUsed(long inLastUsed)
	{
		fieldLastUsed = inLastUsed;
	}
	public Server getServerConnection()
	{
		return fieldServerConnection;
	}
	public void setServerConnection(Server inServerConnection)
	{
		fieldServerConnection = inServerConnection;
	}
	public void setShutdownList(ShutdownList inList)
	{
		inList.addForShutdown(this); //used to support shutdownd
	}
	public void shutdown()
	{
		if( hasStarted)
		Cumulus.CumulusStop();
	}
	protected void finalize() throws Throwable
	{
		shutdown();
		super.finalize();
	}
	public boolean isEnabled()
	{
		return getFile().getLastModified() > -1;
	}
	protected Map getCatalogCollections()
	{
		if (fieldCatalogCollections == null)
		{
			fieldCatalogCollections = new HashMap(2);
		}
		return fieldCatalogCollections;
	}
	public CatalogCollection getCatalogCollection(String inCatName) throws Exception
	{
		checkConnection();
		CatalogCollection col = (CatalogCollection)getCatalogCollections().get(inCatName);
		long now = System.currentTimeMillis();

		//expire after 15 minutes of iddle

		if( col != null || now >  getLastUsed() + (1000 * 60 * 15))
		{
			try
			{
				Categories root = col.getCategories();
			}
			catch ( Exception ex)
			{
				log.error("Cumulus must have closed. Checking connection");
				col = null;
				reconnect();
			}
		}
		setLastUsed(System.currentTimeMillis());

		if( col == null )
		{
			log.info("Opening: " + inCatName);
			ServerCatalog cat = getAvailableServer().getServerCatalogs().getServerCatalog(inCatName);
			col = cat.open();
			getCatalogCollections().put(inCatName, col);
		}
		return col;
	}
	protected void checkConnection()
	{
		getServerCatalogs();
		
	}
	public boolean isSkipThumbnails()
	{
		return fieldSkipThumbnails;
	}
	public void setSkipThumbnails(boolean inSkipThumbnails)
	{
		fieldSkipThumbnails = inSkipThumbnails;
	}
	public boolean isSkipMediums()
	{
		return fieldSkipMediums;
	}
	public void setSkipMediums(boolean inSkipMediums)
	{
		fieldSkipMediums = inSkipMediums;
	}
}
