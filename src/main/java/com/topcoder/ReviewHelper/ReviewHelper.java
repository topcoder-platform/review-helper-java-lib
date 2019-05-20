package com.topcoder.ReviewHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.appirio.tech.core.api.v3.util.jwt.JWTTokenGenerator;

public class ReviewHelper {

	String clientId = "";
	String clientSecret = "";
	String audience = "";
	String m2mAuthDomain = "";
	String tcDomain = "";

	private static Properties loadPropertyFile() throws Exception {
		Properties props = new Properties();
		FileInputStream in = new FileInputStream("toekn.properties");
		props.load(in);
		in.close();

		return props;
	}

	// String clientId, String clientSecret, String audience, String m2mAuthDomain
	public static String getToken() throws Exception {
		Properties props = loadPropertyFile();

		JWTTokenGenerator jwtTokenGenerator = JWTTokenGenerator.getInstance(props.getProperty("clientId"),
				props.getProperty("clientSecret"), props.getProperty("audience"), props.getProperty("m2mAuthDomain"),
				30, null);
		String token = jwtTokenGenerator.getMachineToken();
		return token;
	}

	public static String generateReview(JSONObject postJSON, String testPhase, String token) throws Exception {

		Properties props = loadPropertyFile();
		Client client = ClientBuilder.newClient();

		Response response;
		if (testPhase == "system") {
			WebTarget webTarget = client.target(props.getProperty("tcDomain") + "/reviewSummations");

			response = webTarget.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + token).post(Entity.json(postJSON));
		} else {
			WebTarget webTarget = client.target(props.getProperty("tcDomain") + "/reviews");

			response = webTarget.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + token).post(Entity.json(postJSON));

		}

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		return response.readEntity(String.class);

	}

	public static JSONArray getReviews(String challengeId, String token) throws Exception {

		Properties props = loadPropertyFile();
		JSONArray reviews = new JSONArray();
		JSONObject memberReviews = new JSONObject();

		try {

			Client client = ClientBuilder.newClient();
			WebTarget webTarget = client.target(
					props.getProperty("tcDomain") + "/submissions/?challengeId=" + challengeId + "&perPage=100");

			Response response = webTarget.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + token).get();

			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
			}

			String submissions = response.readEntity(String.class);
			JSONParser parser = new JSONParser();
			Object jsonObj = parser.parse(submissions);
			JSONArray jsonArray = (JSONArray) jsonObj;

			for (Object submission : jsonArray) {
				JSONObject jsonSub = (JSONObject) submission;
				JSONArray reviewArray = (JSONArray) jsonSub.get("review");
				Long memberId = (Long) jsonSub.get("memberId");

				if (!memberReviews.containsKey(memberId)) {
					memberReviews.put(memberId, new JSONArray());
				}

				JSONArray tempJSONArray = new JSONArray();

				for (Object review : reviewArray) {
					JSONObject jsonReview = (JSONObject) review;
					if (!jsonReview.get("typeId").equals(props.getProperty("avScanTypeId"))) {
						tempJSONArray = (JSONArray) memberReviews.get(memberId);
						tempJSONArray.add(review);
					}

				}

				if (tempJSONArray.size() > 0) {
					memberReviews.put(memberId, tempJSONArray);
				}
			}

			for (Object memberId : memberReviews.keySet()) {
				JSONArray tempReviews = (JSONArray) memberReviews.get(memberId);
				reviews.add(tempReviews.get(0));
			}

		} catch (Exception e) {
			System.out.println(e);
		}
		return reviews;
	}

}
