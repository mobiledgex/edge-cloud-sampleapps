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

package com.mobiledgex.computervision;

import java.text.DecimalFormat;

/**
 * This class calculates a rolling average as values are added. Standard deviation is also provided.
 */
public class RollingAverage {
    private final long[] window;
    private float sum = 0f;
    private int fill;
    private int position;
    private ImageServerInterface.CloudletType cloudLetType;
    private String name;
    private long current;

    private boolean detailedStats = false; //TODO: Make a preference.

    /**
     * Constructor for the RollingAverage.
     * @param cloudLetType  The cloudlet type. Used for the statistics text.
     * @param name  The name of the set. Used for the statistics text.
     * @param size  The maximum number of values to keep in the set.
     */
    public RollingAverage(ImageServerInterface.CloudletType cloudLetType, String name, int size) {
        this.cloudLetType = cloudLetType;
        this.name = name;
        this.window=new long[size];
    }

    /**
     * Add a number to the set.
     * @param number  The number to add to the set.
     */
    public void add(long number) {
        current = number;

        if(fill==window.length){
            sum-=window[position];
        }else{
            fill++;
        }

        sum+=number;
        window[position++]=number;

        if(position == window.length){
            position=0;
        }

    }

    /**
     * Return the most recently added value.
     * @return  The most recently added value.
     */
    public long getCurrent() { return current; }

    /**
     * Return the rolling average.
     * @return  The rolling average.
     */
    public long getAverage() {
        return (long) (sum / fill);
    }

    /**
     * Get the standard deviation of the entire set.
     * @return Population Standard Deviation, σ
     */
    public long getStdDev() {
        double avg = getAverage();
        double sum = 0;

        for (int i = 0; i < fill; i++) {
            sum += Math.pow(window[i] - avg, 2);
        }

        return (long) Math.sqrt(sum/fill);
    }

    /**
     * Sets whether to include each sample in the set when {@link #getStatsText()} is called.
     * @param detailedStats
     */
    public void setDetailedStats(boolean detailedStats) {
        this.detailedStats = detailedStats;
    }

    /**
     * Get a textual summary of the stats.
     * @return  String to display in network stats window.
     */
    public String getStatsText() {
        if(fill == 0) {
            return "";
        }
        String stats = cloudLetType + " " + name + " Latency:\n";
        DecimalFormat decFor = new DecimalFormat("#.##");
        long min = Integer.MAX_VALUE;
        long max = -1;
        for(int i = 0; i < fill; i++) {
            if(detailedStats) {
                stats += i + ". time=" + decFor.format(window[i] / 1000000) + " ms\n";
            }
            if(window[i] / 1000000 < min) {
                min = window[i] / 1000000;
            }
            if(window[i] / 1000000 > max) {
                max = window[i] / 1000000;
            }
        }
        String avg = decFor.format(getAverage() / 1000000);
        String stdDev = decFor.format(getStdDev() / 1000000);
        stats += "min/avg/max/stddev = "+decFor.format(min)+"/"+avg+"/"+decFor.format(max)+"/"+stdDev+" ms";
        return stats;
    }
}
