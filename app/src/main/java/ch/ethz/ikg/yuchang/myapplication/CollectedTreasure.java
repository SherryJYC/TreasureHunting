package ch.ethz.ikg.yuchang.myapplication;


import java.io.Serializable;

/**
 *  class for defining collected treasure
 */

public class CollectedTreasure implements Serializable { //implements Parcelable {
    private String timestamp;
    private String name;
    private double lon;
    private double lat;
    private int collectedcoin;
    /**
     * Creates a new TreasureItem.
     *
     * @param timestamp time when this treasure is collected
     * @param name The name of this treasure.
     * @param lon The longitude.
     * @param lat The latitude.
     * @param collectedcoin the coins collected for this treasure, including maxcoin and bonus
     */

    public CollectedTreasure(String timestamp,String name, double lon, double lat, int collectedcoin){
        this.timestamp = timestamp;
        this.name = name;
        this.lat = lat;
        this.lon = lon;
        this.collectedcoin = collectedcoin;
    }

    public void setTimestamp(String timestamp){
        this.timestamp = timestamp;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setLon(double lon){
        this.lon = lon;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }
    public void setCollectedcoin(int collectedcoin) {
        this.collectedcoin = collectedcoin;
    }

    public String getTimestamp() {
        return timestamp;
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

    public int getCollectedcoin() {
        return collectedcoin;
    }
}
