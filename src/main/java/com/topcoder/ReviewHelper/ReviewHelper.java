package com.topcoder.ReviewHelper;

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

	public static String getToken(String clientId, String clientSecret, String audience, String m2mAuthDomain)
			throws Exception {
		JWTTokenGenerator jwtTokenGenerator = JWTTokenGenerator.getInstance(clientId, clientSecret, audience,
				m2mAuthDomain, 30, null);
		String token = jwtTokenGenerator.getMachineToken();
		return token;
	}

	public static String generateReview(JSONObject postJSON, String testPhase, String token) throws Exception {

		Client client = ClientBuilder.newClient();

		Response response;
		if (testPhase == "system") {
			WebTarget webTarget = client.target("http://api.topcoder.com/v5/reviewSummations");

			response = webTarget.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + token).post(Entity.json(postJSON));
		} else {
			WebTarget webTarget = client.target("http://api.topcoder.com/v5/reviews");

			response = webTarget.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
					.header("Authorization", "Bearer " + token).post(Entity.json(postJSON));

		}

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
		}

		return response.readEntity(String.class);

	}

	public static JSONArray getReviews(String challengeId, String token) {

		JSONArray reviews = new JSONArray();
		JSONObject memberReviews = new JSONObject();

		try {

			Client client = ClientBuilder.newClient();
			WebTarget webTarget = client
					.target("http://api.topcoder.com/v5/submissions/?challengeId=" + challengeId + "&perPage=100");

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
					if (!jsonReview.get("typeId").equals("55bbb17d-aac2-45a6-89c3-a8d102863d05")) {
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
