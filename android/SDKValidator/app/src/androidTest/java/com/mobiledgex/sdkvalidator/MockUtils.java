/**
 * Copyright 2018-2020 MobiledgeX, Inc. All rights and licenses reserved.
 * MobiledgeX, Inc. 156 2nd Street #408, San Francisco, CA 94105
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobiledgex.sdkvalidator;

import android.location.Location;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import distributed_match_engine.AppClient;
import distributed_match_engine.AppClient.QosPosition;
import distributed_match_engine.LocOuterClass;

public class MockUtils {
    private final static String TAG = "MockUtils";

    public static Location createLocation(String provider, double longitude, double latitude) {
        Location loc = new Location(provider);
        loc.setLongitude(longitude);
        loc.setLatitude(latitude);
        loc.setTime(System.currentTimeMillis());
        return loc;
    }

    public static ArrayList<QosPosition> createQosPositionArray(Location firstLocation, double direction_degrees, double totalDistanceKm, double increment) {
        // Create a bunch of locations to get QOS information. Server is to be proxied by the DME server.
        ArrayList<QosPosition> positions = new ArrayList<>();

        double kmPerDegreeLong = 111.32; // at Equator
        double kmPerDegreeLat = 110.57; // at Equator
        double addLongitude = (Math.cos(direction_degrees/(Math.PI/180)) * increment) / kmPerDegreeLong;
        double addLatitude = (Math.sin(direction_degrees/(Math.PI/180)) * increment) / kmPerDegreeLat;
        double i = 0d;
        double longitude = firstLocation.getLongitude();
        double latitude = firstLocation.getLatitude();

        long id = 1;

        while (i < totalDistanceKm) {
            longitude += addLongitude;
            latitude += addLatitude;
            i += increment;

            // FIXME: No time is attached to GPS location, as that breaks the server!
            LocOuterClass.Loc loc = LocOuterClass.Loc.newBuilder()
                    .setLongitude(longitude)
                    .setLatitude(latitude)
                .build();

            QosPosition np = AppClient.QosPosition.newBuilder()
                    .setPositionid(id++)
                    .setGpsLocation(loc)
                    .build();

            positions.add(np);
        }

        return positions;
    }

    /**
     * Returns a destination long/lat as a Location object, along direction (in degrees), some distance in kilometers away.
     *
     * @param longitude_src
     * @param latitude_src
     * @param direction_degrees
     * @param kilometers
     * @return
     */
    public static Location createLocation(double longitude_src, double latitude_src, double direction_degrees, double kilometers) {
        double direction_radians = direction_degrees * (Math.PI / 180);

        // Provider is static class name:
        Location newLoc = new Location(MethodHandles.lookup().lookupClass().getName());

        // Not accurate:
        double kmPerDegreeLat = 110.57; //at Equator
        double kmPerDegreeLong = 111.32; //at Equator
        newLoc.setLongitude(longitude_src + kilometers/kmPerDegreeLong * Math.cos(direction_radians));
        newLoc.setLatitude(latitude_src + kilometers/kmPerDegreeLat * Math.sin(direction_radians));

        return newLoc;
    }

    public static LocOuterClass.Loc androidToMessageLoc(Location location) {
        return LocOuterClass.Loc.newBuilder()
                .setLatitude(location.getLatitude())
                .setLongitude(location.getLongitude())
                .setTimestamp(LocOuterClass.Timestamp.newBuilder()
                        .setSeconds(System.currentTimeMillis()/1000)
                        .build())
                .build();
    }
}
