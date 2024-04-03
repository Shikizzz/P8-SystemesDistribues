package com.openclassrooms.tourguide.model.DTO;

public class AttractionDTO {
    private String name;
    private double attractionsLatitude;
    private double attractionsLongitude;
    private double usersLatitude;
    private double usersLongitude;
    private double distance;
    private int rewardsPoints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAttractionsLatitude() {
        return attractionsLatitude;
    }

    public void setAttractionsLatitude(double attractionsLatitude) {
        this.attractionsLatitude = attractionsLatitude;
    }

    public double getAttractionsLongitude() {
        return attractionsLongitude;
    }

    public void setAttractionsLongitude(double attractionsLongitude) {
        this.attractionsLongitude = attractionsLongitude;
    }

    public double getUsersLatitude() {
        return usersLatitude;
    }

    public void setUsersLatitude(double usersLatitude) {
        this.usersLatitude = usersLatitude;
    }

    public double getUsersLongitude() {
        return usersLongitude;
    }

    public void setUsersLongitude(double usersLongitude) {
        this.usersLongitude = usersLongitude;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getRewardsPoints() {
        return rewardsPoints;
    }

    public void setRewardsPoints(int rewardsPoints) {
        this.rewardsPoints = rewardsPoints;
    }

    @Override
    public String toString() {
        return "AttractionDTO{" +
                "name='" + name + '\'' +
                ", attractionsLatitude=" + attractionsLatitude +
                ", attractionsLongitude=" + attractionsLongitude +
                ", usersLatitude=" + usersLatitude +
                ", usersLongitude=" + usersLongitude +
                ", distance=" + distance +
                ", rewardsPoints=" + rewardsPoints +
                '}';
    }
}
