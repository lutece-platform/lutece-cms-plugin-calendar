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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A small static preview of the place of an event: the OpenStreetMap tiles covering a frame centred on its coordinates,
 * placed by the server so that the page needs no map library and no script. The tiles come from the same service as
 * the "view on the map" link; the template shows the OpenStreetMap attribution its licence requires.
 */
public final class MapPreview
{
    /** Width of the preview, in CSS pixels */
    public static final int WIDTH = 320;
    /** Height of the preview, in CSS pixels */
    public static final int HEIGHT = 140;
    private static final int ZOOM = 16;
    private static final int TILE_SIZE = 256;
    private static final String URL_TILE = "https://tile.openstreetmap.org/%d/%d/%d.png";

    /**
     * Private constructor
     */
    private MapPreview( )
    {
    }

    /**
     * Gives the tiles of the preview of a place
     * @param dLatitude the latitude, in degrees
     * @param dLongitude the longitude, in degrees
     * @return the tiles, each one a map with its url and its left and top position in the frame, in pixels
     */
    public static List<Map<String, Object>> getTiles( double dLatitude, double dLongitude )
    {
        int nTiles = 1 << ZOOM;
        double dLatitudeRadians = Math.toRadians( Math.max( -85.0511, Math.min( 85.0511, dLatitude ) ) );
        double dX = ( dLongitude + 180.0 ) / 360.0 * nTiles * TILE_SIZE;
        double dY = ( 1.0 - Math.log( Math.tan( dLatitudeRadians ) + 1.0 / Math.cos( dLatitudeRadians ) ) / Math.PI ) / 2.0 * nTiles * TILE_SIZE;
        double dLeft = dX - WIDTH / 2.0;
        double dTop = dY - HEIGHT / 2.0;
        List<Map<String, Object>> listTiles = new ArrayList<>( );

        for ( int nTileY = (int) Math.floor( dTop / TILE_SIZE ); nTileY <= (int) Math.floor( ( dTop + HEIGHT ) / TILE_SIZE ); nTileY++ )
        {
            for ( int nTileX = (int) Math.floor( dLeft / TILE_SIZE ); nTileX <= (int) Math.floor( ( dLeft + WIDTH ) / TILE_SIZE ); nTileX++ )
            {
                if ( ( nTileY >= 0 ) && ( nTileY < nTiles ) )
                {
                    Map<String, Object> tile = new HashMap<>( );
                    tile.put( "url", String.format( Locale.ROOT, URL_TILE, ZOOM, Math.floorMod( nTileX, nTiles ), nTileY ) );
                    tile.put( "left", (int) Math.round( nTileX * (double) TILE_SIZE - dLeft ) );
                    tile.put( "top", (int) Math.round( nTileY * (double) TILE_SIZE - dTop ) );
                    listTiles.add( tile );
                }
            }
        }

        return listTiles;
    }
}
