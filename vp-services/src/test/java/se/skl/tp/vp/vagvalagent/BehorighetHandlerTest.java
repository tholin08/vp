/**
 * Copyright (c) 2013 Sveriges Kommuner och Landsting (SKL).
 * 								<http://www.skl.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagvalagent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.AN_HOUR_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_ONE_HOUR;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_TEN_YEARS;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.TWO_HOURS_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createAuthorization;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.getRelativeDate;

import java.net.URL;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheImpl;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;

public class BehorighetHandlerTest {

	HsaCache hsaCache;
	
	@Before
	public void beforeTest() throws Exception {

		URL url = getClass().getClassLoader().getResource("hsacache.xml");
		hsaCache = new HsaCacheImpl().init(url.getFile());
	}	
	
	@Test
	public void testMapCreation() throws Exception {

		ArrayList<AnropsBehorighetsInfoType> authorization = new ArrayList<AnropsBehorighetsInfoType>();
		authorization.add(createAuthorization("sender-1", "namnrymd-1", "receiver-1"));
		authorization.add(createAuthorization("sender-2", "namnrymd-1", "receiver-1"));
		authorization.add(createAuthorization("sender-3", "namnrymd-1", "receiver-1", getRelativeDate(TWO_HOURS_AGO), getRelativeDate(AN_HOUR_AGO)));
		authorization.add(createAuthorization("sender-3", "namnrymd-1", "receiver-1", getRelativeDate(IN_ONE_HOUR), getRelativeDate(IN_TEN_YEARS)));
		
		BehorighetHandler bh = new BehorighetHandler(hsaCache, authorization);
		
		assertEquals(1, bh.lookupInPermissionMap("receiver-1", "sender-1", "namnrymd-1").size());
		assertEquals(1, bh.lookupInPermissionMap("receiver-1", "sender-2", "namnrymd-1").size());
		assertEquals(2, bh.lookupInPermissionMap("receiver-1", "sender-3", "namnrymd-1").size());
		assertNull(bh.lookupInPermissionMap("receiver-1", "sender-4", "namnrymd-1"));		
	}
}