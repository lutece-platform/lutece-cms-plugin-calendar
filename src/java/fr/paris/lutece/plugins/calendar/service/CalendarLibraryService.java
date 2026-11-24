/*
 * Copyright (c) 2002-2025, City of Paris
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
package fr.paris.lutece.plugins.calendar.service;

import java.util.List;
import java.util.Locale;

import fr.paris.lutece.plugins.calendar.business.parameter.CalendarParameterHome;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.ReferenceItem;
import fr.paris.lutece.util.ReferenceList;

/**
 * The JavaScript library drawing the calendar views of the front and back offices, chosen in the advanced parameters:
 * EventCalendar (the default) or Agendo.
 */
public final class CalendarLibraryService
{
    /** Key of the advanced parameter, and name of the field of its form */
    public static final String PARAMETER_CALENDAR_LIBRARY = "calendar_library";
    /** Model mark of the chosen library */
    public static final String MARK_CALENDAR_LIBRARY = "calendar_library";
    /** Model mark of the list of the libraries, for a select */
    public static final String MARK_CALENDAR_LIBRARY_LIST = "calendar_library_list";
    /** EventCalendar, the library used until now */
    public static final String LIBRARY_EVENT_CALENDAR = "event-calendar";
    /** Agendo, a vanilla JavaScript calendar with Tabler and Bootstrap themes */
    public static final String LIBRARY_AGENDO = "agendo";
    /** The libraries, the default first */
    public static final List<String> LIBRARIES = List.of( LIBRARY_EVENT_CALENDAR, LIBRARY_AGENDO );

    private static final String PREFIX_LABEL = "calendar.advanced_parameters.library.";

    /**
     * Static methods only
     */
    private CalendarLibraryService( )
    {
    }

    /**
     * Tells whether a value names a known library
     * @param strLibrary the value
     * @return true for event-calendar and agendo
     */
    public static boolean isLibrary( String strLibrary )
    {
        return LIBRARIES.contains( strLibrary );
    }

    /**
     * Returns the chosen library, the default one when the parameter is missing or holds an unknown value
     * @param plugin the plugin
     * @return the key of the library
     */
    public static String getLibrary( Plugin plugin )
    {
        ReferenceItem parameter = CalendarParameterHome.findByKey( PARAMETER_CALENDAR_LIBRARY, plugin );
        String strLibrary = ( parameter != null ) ? parameter.getName( ) : null;

        return isLibrary( strLibrary ) ? strLibrary : LIBRARY_EVENT_CALENDAR;
    }

    /**
     * Returns the libraries with their labels, for the select of the advanced parameters
     * @param locale the locale of the labels
     * @return the list
     */
    public static ReferenceList getLibraryList( Locale locale )
    {
        ReferenceList list = new ReferenceList( );

        for ( String strLibrary : LIBRARIES )
        {
            list.addItem( strLibrary, I18nService.getLocalizedString( PREFIX_LABEL + strLibrary, locale ) );
        }

        return list;
    }
}
