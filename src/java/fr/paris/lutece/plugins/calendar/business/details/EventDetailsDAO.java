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

import java.sql.Types;

import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.util.sql.DAOUtil;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Access to the table calendar_event_details
 */
@ApplicationScoped
public final class EventDetailsDAO implements IEventDetailsDAO
{
    private static final String SQL_QUERY_SELECT = "SELECT id_event, price_type, price, attendance_mode, online_url, organizer_name, organizer_email, organizer_phone, organizer_url, registration_url, accessibility, latitude, longitude FROM calendar_event_details WHERE id_event = ?";
    private static final String SQL_QUERY_INSERT = "INSERT INTO calendar_event_details ( id_event, price_type, price, attendance_mode, online_url, organizer_name, organizer_email, organizer_phone, organizer_url, registration_url, accessibility, latitude, longitude ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";
    private static final String SQL_QUERY_DELETE = "DELETE FROM calendar_event_details WHERE id_event = ?";

    /**
     * {@inheritDoc}
     */
    @Override
    public EventDetails load( int nIdEvent, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_SELECT, plugin ) )
        {
            daoUtil.setInt( 1, nIdEvent );
            daoUtil.executeQuery( );

            if ( !daoUtil.next( ) )
            {
                return null;
            }

            EventDetails details = new EventDetails( );
            int nIndex = 1;
            details.setIdEvent( daoUtil.getInt( nIndex++ ) );
            details.setPriceType( daoUtil.getString( nIndex++ ) );
            details.setPrice( daoUtil.getString( nIndex++ ) );
            details.setAttendanceMode( daoUtil.getString( nIndex++ ) );
            details.setOnlineUrl( daoUtil.getString( nIndex++ ) );
            details.setOrganizerName( daoUtil.getString( nIndex++ ) );
            details.setOrganizerEmail( daoUtil.getString( nIndex++ ) );
            details.setOrganizerPhone( daoUtil.getString( nIndex++ ) );
            details.setOrganizerUrl( daoUtil.getString( nIndex++ ) );
            details.setRegistrationUrl( daoUtil.getString( nIndex++ ) );
            details.setAccessibility( daoUtil.getString( nIndex++ ) );
            details.setLatitude( getDouble( daoUtil, nIndex++ ) );
            details.setLongitude( getDouble( daoUtil, nIndex++ ) );

            return details;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store( EventDetails details, Plugin plugin )
    {
        delete( details.getIdEvent( ), plugin );

        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_INSERT, plugin ) )
        {
            int nIndex = 1;
            daoUtil.setInt( nIndex++, details.getIdEvent( ) );
            daoUtil.setString( nIndex++, details.getPriceType( ) );
            daoUtil.setString( nIndex++, details.getPrice( ) );
            daoUtil.setString( nIndex++, details.getAttendanceMode( ) );
            daoUtil.setString( nIndex++, details.getOnlineUrl( ) );
            daoUtil.setString( nIndex++, details.getOrganizerName( ) );
            daoUtil.setString( nIndex++, details.getOrganizerEmail( ) );
            daoUtil.setString( nIndex++, details.getOrganizerPhone( ) );
            daoUtil.setString( nIndex++, details.getOrganizerUrl( ) );
            daoUtil.setString( nIndex++, details.getRegistrationUrl( ) );
            daoUtil.setString( nIndex++, details.getAccessibility( ) );
            setDouble( daoUtil, nIndex++, details.getLatitude( ) );
            setDouble( daoUtil, nIndex++, details.getLongitude( ) );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( int nIdEvent, Plugin plugin )
    {
        try ( DAOUtil daoUtil = new DAOUtil( SQL_QUERY_DELETE, plugin ) )
        {
            daoUtil.setInt( 1, nIdEvent );
            daoUtil.executeUpdate( );
        }
    }

    /**
     * Reads a nullable decimal column
     *
     * @param daoUtil
     *            the result
     * @param nIndex
     *            the column
     * @return the value, or null
     */
    private static Double getDouble( DAOUtil daoUtil, int nIndex )
    {
        Object value = daoUtil.getObject( nIndex );

        return ( value instanceof Number ) ? Double.valueOf( ( (Number) value ).doubleValue( ) ) : null;
    }

    /**
     * Writes a nullable decimal parameter
     *
     * @param daoUtil
     *            the statement
     * @param nIndex
     *            the parameter
     * @param value
     *            the value, or null
     */
    private static void setDouble( DAOUtil daoUtil, int nIndex, Double value )
    {
        if ( value == null )
        {
            daoUtil.setNull( nIndex, Types.DOUBLE );
        }
        else
        {
            daoUtil.setDouble( nIndex, value );
        }
    }
}
