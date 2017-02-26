package com.kevinmessier.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class TrafficData {

	private String dataDir = "./data/";

	private String home;
	private String work;
	private String apiKey;
	private LocalDateTime storedTimestamp;

	/////////////////
	// Main method //
	/////////////////
	public static void main(String[] args) {

		// TODO logging

		TrafficData t = new TrafficData();

		 if (args.length != 3) {
			 System.out.println("ERROR: You must supply 3 arguments");
			 t.printUsage();
			 System.exit(1);
		 }
		
		 t.setHome(args[0]);
		 t.setWork(args[1]);
		 t.setApiKey(args[2]);

		// Get current date and time
		t.setStoredTimestamp(LocalDateTime.now());
		String currentDate = t.getStoredTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		String currentTime = t.getStoredTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		System.out.println("Current date and time: " + currentDate + " " + currentTime);

		// Check for data dir
		System.out.println("Checking for " + t.dataDir);
		if (t.checkForFile(t.dataDir)) {
			System.out.println(t.dataDir + " exists");
		} else {
			System.out.print(t.dataDir + " does not exist. Creating...");
			File dir = new File(t.dataDir);
			if (dir.mkdir()) {
				System.out.println("Done");
			} else {
				System.out.println("ERROR");
				System.out.println("Couldn't create " + t.dataDir + ", Exiting");
				System.exit(1);
			}
		}

		// Get current traffic data as JSON objects
		System.out.print("Getting traffic data... ");
		JSONObject trafficDataToWork = t.getTrafficData(t.buildUrl(t.getHome(), t.getWork(), t.getApiKey()));
		JSONObject trafficDataToHome = t.getTrafficData(t.buildUrl(t.getWork(), t.getHome(), t.getApiKey()));
		System.out.println("Done");

		// Extract and transform data
		JSONObject driveToWorkData = t.tranformData(trafficDataToWork, currentDate, currentTime, t.getHome(),
				t.getWork());
		JSONObject driveToHomeData = t.tranformData(trafficDataToHome, currentDate, currentTime, t.getWork(),
				t.getHome());

		// Add data to files
		String toWorkDataFile = t.dataDir + currentDate + "_to_work.json";
		String toHomeDataFile = t.dataDir + currentDate + "_to_home.json";
		
		if(!t.addDataToJsonFile(toWorkDataFile, driveToWorkData)){
			System.out.println("Error writing data to file " + toWorkDataFile);
		}
		
		if(!t.addDataToJsonFile(toHomeDataFile, driveToHomeData)){
			System.out.println("Error writing data to file " + toHomeDataFile);
		}
	}

	private String buildUrl(String origin, String destination, String apiKey) {
		return "https://maps.googleapis.com/maps/api/directions/json?units=imperial&origin="
				+ formatLocationString(origin) + "&destination=" + formatLocationString(destination)
				+ "&departure_time=now&key=" + apiKey;
	}

	private static String formatLocationString(String location) {
		return location.replace(" ", "+");
	}

	private JSONObject getTrafficData(String url) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);

		CloseableHttpResponse response = null;
		try {
			response = httpClient.execute(httpGet);
		} catch (ClientProtocolException e) {
			System.out.println("Error performing HTTP GET");
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Error performing HTTP GET");
			e.printStackTrace();
			System.exit(1);
		}

		HttpEntity entity = response.getEntity();

		JSONObject jsonObject = null;
		if (entity != null) {
			// Parse JSON response
			JSONParser jp = new JSONParser();

			Object obj = null;
			try {
				obj = jp.parse(EntityUtils.toString(entity));
			} catch (ParseException e) {
				System.out.println("Error parsing JSON");
				e.printStackTrace();
				System.exit(1);
			} catch (org.json.simple.parser.ParseException e) {
				System.out.println("Error parsing JSON");
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				System.out.println("Error parsing JSON");
				e.printStackTrace();
				System.exit(1);
			}

			jsonObject = (JSONObject) obj;
		}

		return jsonObject;
	}

	private JSONObject tranformData(JSONObject json, String date, String time, String origin, String destination) {
		// Extract data from Google Maps API response
		JSONArray routesArray = (JSONArray) json.get("routes");
		JSONObject route = (JSONObject) routesArray.get(0);
		String summary = (String) route.get("summary");
		JSONArray legsArray = (JSONArray) route.get("legs");
		JSONObject leg_0 = (JSONObject) legsArray.get(0);
		JSONObject durationInTrafficObj = (JSONObject) leg_0.get("duration_in_traffic");
		String durationInTraffic = (String) durationInTrafficObj.get("text");

		JSONObject returnData = new JSONObject();
		returnData.put("date", date);
		returnData.put("time", time);
		returnData.put("origin", origin);
		returnData.put("destination", destination);
		returnData.put("route", summary);
		returnData.put("durationInTraffic", convertTimeStringToMinutes(durationInTraffic));

		// System.out.println(returnData.toJSONString());

		return returnData;
	}

	private boolean checkForFile(String filePath) {
		File f = new File(filePath);
		if (f.exists())
			return true;
		else
			return false;
	}

	private boolean addDataToJsonFile(String fileName, JSONObject jsonData) {

		JSONArray newJSONArray;
		
		// Check for data file
		if (checkForFile(fileName)) {
			System.out.print(fileName + " exists. Adding data... ");

			// Load file and add data
			JSONParser jp = new JSONParser();
			try {
				newJSONArray = (JSONArray) jp.parse(new FileReader(new File(fileName)));
			} catch (FileNotFoundException e) {
				System.out.println("\nCouldn't find file " + fileName + " (This should never be printed)");
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				System.out.println("\nIOException trying to parse " + fileName);
				e.printStackTrace();
				return false;
			} catch (org.json.simple.parser.ParseException e) {
				System.out.println("\nException parsing " + fileName);
				e.printStackTrace();
				return false;
			}
			
			// Add new data
			newJSONArray.add(jsonData);
		}
		// Create a new JSONArray with new data
		else {
			System.out.print(fileName + " doesn't exist. Creating and adding data... ");
			newJSONArray = new JSONArray();
			newJSONArray.add(jsonData);
		}

		// Write to file
		FileWriter writer;
		try {
			writer = new FileWriter(fileName);
			writer.write(newJSONArray.toJSONString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			System.out.println("\nIOException writing to file " + fileName);
			e.printStackTrace();
			return false;
		}
		
		System.out.println("Done");

		return true;
	}

	private int convertTimeStringToMinutes(String timeString) {
		String[] parts = timeString.split(" ");
		if (parts.length == 4) {
			int hours = Integer.parseInt(parts[0]);
			int minutes = Integer.parseInt(parts[2]);
			return (hours * 60) + minutes;
		} else if (parts.length == 2) {
			return Integer.parseInt(parts[0]);
		} else {
			return 0;
		}
	}

	/////////////////////////
	// Getters and setters //
	/////////////////////////
	public String getHome() {
		return home;
	}

	public void setHome(String location) {
		this.home = location;
	}

	public String getWork() {
		return work;
	}

	public void setWork(String location) {
		this.work = location;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public LocalDateTime getStoredTimestamp() {
		return storedTimestamp;
	}

	public void setStoredTimestamp(LocalDateTime currentDateTime) {
		this.storedTimestamp = currentDateTime;
	}

	///////////
	// Usage //
	///////////
	private void printUsage() {
		System.out.println("Usage:");
		System.out.println("TrafficData <home address> <work> <google maps api key>");
	}
}
