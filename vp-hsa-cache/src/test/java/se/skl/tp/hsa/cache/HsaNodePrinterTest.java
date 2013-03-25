/**
 * Copyright 2013 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.hsa.cache;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.junit.Test;

public class HsaNodePrinterTest {
	
	String expected = 
			"dn=o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000004-1234,lineNo=26"+ System.getProperty("line.separator") +
			"  dn=ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000003-1234,lineNo=21"+ System.getProperty("line.separator") +
			"    dn=ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000002-1234,lineNo=16"+ System.getProperty("line.separator") +
			"      dn=ou=N\u00e4ssj\u00f6 VC DLK,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000000-1234,lineNo=11"+ System.getProperty("line.separator") +
			"      dn=ou=N\u00e4ssj\u00f6 VC DLM,ou=N\u00e4ssj\u00f6 Prim\u00e4rv\u00e5rdsomr\u00e5de,ou=H\u00f6glandets sjukv\u00e5rdsomr\u00e5de,o=Landstinget i J\u00f6nk\u00f6ping,l=VpW,c=SE,hsaId=SE0000000001-1234,lineNo=6"+ System.getProperty("line.separator");

	
	@Test
	public void testPrint() throws Exception {
		URL url = getClass().getClassLoader().getResource("simpleTest.xml");
		HsaCacheImpl impl = (HsaCacheImpl)new HsaCacheImpl().init(url.getFile());
		
		HsaNode topNode = impl.getNode("SE0000000004-1234");
		
		StringWriter sw = new StringWriter();
		
		new HsaNodePrinter(topNode,2).printTree(new PrintWriter(sw));
		
		assertEquals(expected, sw.toString());
		
	}
}