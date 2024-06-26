package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.model.DTO.AttractionDTO;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.model.user.User;
import com.openclassrooms.tourguide.model.user.UserReward;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;

import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;
	private ExecutorService executorService = Executors.newFixedThreadPool(128);

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) throws ExecutionException, InterruptedException {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user).get();
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public CompletableFuture<VisitedLocation> trackUserLocation(User user) throws ExecutionException, InterruptedException {
		CompletableFuture<VisitedLocation> visitedLocation = CompletableFuture.supplyAsync(() -> gpsUtil.getUserLocation(user.getUserId()), executorService);
		CompletableFuture updateUserVisitedLocations = new CompletableFuture<>();
		updateUserVisitedLocations = visitedLocation
				.thenAccept(supplyResult -> {
					try {
						user.addToVisitedLocations(visitedLocation.get());
						rewardsService.calculateRewards(user);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} catch (ExecutionException e) {
						throw new RuntimeException(e);
					}
				});
		return visitedLocation;
	}

	public TreeMap<Integer, Attraction> getAttractionsDistance(VisitedLocation visitedLocation){
		TreeMap<Integer, Attraction> sortedDistanceMap = new TreeMap<>();
		for (Attraction attraction : gpsUtil.getAttractions()) {
			sortedDistanceMap.put((int) rewardsService.getDistance(visitedLocation.location, attraction), attraction);
		}
		return sortedDistanceMap;
	}

	public List<Attraction> getNearByAttractionsAsList(VisitedLocation visitedLocation){
		List<Attraction> nearbyAttractions = new ArrayList<>();
		TreeMap<Integer, Attraction> sortedAttractionsByDistance = getAttractionsDistance(visitedLocation);
		for (int i=0; i<5; i++) {
			nearbyAttractions.add(sortedAttractionsByDistance.firstEntry().getValue());
			sortedAttractionsByDistance.remove(sortedAttractionsByDistance.firstKey());
		}
		return nearbyAttractions;
	}

	public List<AttractionDTO> getNearByAttractions(VisitedLocation visitedLocation){
		List<AttractionDTO> attractionDTOs = new ArrayList<>();
		List<Attraction> sortedDistanceList = getNearByAttractionsAsList(visitedLocation);
		for (Attraction attraction : sortedDistanceList) {
			AttractionDTO attractionDTO = new AttractionDTO();
			attractionDTO.setName(attraction.attractionName);
			attractionDTO.setAttractionsLatitude(attraction.latitude);
			attractionDTO.setAttractionsLongitude(attraction.longitude);
			attractionDTO.setUsersLatitude(visitedLocation.location.latitude);
			attractionDTO.setUsersLongitude(visitedLocation.location.longitude);
			attractionDTO.setDistance(rewardsService.getDistance(visitedLocation.location, attraction));
			attractionDTO.setRewardsPoints(rewardsService.getRewardPoints(attraction, new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com")));
			attractionDTOs.add(attractionDTO);
		}
		return attractionDTOs;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 *
	 * Methods Below: For Internal Testing
	 *
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
