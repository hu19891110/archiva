package org.apache.archiva.web.test;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.web.test.parent.AbstractArchivaTest;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Olivier Lamy
 */
@Test( groups = { "usermanagement" }, dependsOnGroups = "about" )
public class RolesManagementTest
    extends AbstractArchivaTest
{
    @Test
    public void testReadRolesAndUpdateDescription()
        throws Exception
    {
        login( getAdminUsername(), getAdminPassword() );
        clickLinkWithLocator( "menu-roles-list-a", true );
        assertTextPresent( "Archiva System Administrator " );
        Assert.assertTrue( StringUtils.isEmpty( getText( "role-description-Guest" ) ) );
        clickLinkWithLocator( "edit-role-Guest" );
        String desc = "The guest description";
        setFieldValue( "role-edit-description", desc );
        clickButtonWithLocator( "role-edit-description-save" );
        clickLinkWithLocator( "roles-view-tabs-a-roles-grid" );
        Assert.assertTrue( StringUtils.equals( desc, getText( "role-description-Guest" ) ) );
    }
}