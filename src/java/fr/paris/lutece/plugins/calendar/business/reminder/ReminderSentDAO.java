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
import fr.paris.lutece.portal.service.util.AppException;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Access to the table calendar_reminder_sent
 */
@ApplicationScoped
public final class ReminderSentDAO implements IReminderSentDAO
{
    private static final String SQL_QUERY_SELECT = "SELECT COUNT(*) FROM calendar_reminder_sent WHERE id_occurrence = ? AND id_subscriber = ?";
    private static final String SQL_QUERY_INSERT = "INSERT INTO calendar_reminder_sent ( id_occurrence, id_subscriber, date_sent ) VALUES ( ?, ?, ? )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM calendar_reminder_sent WHERE id_occurrence = ? AND id_subscriber = ?";
    private static final String SQL_QUERY_DELETE_OLDER = "DELETE FROM calendar_reminder_sent WHERE date_sent < ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insertIfAbsent( int nOccurrenceId, int nSubscriberId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nOccurrenceId );
            daoUtil.setInt( 2, nSubscriberId );
            daoUtil.executeQuery( );

            if ( daoUtil.next( ) && daoUtil.getInt( 1 ) > 0 )
            {
                return false;
            }
        }

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, plugin ) )
        {
            daoUtil.setInt( 1, nOccurrenceId );
            daoUtil.setInt( 2, nSubscriberId );
            daoUtil.setTimestamp( 3, new Timestamp( System.currentTimeMillis( ) ) );
            daoUtil.executeUpdate( );

            return daoUtil.getReturnedRowCount( ) == 1;
        }
        catch( AppException e )
        {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nOccurrenceId, int nSubscriberId, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nOccurrenceId );
            daoUtil.setInt( 2, nSubscriberId );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOlderThan( Timestamp tLimit, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE_OLDER, plugin ) )
        {
            daoUtil.setTimestamp( 1, tLimit );
            daoUtil.executeUpdate( );
        }
    }
}
