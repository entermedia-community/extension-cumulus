package com.openedit.archive.cumulus;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.openedit.xml.XmlFile;

import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.Cumulus;
import com.canto.cumulus.CumulusException;
import com.canto.cumulus.Record;
import com.canto.cumulus.Records;
import com.canto.cumulus.ServerCatalog;
import com.openedit.WebPageRequest;
import com.openedit.util.XmlUtil;

/**
 * This is a class used to test cumulus searches.
 * @author work
 *
 */
public class CumulusSearcher
{
	
	protected CumulusConnectionPool pool;
	CumulusSearcher()
	{
		
		String option1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
			+ "<archive>" 
			+ "<username>cumulus</username>" 
			+ " <password>cumulus</password>"
				+ "<readonly>false</readonly>" 
				+ "<server>10.250.50.89</server>"
			//	+ "<server>216.68.239.220</server>"
				+ "<skipthumbs>true</skipthumbs>"
				+ "<skipmeds>false</skipmeds>"
				+ "</archive>";
		
		final XmlFile file = new XmlFile();
		Reader reader = new StringReader( option1 );
		Element element = new XmlUtil().getXml(reader, "UTF-8");

		file.setRoot(element);
		pool = new CumulusConnectionPool()
		{
			protected XmlFile getFile()
			{
				return file;
			}
		};
	}
	
	public void search(WebPageRequest inReq) throws CumulusException
	{
		pool.reconnect();
		String catname = inReq.findValue("catname");
		ServerCatalog cumulusCat = pool.getServerCatalogs().getServerCatalog(catname);
		CatalogCollection col = cumulusCat.open();
		String lastModified = "Record Modification Date";
		
		String date = inReq.findValue("date");
//		String lastChecked = "2008-10-02 01:10:50";
//		String lastChecked = "10/2/2008 9:28:21 AM";
		String sQuery = lastModified + "\tafter\t" + date;
		System.out.println(sQuery);
		col.find(sQuery, Cumulus.FindNew);
		Records records = col.getRecords();
		int count = records.countRecords();
		System.out.println("Num records: " + count);
		
		List list = new ArrayList();
		for (int i = 0; i < count; i++)
		{
			Record record = records.getRecord(i);
			list.add(record.getFields().getField("Record Name").getValue());
			list.add(record.getFields().getField("Record Modification Date").getValue());
		}
		inReq.putPageValue("results", list);
	}
}
