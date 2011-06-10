/*
 * Created on Aug 16, 2005
 */
package com.openedit.archive.cumulus;


/*
 * Copyright (c) Ian F. Darwin, http://www.darwinsys.com/, 1996-2002.
 * All rights reserved. Software written by Ian F. Darwin and others.
 * $Id: Parser.java,v 1.1 2010/04/20 15:24:37 cburkey Exp $
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Java, the Duke mascot, and all variants of Sun's Java "steaming coffee
 * cup" logo are trademarks of Sun Microsystems. Sun's, and James Gosling's,
 * pioneering role in inventing and promulgating (and standardizing) the Java 
 * language and environment is gratefully acknowledged.
 * 
 * The pioneering role of Dennis Ritchie and Bjarne Stroustrup, of AT&T, for
 * inventing predecessor languages C and C++ is also gratefully acknowledged.
 */
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/* Simple demo of CSV parser class.
 */
/** Parse comma-separated values (CSV), a common Windows file format.
 * Sample input: "LU",86.25,"11/4/1998","2:19PM",+4.0625
 * <p>
 * Inner logic adapted from a C++ original that was
 * Copyright (C) 1999 Lucent Technologies
 * Excerpted from 'The Practice of Programming'
 * by Brian W. Kernighan and Rob Pike.
 * <p>
 * Included by permission of the http://tpop.awl.com/ web site, 
 * which says:
 * "You may use this code for any purpose, as long as you leave 
 * the copyright notice and book citation attached." I have done so.
 * @author Brian W. Kernighan and Rob Pike (C++ original)
 * @author Ian F. Darwin (translation into Java and removal of I/O)
 * @author Ben Ballard (rewrote advQuoted to handle '""' and for readability)
 */
public class Parser {  

	  public static void main(String[] args) {
		  Parser parser = new Parser();
		    List list = parser.parse(
		      "\"LU\",86.25,\"11/4/1998\",\"2:19PM\",+4.0625");
		    Iterator it = list.iterator();
		    while (it.hasNext()) {
		      System.out.println(it.next());
		    }

		    // Now test with a non-default separator
		    parser = new Parser('|');
		    list = parser.parse(
		      "\"LU\"|86.25|\"11/4/1998\"|\"2:19PM\"|+4.0625");
		    it = list.iterator();
		    while (it.hasNext()) {
		      System.out.println(it.next());
		    }
		  }
	 
	 
  public static final char DEFAULT_SEP = ',';

  /** Construct a CSV parser, with the default separator (`,'). */
  public Parser() {
    this(DEFAULT_SEP);
  }

  /** Construct a CSV parser with a given separator. 
   * @param sep The single char for the separator (not a list of
   * separator characters)
   */
  public Parser(char sep) {
    fieldSep = sep;
  }

  /** The fields in the current String */
  protected List list = new ArrayList();
  protected Pattern tabs =  Pattern.compile("\t");

  /** the separator char for this parser */
  protected char fieldSep;

  
  public String[] parseRegEx(String line)
  {
	  //list.clear();      
	  String[] args = tabs.split(line);
	  //list.addAll(Arrays.asList(args));
	  return args;
  }
  
  /** parse: break the input String into fields
   * @return java.util.Iterator containing each field 
   * from the original as a String, in order.
   */
  public List parse(String line)
  {
    StringBuffer sb = new StringBuffer();
    list.clear();      // recycle to initial state
    int i = 0;

    if (line.length() == 0) {
      list.add(line);
      return list;
    }

    do {
            sb.setLength(0);
            if (i < line.length() && line.charAt(i) == '"')
                i = advQuoted(line, sb, ++i);  // skip quote
            else
                i = advPlain(line, sb, i);
            list.add(sb.toString());
      i++;
    } while (i < line.length());

    return list;
  }

  /** advQuoted: quoted field; return index of next separator */
  protected int advQuoted(String s, StringBuffer sb, int i)
  {
    int j;
    int len= s.length();
        for (j=i; j<len; j++) {
            if (s.charAt(j) == '"' && j+1 < len) {
                if (s.charAt(j+1) == '"') {
                    j++; // skip escape char
                } else if (s.charAt(j+1) == fieldSep) { //next delimeter
                    j++; // skip end quotes
                    break;
                }
            } else if (s.charAt(j) == '"' && j+1 == len) { // end quotes at end of line
                break; //done
      }
      sb.append(s.charAt(j));  // regular character.
    }
    return j;
  }

  /** advPlain: unquoted field; return index of next separator */
  protected int advPlain(String s, StringBuffer sb, int i)
  {
    int j;

    j = s.indexOf(fieldSep, i); // look for separator
        if (j == -1) {                 // none found
            sb.append(s.substring(i));
            return s.length();
        } else {
            sb.append(s.substring(i, j));
            return j;
        }
    }
}

