/*
 * Created on Aug 16, 2005
 */
package com.openedit.archive.cumulus;

import java.io.File;

import org.openedit.data.Searcher;
import org.openedit.entermedia.Asset;
import org.openedit.entermedia.BaseEnterMediaTest;
import org.openedit.entermedia.ConvertStatus;

import com.openedit.hittracker.HitTracker;
import com.openedit.hittracker.SearchQuery;
import com.openedit.util.FileUtils;

public class CumulusConvertTest extends BaseEnterMediaTest
{
	public CumulusConvertTest(String inArg0)
	{
		super(inArg0);
	}

	protected void setUp() throws Exception
	{
		//System.setProperty("oe.root.path","../entermedia-cumulus/webapp");
		
		File etc = new File( getRoot().getParentFile(),"etc");
		//copy back the export file
		FileUtils util = new FileUtils();
		File export = new File(etc,"/export.cre");
		File out = new File(getMediaArchive().getRootDirectory(),"/import/export.cre");
	//	util.copyFiles(export,out);
		File input = new File( etc, "archivefiles");
		File output = new File( "/tmp/archivefiles");
		util.dirCopy(input,output);
	}

	public void testConvert() throws Exception
	{
		//File cats = new File( getStore().getStoreDirectory() , "./configuration/catalogs.xml");
		//cats.delete();
		CumulusConverter converter = new CumulusConverter();
		converter.setPageManager(getFixture().getPageManager());
		converter.importAssets(getMediaArchive(), new ConvertStatus());
		getMediaArchive().getAssetArchive().clearAssets();
		Asset product = getMediaArchive().getAsset("ac01dottif1078102621");
		assertNotNull(product);
		assertTrue(product.getCategories().size() > 0);
	}
	public void testIndex() throws Exception
	{
		getMediaArchive().reindexAll();
		//digitalcolor_acheatingservices_airconditioningcontractors
		Searcher searcher = getMediaArchive().getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		query.addMatches("category", "digital_colorac__heating_servicesair_conditioning_contractors");
		HitTracker hits = searcher.search(query);
		assertTrue( hits.getTotal() > 1 );

		query = searcher.createSearchQuery();
		query.addMatches("description", "air conditioning");
		hits = searcher.search(query);
		assertTrue( hits.getTotal() > 1 );
	}
	public void XtestMedium() throws Exception
	{
		File missing = new File( getMediaArchive().getRootDirectory(),"/products/images/thumb/a/acfanvent01dottif1078102626.jpg");
		missing.delete();

		File med = new File( getMediaArchive().getRootDirectory(),"/products/images/medium/a/acfanvent01dottif1078102626.jpg");
		med.delete();
		//FIXME: TODO: Fix this
		//getMediaArchive().getImageCreator().run(); //This should make a new thumbnail using image magic. 
		//It should also make a new medium sized image
		
		assertTrue( missing.exists());
		assertTrue( med.exists());
		
	}
	
	
	public void testExtract() throws Exception
	{
		CumulusConverter convert = new CumulusConverter();
		convert.setPageManager(getFixture().getPageManager());
		/*
		 * 		name = name.replaceAll(" ","sp");
		name = name.replaceAll("&","amp");
		name = name.replaceAll("(","lp");
		name = name.replaceAll(")","rp");
		name = name.replaceAll("\\.","dot");
		name = name.replaceAll("_","und");
		name = name.replaceAll("+","plus");
		name = name.replaceAll("-","min");

		 */
		String t = "ddf_ddf";
		assertEquals("ddfddf",convert.extractId(t, false) );

		t = "ddf&ddf";
		assertEquals("ddfddf",convert.extractId(t,false) );
		t = "ddf(ddf";
		assertEquals("ddfddf",convert.extractId(t,false) );
		t = "ddf)ddf";
		assertEquals("ddfddf",convert.extractId(t,false) );
		t = "ddf.ddf";
		assertEquals("ddfddf",convert.extractId(t,false) );
		t = "ddf+ddf";
		assertEquals("ddfddf",convert.extractId(t,false) );
		t = "ddf-ddf";
		assertEquals("ddfddf",convert.extractId(t,false) );
	}
	public void testSearchAssets() throws Exception
	{
		Searcher searcher = getMediaArchive().getAssetSearcher();
		SearchQuery query = searcher.createSearchQuery();
		getMediaArchive().reindexAll();
		query.addMatches("description", "\"AC01.tif\"");
		HitTracker hits = searcher.search(query);
		assertTrue( hits.getTotal() > 0);

		query = searcher.createSearchQuery();
		query.addMatches("description", "Conditioning");
		hits = searcher.search(query);
		assertTrue( hits.getTotal() > 10 );

		query = searcher.createSearchQuery();
		query.addMatches("description", "AC01*");
		hits = searcher.search(query);
		assertTrue( hits.getTotal() > 0);

		query = searcher.createSearchQuery();
		query.addMatches("description", "\"Conditioning\"");
		hits = searcher.search(query);
		assertTrue( hits.getTotal() > 10 );

		query = searcher.createSearchQuery();
		query.addMatches("description", "Digital");
		hits = searcher.search(query);
		assertTrue( hits.getTotal() > 10 );
		
		//hits = getStore().search("border");
		//TODO: get auto wildcard searching added assertEquals( 1, hits.length() );

	}

	
}
