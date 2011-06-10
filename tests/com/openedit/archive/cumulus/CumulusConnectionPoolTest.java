package com.openedit.archive.cumulus;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.dom4j.Element;
import org.openedit.xml.XmlFile;

import com.openedit.util.XmlUtil;

public class CumulusConnectionPoolTest extends TestCase
{
	String option1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
		+ "<archive>" 
		+ "<username>cumulus</username>" 
		+ " <password>cumulus</password>"
			+ "<readonly>false</readonly>" 
			+ "<server>10.250.220.73</server>"
			+ "<skipthumbs>true</skipthumbs>"
			+ "<skipmeds>false</skipmeds>"
			+ "</archive>";
	
	CumulusConnectionPool pool;

	protected void setUp() throws Exception
	{
		super.setUp();
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

	public void testReconnect() throws Exception
	{
		pool.reconnect();
		assertTrue("expect skip thumbs to be true", pool.isSkipThumbnails());
		assertTrue("expect skip meds to be false",!pool.isSkipMediums());
	}

}
