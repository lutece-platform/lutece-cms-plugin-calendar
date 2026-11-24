/*
 * Copyright (c) 2002-2014, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.calendar.business;

import fr.paris.lutece.plugins.calendar.business.category.Category;
import fr.paris.lutece.plugins.calendar.business.category.CategoryHome;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.test.LuteceTestCase;

import org.junit.jupiter.api.Test;

/**
 * Round trip of a category through its Home and its DAO
 */
public class CategoryBusinessTest extends LuteceTestCase
{
    private static final String NAME1 = "CategoryName1";
    private static final String NAME2 = "CategoryName2";
    private static final String DESCRIPTION1 = "Description1";
    private static final String DESCRIPTION2 = "Description2";
    private static final String WORKGROUP = "all";
    private static final String MIME_TYPE = "image/png";

    /**
     * Creates, reads, updates and removes a category
     */
    @Test
    public void testBusiness( )
    {
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        Category category = new Category( );
        category.setName( NAME1 );
        category.setDescription( DESCRIPTION1 );
        category.setWorkgroup( WORKGROUP );
        category.setIconContent( new byte [ ] { 1, 2, 3 } );
        category.setIconMimeType( MIME_TYPE );

        CategoryHome.create( category, plugin );

        Category categoryStored = CategoryHome.find( category.getId( ), plugin );
        assertNotNull( categoryStored, "the created category is found" );
        assertEquals( NAME1, categoryStored.getName( ) );
        assertEquals( DESCRIPTION1, categoryStored.getDescription( ) );
        assertEquals( WORKGROUP, categoryStored.getWorkgroup( ) );
        assertFalse( CategoryHome.findByName( NAME1, plugin ).isEmpty( ), "the category is found by its name" );

        category.setName( NAME2 );
        category.setDescription( DESCRIPTION2 );
        CategoryHome.update( category, plugin );
        categoryStored = CategoryHome.find( category.getId( ), plugin );
        assertEquals( NAME2, categoryStored.getName( ) );
        assertEquals( DESCRIPTION2, categoryStored.getDescription( ) );
        assertEquals( 0, CategoryHome.findCountIdEvents( category.getId( ), plugin ) );

        CategoryHome.remove( category.getId( ), plugin );
        assertNull( CategoryHome.find( category.getId( ), plugin ), "the removed category is gone" );
    }
}
