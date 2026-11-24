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

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.calendar.business.CalendarHome;
import fr.paris.lutece.plugins.calendar.business.OccurrenceEvent;
import fr.paris.lutece.plugins.calendar.web.Constants;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.util.AppPropertiesService;

/**
 * The events proposed from the site: a calendar that accepts proposals lets logged-in users add events, which get the
 * status proposed and stay out of every public view (calendar views, portlets, upcoming events, exports, reminders,
 * page of the event) until an administrator confirms them by changing their status in the back office
 */
public final class EventProposals
{
    private static final String DEFAULT_STATUS_PROPOSED = "proposed";

    /**
     * Private constructor
     */
    private EventProposals( )
    {
    }

    /**
     * Returns the status of a proposed event
     * @return the status, proposed unless the property calendar.event.status.proposed says otherwise
     */
    public static String getProposedStatus( )
    {
        return AppPropertiesService.getProperty( Constants.PROPERTY_EVENT_STATUS_PROPOSED, DEFAULT_STATUS_PROPOSED );
    }

    /**
     * Tells whether a status is the one of a proposed event
     * @param strStatus the status
     * @return true when the event is a proposal waiting for confirmation
     */
    public static boolean isProposed( String strStatus )
    {
        return getProposedStatus( ).equals( StringUtils.trimToEmpty( strStatus ) );
    }

    /**
     * Keeps the occurrences the public may see
     * @param listOccurrences the occurrences
     * @return a new list without the proposed occurrences
     */
    public static List<OccurrenceEvent> publicOnly( List<OccurrenceEvent> listOccurrences )
    {
        return listOccurrences.stream( ).filter( occurrence -> !isProposed( occurrence.getStatus( ) ) ).toList( );
    }

    /**
     * Returns the proposals of a calendar waiting for confirmation: its events all of whose occurrences are proposed
     * @param nAgendaId the identifier of the calendar
     * @param plugin the plugin
     * @return the identifiers of the proposed events
     */
    public static Set<Integer> getProposedEventIds( int nAgendaId, Plugin plugin )
    {
        return CalendarHome.findEventIdsWithOnlyStatus( nAgendaId, getProposedStatus( ), plugin );
    }
}
