package com.openedit.archive.cumulus;

import junit.framework.TestCase;

public class HexToBinaryConverterTest extends TestCase
{
	public void testHexToBinary_Normal()
	{
		byte[] bytes = new HexToBinaryConverter().hexToBinary( "00121EA0FF" );
		assertEquals( 5, bytes.length );
		assertEquals( (byte) 0, bytes[0] );
		assertEquals( (byte) '\u0012', bytes[1] );
		assertEquals( (byte) '\u001E', bytes[2] );
		assertEquals( (byte) '\u00A0', bytes[3] );
		assertEquals( (byte) '\u00FF', bytes[4] );
	}
	
	public void testHexToBinary_InvalidHex()
	{
		try
		{
			new HexToBinaryConverter().hexToBinary( "0012FX" );
			fail( "Should have caught an IllegalArgumentException" );
		}
		catch ( IllegalArgumentException iae )
		{
			// This is expected.
		}
	}
	
	public void testHexToBinary_InvalidLength()
	{
		try
		{
			new HexToBinaryConverter().hexToBinary( "12345" );
			fail( "Should have caught an IllegalArgumentException" );
		}
		catch ( IllegalArgumentException iae )
		{
			// This is expected.
		}
	}
}
