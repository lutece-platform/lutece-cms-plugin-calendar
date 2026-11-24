/*
 * Copyright (c) 2002-2026, City of Paris
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

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.details.EventDetails;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads the details of an event from the event form, and lists the forms of plugin-forms a visitor can register with
 */
@ApplicationScoped
public class EventDetailsService
{
    /** Prefix of the parameters of the details in the event form */
    public static final String PREFIX = "event_details_";

    private static final String PLUGIN_FORMS = "forms";
    private static final String SQL_QUERY_FORMS = "SELECT id_form, title FROM forms_form ORDER BY title";
    private static final String URL_FORM = "jsp/site/Portal.jsp?page=forms&view=stepView&id_form=";
    private static final Pattern PATTERN_URL = Pattern.compile( "^(https?://|jsp/site/)\\S+$", Pattern.CASE_INSENSITIVE );
    private static final Pattern PATTERN_EMAIL = Pattern.compile( "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$" );

    /**
     * Reads the details of an event from the parameters of the event form; a value that is not valid is left empty
     *
     * @param request
     *            the request of the form
     * @param nIdEvent
     *            the event
     * @return the details
     */
    public EventDetails readDetails( HttpServletRequest request, int nIdEvent )
    {
        EventDetails details = new EventDetails( );
        details.setIdEvent( nIdEvent );
        details.setPriceType( oneOf( request.getParameter( PREFIX + "price_type" ), EventDetails.PRICE_FREE, EventDetails.PRICE_PAID ) );
        details.setPrice( EventDetails.PRICE_PAID.equals( details.getPriceType( ) ) ? text( request.getParameter( PREFIX + "price" ), 100 ) : null );
        details.setAttendanceMode(
                oneOf( request.getParameter( PREFIX + "attendance_mode" ), EventDetails.ATTENDANCE_OFFLINE, EventDetails.ATTENDANCE_ONLINE, EventDetails.ATTENDANCE_MIXED ) );
        details.setOnlineUrl( EventDetails.ATTENDANCE_OFFLINE.equals( details.getAttendanceMode( ) ) ? null : url( request.getParameter( PREFIX + "online_url" ) ) );
        details.setOrganizerName( text( request.getParameter( PREFIX + "organizer_name" ), 100 ) );
        details.setOrganizerEmail( email( request.getParameter( PREFIX + "organizer_email" ) ) );
        details.setOrganizerPhone( text( request.getParameter( PREFIX + "organizer_phone" ), 30 ) );
        details.setOrganizerUrl( url( request.getParameter( PREFIX + "organizer_url" ) ) );
        details.setRegistrationUrl( url( request.getParameter( PREFIX + "registration_url" ) ) );
        details.setAccessibility( text( request.getParameter( PREFIX + "accessibility" ), 255 ) );

        Double latitude = coordinate( request.getParameter( PREFIX + "latitude" ), 90 );
        Double longitude = coordinate( request.getParameter( PREFIX + "longitude" ), 180 );

        if ( latitude != null && longitude != null )
        {
            details.setLatitude( latitude );
            details.setLongitude( longitude );
        }

        return details;
    }

    /**
     * Lists the forms of plugin-forms, to register with an event; empty when plugin-forms is not enabled
     *
     * @return the forms: code = the URL of the form, name = its title
     */
    public ReferenceList getRegistrationForms( )
    {
        ReferenceList listForms = new ReferenceList( );

        if ( !PluginService.isPluginEnable( PLUGIN_FORMS ) )
        {
            return listForms;
        }

        Plugin pluginForms = PluginService.getPlugin( PLUGIN_FORMS );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_FORMS, pluginForms ) )
        {
            daoUtil.executeQuery( );

            while ( daoUtil.next( ) )
            {
                listForms.addItem( URL_FORM + daoUtil.getInt( 1 ), daoUtil.getString( 2 ) );
            }
        }
        catch( RuntimeException e )
        {
            AppLogService.error( "Calendar: the forms of plugin-forms cannot be listed", e );
        }

        return listForms;
    }

    /**
     * Keeps a value when it is one of the allowed ones
     *
     * @param strValue
     *            the value
     * @param allowed
     *            the allowed values
     * @return the value, or null
     */
    private static String oneOf( String strValue, String... allowed )
    {
        for ( String strAllowed : allowed )
        {
            if ( strAllowed.equals( strValue ) )
            {
                return strValue;
            }
        }

        return null;
    }

    /**
     * Trims a text and cuts it to a length
     *
     * @param strValue
     *            the text
     * @param nMax
     *            the length of the column
     * @return the text, or null when it is blank
     */
    private static String text( String strValue, int nMax )
    {
        return StringUtils.isBlank( strValue ) ? null : StringUtils.left( strValue.trim( ), nMax );
    }

    /**
     * Keeps a web address: absolute http(s), or a page of the site
     *
     * @param strValue
     *            the address
     * @return the address, or null when it is not one
     */
    private static String url( String strValue )
    {
        String strUrl = text( strValue, 255 );

        return ( strUrl != null && PATTERN_URL.matcher( strUrl ).matches( ) ) ? strUrl : null;
    }

    /**
     * Keeps an e-mail address
     *
     * @param strValue
     *            the address
     * @return the address, or null when it is not one
     */
    private static String email( String strValue )
    {
        String strEmail = text( strValue, 100 );

        return ( strEmail != null && PATTERN_EMAIL.matcher( strEmail ).matches( ) ) ? strEmail : null;
    }

    /**
     * Reads a coordinate
     *
     * @param strValue
     *            the coordinate, with a dot or a comma
     * @param nMax
     *            the largest absolute value
     * @return the coordinate, or null when it is missing or out of range
     */
    private static Double coordinate( String strValue, int nMax )
    {
        if ( StringUtils.isBlank( strValue ) )
        {
            return null;
        }

        try
        {
            double dValue = Double.parseDouble( strValue.trim( ).replace( ',', '.' ) );

            return ( Math.abs( dValue ) <= nMax ) ? dValue : null;
        }
        catch( NumberFormatException e )
        {
            return null;
        }
    }
}
