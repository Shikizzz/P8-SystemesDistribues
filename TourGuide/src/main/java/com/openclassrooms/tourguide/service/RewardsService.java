package com.openclassrooms.tourguide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;
	
	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}
/*
	public void calculateRewards(User user){
		List<VisitedLocation> userLocations = user.getVisitedLocations().stream().toList();
		List<Attraction> attractions = gpsUtil.getAttractions();

		List<UserReward> userRewards = user.getUserRewards();
		CopyOnWriteArrayList<UserReward> modifiableUserRewards = new CopyOnWriteArrayList<>(userRewards);

		for(VisitedLocation visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				if(modifiableUserRewards.stream()
						.filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation, attraction)) {
						modifiableUserRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}
		user.setUserRewards(modifiableUserRewards);
	}*/

	public void calculateRewards(User user) throws ExecutionException, InterruptedException {
		List<VisitedLocation> locations = user.getVisitedLocations();
		List<CompletableFuture<VisitedLocation>> userLocations = new ArrayList<>();
		for(VisitedLocation visitedLocation : locations){
			userLocations.add(CompletableFuture.supplyAsync(()-> visitedLocation));
		}
		List<Attraction> attractions = gpsUtil.getAttractions();

		List<UserReward> userRewards = user.getUserRewards();
		CopyOnWriteArrayList<UserReward> modifiableUserRewards = new CopyOnWriteArrayList<>(userRewards);

		for(CompletableFuture<VisitedLocation> visitedLocation : userLocations) {
			for(Attraction attraction : attractions) {
				if(modifiableUserRewards.stream()
						.filter(r -> r.attraction.attractionName.equals(attraction.attractionName)).count() == 0) {
					if(nearAttraction(visitedLocation.get(), attraction)) {
						visitedLocation.thenRun(()-> {
							try {
								modifiableUserRewards.add(new UserReward(visitedLocation.get(), attraction, getRewardPoints(attraction, user)));
							} catch (InterruptedException e) {
								throw new RuntimeException(e);
							} catch (ExecutionException e) {
								throw new RuntimeException(e);
							}
						});
					}
				}
			}
		}
		user.setUserRewards(modifiableUserRewards);
	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location){
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction){
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	public int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2){
		double lat1 = Math.toRadians(loc1.latitude);
		double lon1 = Math.toRadians(loc1.longitude);
		double lat2 = Math.toRadians(loc2.latitude);
		double lon2 = Math.toRadians(loc2.longitude);
/*
		CompletableFuture<Double> angle =
				CompletableFuture.supplyAsync(()-> Math.sin(lat1) * Math.sin(lat2))
						.thenCombine(CompletableFuture.supplyAsync(()->Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2)), (a, b)->Math.acos(a+b));
*/

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
