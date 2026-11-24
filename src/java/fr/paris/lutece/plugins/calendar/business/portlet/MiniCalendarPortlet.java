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
package fr.paris.lutece.plugins.calendar.business.portlet;

import jakarta.enterprise.inject.spi.CDI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.Event;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.plugins.calendar.service.Utils;
import fr.paris.lutece.plugins.calendar.service.PortletMonthCalendar;
import fr.paris.lutece.plugins.calendar.service.search.CalendarSearchService;
import fr.paris.lutece.portal.business.portlet.PortletHtmlContent;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.util.date.DateUtil;


/**
 * This class represents the business object CalendarPortlet.
 */
public class MiniCalendarPortlet extends PortletHtmlContent
{
    private static final String TEMPLATE_PORTLET_DEFAULT = "skin/plugins/calendar/portlet/mini_calendar_portlet.html";

    // Markers
    private static final String MARK_MONTH = "month";
    private static final String MARK_MONTH_URL = "month_url";
    private static final String MARK_PAGE_ID = "page_id";
    private static final String MARK_TOP_EVENTS = "top_events";
    private static final String MARK_EVENTS = "events";

    // Event keys
    private static final String KEY_EVENT_ID = "eventId";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DATETIME_BEGIN = "dateTimeBegin";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_DATE = "date";
    private static final String KEY_DATE_END = "dateEnd";

    //JSP
    private static final String JSP_PORTAL_URL = "jsp/site/Portal.jsp";

    //Commons
    private static final String POINT_INTERROGATION = "?";
    private static final String EGAL = "=";
    private static final String PARAM_PAGE_ID = "page_id";

    //Session variable
    private static Calendar _cal;

    /**
     * Sets the name of the plugin to which the portlet belongs
     * @param strPluginName The plugin name
     */
    @Override
    public void setPluginName( String strPluginName )
    {
        super.setPluginName( strPluginName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHtmlContent( HttpServletRequest request )
    {
        Map<String, Object> model = createPortletModel( );
        Locale locale;

        if ( _cal == null )
        {
            _cal = new GregorianCalendar( );
        }

        if ( request != null )
        {
            locale = request.getLocale( );

            if ( Constants.PARAMETER_NEXT.equals( request.getParameter( Constants.PARAMETER_MONTH ) ) )
            {
                _cal.add( Calendar.MONTH, 1 );
            }
            else if ( Constants.PARAMETER_PREV.equals( request.getParameter( Constants.PARAMETER_MONTH ) ) )
            {
                _cal.add( Calendar.MONTH, -1 );
            }

            model.put( MARK_MONTH_URL, AppPathService.getBaseUrl( request ) + JSP_PORTAL_URL + POINT_INTERROGATION + PARAM_PAGE_ID + EGAL );
        }
        else
        {
            locale = I18nService.getDefaultLocale( );
        }

        model.put( MARK_PAGE_ID, getPageId( ) );
        model.put( MARK_MONTH, PortletMonthCalendar.build( locale, _cal, request ) );

        //Top event section
        Plugin plugin = PluginService.getPlugin( Constants.PLUGIN_NAME );

        if ( MiniCalendarPortletHome.showTopEvent( ) )
        {
            model.put( MARK_TOP_EVENTS, CalendarHome.findTopEventList( plugin ) );
        }

        model.put( MARK_EVENTS, getEvents( request, locale ) );

        return renderTemplate( request, TEMPLATE_PORTLET_DEFAULT, model );
    }

    /**
     * Returns the events of the day, or of the period, asked by the request
     * @param request the request, or null
     * @param locale the locale
     * @return the events, sorted by their start time
     */
    private List<Map<String, Object>> getEvents( HttpServletRequest request, Locale locale )
    {
        String strDateBegin = ( request != null ) ? request.getParameter( Constants.PARAMETER_DATE ) : StringUtils.EMPTY;

        // If there is no date begin in the parameter, then get today's date
        if ( StringUtils.isBlank( strDateBegin ) )
        {
            strDateBegin = DateUtil.getCurrentDateString( locale );
        }

        String [ ] arrayCalendar = Utils.getCalendarIds( request );
        Plugin pluginCalendar = PluginService.getPlugin( Constants.PLUGIN_NAME );
        Locale localeRequest = ( request != null ) ? request.getLocale( ) : Locale.getDefault( );
        Date dateBegin = DateUtil.formatDate( strDateBegin, localeRequest );

        // If there is a date end, then it is a search on a date interval
        String strDateEnd = ( request != null ) ? request.getParameter( Constants.PARAMETER_DATE_END ) : StringUtils.EMPTY;
        Date dateEnd = null;

        if ( StringUtils.isNotBlank( strDateEnd ) )
        {
            dateEnd = DateUtil.formatDate( strDateEnd, localeRequest );
        }

        if ( dateEnd == null )
        {
            dateEnd = dateBegin;
        }

        List<Event> listEvent = CDI.current( ).select( CalendarSearchService.class ).get( ).getSearchResults( arrayCalendar, null,
                StringUtils.EMPTY, dateBegin, dateEnd, pluginCalendar );
        List<Map<String, Object>> listEvents = new ArrayList<>( );

        if ( listEvent == null )
        {
            return listEvents;
        }

        listEvent.sort( Comparator.comparing( MiniCalendarPortlet::getStartTime ) );

        for ( Event event : listEvent )
        {
            Map<String, Object> mapEvent = new HashMap<>( );

            // Search on date interval, then display date begin and date end
            if ( StringUtils.isNotBlank( strDateEnd ) && !strDateBegin.equals( strDateEnd ) )
            {
                mapEvent.put( KEY_DATE, DateUtil.getDateString( event.getDate( ), locale ) );
                mapEvent.put( KEY_DATE_END, DateUtil.getDateString( event.getDateEnd( ), locale ) );
            }
            // Else only display the date on which the user has clicked
            else
            {
                mapEvent.put( KEY_DATE, strDateBegin );
                mapEvent.put( KEY_DATE_END, StringUtils.EMPTY );
            }

            mapEvent.put( KEY_TITLE, StringUtils.defaultString( event.getTitle( ) ) );
            mapEvent.put( KEY_DATETIME_BEGIN, StringUtils.defaultString( event.getDateTimeStart( ) ) );
            mapEvent.put( KEY_DESCRIPTION, StringUtils.defaultString( event.getDescription( ) ) );
            mapEvent.put( KEY_EVENT_ID, event.getId( ) );
            listEvents.add( mapEvent );
        }

        return listEvents;
    }

    /**
     * Returns the start time of an event, the start of the day when it has none
     * @param event the event
     * @return the start time
     */
    private static Date getStartTime( Event event )
    {
        try
        {
            return new SimpleDateFormat( "HH:mm" ).parse( event.getDateTimeStart( ) );
        }
        catch( ParseException | NullPointerException e )
        {
            return new Date( 0 );
        }
    }

    /**
     * Updates the current instance of the CalendarPortlet object
     */
    public void update( )
    {
        CDI.current( ).select( MiniCalendarPortletHome.class ).get( ).update( this );
    }

    /**
     * Removes the current instance of the CalendarPortlet object
     */
    public void remove( )
    {
        CDI.current( ).select( MiniCalendarPortletHome.class ).get( ).remove( this );
    }

    @Override
    public boolean canBeCachedForAnonymousUsers( )
    {
        return false;
    }

    @Override
    public boolean canBeCachedForConnectedUsers( )
    {
        return false;
    }
}
