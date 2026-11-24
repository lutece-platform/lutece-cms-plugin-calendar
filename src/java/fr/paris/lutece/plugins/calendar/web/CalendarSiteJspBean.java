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
package fr.paris.lutece.plugins.calendar.web;

import fr.paris.lutece.plugins.calendar.service.CalendarEventFeedService;
import fr.paris.lutece.plugins.calendar.service.CalendarExportService;
import fr.paris.lutece.plugins.calendar.service.CalendarIcalFeedService;
import fr.paris.lutece.plugins.calendar.service.CalendarService;
import fr.paris.lutece.portal.service.message.SiteMessageException;
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.portal.service.util.AppPathService;

import java.io.IOException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * The front office entry points reached by a JSP rather than by the XPage: the links sent by e-mail and the downloads
 */
@RequestScoped
@Named
public class CalendarSiteJspBean
{
    private static final String URL_SHOW_OCCURRENCE = "jsp/site/Portal.jsp?page=calendar&action=show_result&event_id=%d&agenda=%d";

    @Inject
    private CalendarApp _calendarApp;

    @Inject
    private CalendarService _calendarService;

    @Inject
    private CalendarEventFeedService _feedService;

    @Inject
    private CalendarIcalFeedService _icalFeedService;

    @Inject
    private CalendarExportService _exportService;

    /**
     * Sends the event to a friend
     * @param request the request
     * @return the url to redirect to, the site message page when a message tells the outcome
     */
    public String doSendToFriend( HttpServletRequest request )
    {
        try
        {
            return _calendarApp.doSendToFriend( request );
        }
        catch( SiteMessageException e )
        {
            return AppPathService.getSiteMessageUrl( request );
        }
    }

    /**
     * Validates a subscription from the link of the confirmation e-mail
     * @param request the request
     * @return the url of the site message telling the outcome (subscribed, link expired or unknown)
     */
    public String doSubscription( HttpServletRequest request )
    {
        try
        {
            _calendarApp.doSubscription( request );
        }
        catch( SiteMessageException e )
        {
            return AppPathService.getSiteMessageUrl( request );
        }

        return AppPathService.getBaseUrl( request );
    }

    /**
     * Exports a calendar: the RSS feed of the events ( action=rss ), the JSON file of a calendar ( format=json ), or
     * otherwise its iCalendar file. The address is kept from the former exports, so that their links keep working
     * @param request the request
     * @param response the response holding the file
     * @return an empty string, for the JSP
     */
    public String downloadCalendarFile( HttpServletRequest request, HttpServletResponse response )
    {
        try
        {
            if ( Constants.ACTION_RSS.equals( request.getParameter( Constants.PARAMETER_ACTION ) ) )
            {
                _exportService.writeRss( request, response );
            }
            else if ( CalendarExportService.FORMAT_JSON.equals( request.getParameter( CalendarExportService.PARAMETER_FORMAT ) ) )
            {
                _exportService.writeJson( request, response );
            }
            else
            {
                _icalFeedService.writeFeed( request, response );
            }
        }
        catch( IOException e )
        {
            throw new AppException( e.getMessage( ), e );
        }

        return "";
    }

    /**
     * Writes the JSON feed of the occurrences of the calendars the visitor may see, for the EventCalendar views
     * @param request the request, with the parameters start, end, calendar_id and category_id
     * @param response the response
     * @return an empty string, for the JSP
     */
    public String writeEventsFeed( HttpServletRequest request, HttpServletResponse response )
    {
        try
        {
            _feedService.writeFeed( request, response, _calendarService.getAgendaResources( request ),
                    occurrence -> String.format( URL_SHOW_OCCURRENCE, occurrence.getEventId( ), occurrence.getIdCalendar( ) ), false );
        }
        catch( IOException e )
        {
            throw new AppException( e.getMessage( ), e );
        }

        return "";
    }
    /**
     * Writes the iCalendar subscription feed of a calendar open to everybody
     * @param request the request, with the parameter calendar_id
     * @param response the response
     * @return an empty string, for the JSP
     */
    public String writeIcalFeed( HttpServletRequest request, HttpServletResponse response )
    {
        try
        {
            _icalFeedService.writeFeed( request, response );
        }
        catch( IOException e )
        {
            throw new AppException( e.getMessage( ), e );
        }

        return "";
    }
}
