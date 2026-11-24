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
package fr.paris.lutece.plugins.calendar.business.reminder;

import java.sql.Timestamp;

import fr.paris.lutece.portal.service.plugin.Plugin;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Facade of the log of the reminders sent to the subscribers of the calendars
 */
public final class ReminderSentHome
{
    private static IReminderSentDAO _dao = CDI.current( ).select( IReminderSentDAO.class ).get( );

    /**
     * Private constructor: static methods only
     */
    private ReminderSentHome( )
    {
    }

    /**
     * Claims the reminder of an occurrence for a subscriber: records it unless another run already did
     *
     * @param nOccurrenceId
     *            the occurrence
     * @param nSubscriberId
     *            the subscriber
     * @param plugin
     *            the plugin
     * @return true when the reminder is to be sent by the caller
     */
    public static boolean claim( int nOccurrenceId, int nSubscriberId, Plugin plugin )
    {
        return _dao.insertIfAbsent( nOccurrenceId, nSubscriberId, plugin );
    }

    /**
     * Releases a claim, when the reminder could not be sent, so that a next run tries again
     *
     * @param nOccurrenceId
     *            the occurrence
     * @param nSubscriberId
     *            the subscriber
     * @param plugin
     *            the plugin
     */
    public static void release( int nOccurrenceId, int nSubscriberId, Plugin plugin )
    {
        _dao.delete( nOccurrenceId, nSubscriberId, plugin );
    }

    /**
     * Removes the records older than a time
     *
     * @param tLimit
     *            the time
     * @param plugin
     *            the plugin
     */
    public static void removeOlderThan( Timestamp tLimit, Plugin plugin )
    {
        _dao.deleteOlderThan( tLimit, plugin );
    }
}
