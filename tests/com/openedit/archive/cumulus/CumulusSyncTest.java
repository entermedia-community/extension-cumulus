/*
 * Created on Mar 24, 2006
 */
package com.openedit.archive.cumulus;

import junit.textui.TestRunner;

import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.ConvertStatus;

import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.ServerCatalog;
import com.openedit.hittracker.HitTracker;

public class CumulusSyncTest extends BaseEnterMediaTest
{
	public CumulusSyncTest(String inArg0)
	{
		super(inArg0);
	}
	
	public static void main(String[] args)
	{
		//new TestRunner().run(new CumulusSyncTest("testSync"));
		new TestRunner().run(new CumulusSyncTest("testReindex"));
	}
	
	protected void setUp() throws Exception
	{
		System.setProperty("oe.root.path","../openedit-archive/webapp");

	}
	/*
	protected void tearDown() throws Exception
	{
		super.tearDown();
		Cumulus.CumulusStop();
	}
	*/
//	public void testReindex() throws Exception
//	{
//        getArchive().getStore().reindexAll();
//	}
	public void XtestFilterLoading() throws Exception
	{
		CumulusSyncConverter converter = (CumulusSyncConverter)getFixture().getModuleManager().getBean("cumulusSyncConverter");
		
		CatalogCollection col = converter.getServerCatalogs().getServerCatalog("Corporate").open();

		converter.exportFilterListData(col.getCatalogRootCategory(), getMediaArchive());
		
		HitTracker types = getMediaArchive().getSearcherManager().getList(getMediaArchive().getCatalogId(),"people");
		assertNotNull(types);
			assertFalse(types.isEmpty());
	}
	
	
	public void testSync() throws Exception
	{
		//We are going to monitor for changes in each catalog
		CumulusSyncConverter converter = (CumulusSyncConverter)getFixture().getModuleManager().getBean("cumulusSyncConverter");
		converter.setModuleManager(getFixture().getModuleManager());
		ConvertStatus status = new ConvertStatus();
		//status.setCategory(getStore().getCatalog("corporate"));
		status.addInput("/testcatalog/data/assets/uploadfile.jpg");
		//	status.setForcedConvert(true);
		converter.importAssets(getMediaArchive(), status);
	}

	public void XtestUserSync() throws Exception
	{
		//We are going to monitor for changes in each catalog
		CumulusUserConverter converter = (CumulusUserConverter)getFixture().getModuleManager().getBean("cumulusUserConverter");

		ServerCatalog cat = converter.getServerCatalogs().getServerCatalog("$Users");
		ConvertStatus status = new ConvertStatus();
		converter.parseUsers(cat, getMediaArchive(),status);
	}
//	public void XtestReindex() throws Exception
//	{
//        getArchive().getStore().reindexAll();
//	}
	public void XtestLogConvert() throws Exception
	{
		CumulusSyncConverter converter = (CumulusSyncConverter)getFixture().getModuleManager().getBean("cumulusSyncConverter");
		converter.importAssets(getMediaArchive(), new ConvertStatus());
	}

	
	/*public void xxxtestPIISourcePathCreator() throws Exception
	{
		PIISourcePathCreator pii = new PIISourcePathCreator();
		Asset newAsset = new Asset();
	//	newAsset.setOr
		newAsset.setProperty("originalpath", "\\\\Cumulus1\\Sharename\\Photos\\Test Files\\dog.jpg");
		newAsset.setProperty("PII_series", "true");
		newAsset.setProperty("Series", "12345");
		
		String sourcepath = pii.createSourcePath(newAsset, newAsset.get("originalpath"));
		assertEquals("Sharename/Photos/Test Files/12/12345/dog.jpg", sourcepath);
		
	}*/
	
	
	protected void tearDown() throws Exception
	{
		super.tearDown();
		//Cumulus.CumulusStop();
	}
}
