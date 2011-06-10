package com.openedit.archive.cumulus;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests
{

	public static Test suite()
	{
		TestSuite suite = new TestSuite( "Test for com.openedit.store.cumulus" );
		
		
		//$JUnit-BEGIN$
		
		suite.addTestSuite( HexToBinaryConverterTest.class );
		//To slow suite.addTestSuite( CumulusSyncTest.class );
		
		//$JUnit-END$
		return suite;
	}

}
