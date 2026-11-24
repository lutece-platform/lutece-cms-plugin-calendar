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
package fr.paris.lutece.plugins.calendar.business.details;

import fr.paris.lutece.portal.service.plugin.Plugin;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Facade of the details of the events
 */
public final class EventDetailsHome
{
    private static IEventDetailsDAO _dao = CDI.current( ).select( IEventDetailsDAO.class ).get( );

    /**
     * Private constructor: static methods only
     */
    private EventDetailsHome( )
    {
    }

    /**
     * Gives the details of an event
     *
     * @param nIdEvent
     *            the event
     * @param plugin
     *            the plugin
     * @return the details, empty ones when the event has none
     */
    public static EventDetails findByEvent( int nIdEvent, Plugin plugin )
    {
        EventDetails details = _dao.load( nIdEvent, plugin );

        if ( details == null )
        {
            details = new EventDetails( );
            details.setIdEvent( nIdEvent );
        }

        return details;
    }

    /**
     * Saves the details of an event
     *
     * @param details
     *            the details
     * @param plugin
     *            the plugin
     */
    public static void save( EventDetails details, Plugin plugin )
    {
        _dao.store( details, plugin );
    }

    /**
     * Removes the details of an event
     *
     * @param nIdEvent
     *            the event
     * @param plugin
     *            the plugin
     */
    public static void remove( int nIdEvent, Plugin plugin )
    {
        _dao.delete( nIdEvent, plugin );
    }
}
