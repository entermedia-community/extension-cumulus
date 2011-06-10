package com.openedit.archive.cumulus;

public class HexToBinaryConverter
{
	public byte[] hexToBinary( String inHex )
	{
		if ( inHex.length() % 2 != 0 )
		{
			throw new IllegalArgumentException( "Hex string must have an even number of characters" );
		}
		byte[] bytes = new byte[inHex.length() / 2];
		for ( int i = 0; i < inHex.length(); i += 2 )
		{
			bytes[i >> 1] = (byte) ( ( convertChar( inHex.charAt( i ) ) << 4 ) |
				convertChar( inHex.charAt( i + 1 ) ) );
		}
		return bytes;
	}
	
	protected int convertChar( char inChar )
	{
		if ( inChar >= '0' && inChar <= '9' )
		{
			return inChar - '0';
		}
		else if ( inChar >= 'A' && inChar <= 'F' )
		{
			return inChar - 'A' + 10;
		}
		else
		{
			throw new IllegalArgumentException( "Invalid hex character " + inChar );
		}
	}
}
