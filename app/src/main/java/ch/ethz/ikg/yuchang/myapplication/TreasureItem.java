package ch.ethz.ikg.yuchang.myapplication;

import java.util.ArrayList;

/**
 * Help handle row in treasure.csv
 * Each row is a treatureItem with following attributes:
 * treasure name;longitude;latitude;maximum coins
*/

public class TreasureItem {
    private String name;
    private double lon;
    private double lat;
    private int maxcoin;
    private int complete;
    /**
     * Creates a new TreasureItem.
     *
     * @param name The name of this treasure.
     * @param lon The longitude.
     * @param lat The latitude.
     * @param maxcoin The maxcoin of this treasure defined by csv file.
     * complete: 0 is not complete (default), 1 is complete
     */

    public TreasureItem(String name, double lat, double lon, int maxcoin) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.maxcoin = maxcoin;
        this.complete = 0; // default 0: not complete
    }

    public void setName(String name){
        this.name = name;
    }
    public void setLon(double lon){
        this.lon = lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setMaxcoin(int maxcoin) {
        this.maxcoin = maxcoin;
    }

    public void setComplete(int complete) {
        this.complete = complete;
    }

    public int getMaxcoin() {
        return this.maxcoin;
    }

    public String getName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public int getComplete() { return complete; }

    @Override
    public String toString() {
        return super.toString();
    }
}
