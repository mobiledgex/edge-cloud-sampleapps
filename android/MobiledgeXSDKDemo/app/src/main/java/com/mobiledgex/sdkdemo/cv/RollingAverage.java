package com.mobiledgex.sdkdemo.cv;

import java.text.DecimalFormat;

/**
 * This class calculates a rolling average as values are added. Standard deviation is also provided.
 */
public class RollingAverage {
    private final long[] window;
    private float sum = 0f;
    private int fill;
    private int position;
    private ImageProcessorFragment.CloudletType cloudLetType;
    private String name;
    private long current;
    private boolean detailedStats = false; //TODO: Make a preference.

    public RollingAverage(ImageProcessorFragment.CloudletType cloudLetType, String name, int size) {
        this.cloudLetType = cloudLetType;
        this.name = name;
        this.window=new long[size];
    }

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

    public long getCurrent() { return current; }

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
