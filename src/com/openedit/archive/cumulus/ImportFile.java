/*
 * Created on Aug 15, 2005
 */
package com.openedit.archive.cumulus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import com.openedit.util.FileUtils;

public class ImportFile
{
	protected List fieldRows;
	protected Header fieldHeader;
	protected BufferedReader fieldReader;
	protected Parser fieldParser;
	
	public ImportFile()
	{
	}
	public void load(Reader inFile) throws Exception
	{
		//read in tabs or whatever into header object.
		//foreach row add a row object
		BufferedReader reader = new BufferedReader(inFile);
		setReader(reader);
		
		String line = reader.readLine(); 
		while( line != null)
		{
			if( line.startsWith("%Fieldnames"))
			{
				String row = reader.readLine(); //header
				List cells = getParser().parse(row);
				setHeader(new Header());
				getHeader().setHeaders((String[])cells.toArray(new String[cells.size()]));
			}
			else if( line.startsWith("%Data"))
			{
				break;
			}
			line = reader.readLine(); 
		}


	}
	
	public Row getNextRow() throws IOException
	{
		String line = getReader().readLine();
		if ( line == null)
		{
			FileUtils.safeClose(getReader());
			return null;
		}
		//line = line.replace('\u001e',','); //get rid of junk chars
		/*
		line = line.replace('\u001e',' '); //get rid of junk chars
		line = line.replace('\u0005',' '); //get rid of junk chars
		line = line.replace('\u0010',' '); //get rid of junk chars
		line = line.replace('\u001f',' '); //get rid of junk chars
		line = line.replace('\u000f',' '); //get rid of junk chars
		*/
//		line = line.replaceAll("\u001e"," , ");
//
//		//Only keep valid ASCII text
//		StringBuffer escapedSource = new StringBuffer(line.length());
//		//String zeros = "000000";
//		for ( int n = 0; n < line.length(); n++ )
//		{
//			char c = line.charAt( n );
//			if ( c  > 31 && c < 127  )
//			{
//				escapedSource.append( c );
//			}
//			if ( c == '\t')
//			{
//				escapedSource.append( c );
//			}
//			else
//			{ 
//				//skip ISO just 32 - 126
//			}
//		}
//		line = escapedSource.toString();
		
		String[] cells = getParser().parseRegEx(line);
		Row row = new Row();
		row.setHeader(getHeader());
		//row.setData((String[])cells.toArray(new String[cells.size()]));
		row.setData( cells );
		return row;
	}

	public Header getHeader()
	{
		return fieldHeader;
	}

	public void setHeader(Header inHeader)
	{
		fieldHeader = inHeader;
	}
	public BufferedReader getReader()
	{
		return fieldReader;
	}
	public void setReader(BufferedReader inReader)
	{
		fieldReader = inReader;
	}
	public Parser getParser()
	{
		if ( fieldParser == null)
		{
			fieldParser = new Parser('\t');
		}
		return fieldParser;
	}
	public void setParser(Parser inParser)
	{
		fieldParser = inParser;
	}
	public void close()
	{
		FileUtils.safeClose( getReader() );
	}
	
}
