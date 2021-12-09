package flakyTestSearch;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class searchGitHub {
	/**
	 * 
	 */
	public static void findFlakyness() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/search/issues")
		.basicAuth("mlai962", "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")
		.queryString("q", "flaky")
		.queryString("page", 1)
		.queryString("per_page", 100)
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		JSONObject j = jsonResponse.getBody().getObject();
		JSONArray jsonarray = j.getJSONArray("items");
		for (int i = 0; i < jsonarray.length(); i++) {
			JSONObject obj = jsonarray.getJSONObject(i);
			System.out.println(obj.get("repository_url"));
			System.out.println(obj.get("body"));
		}
	}
}