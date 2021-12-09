package flakyTestSearch;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class searchGitHub {
	/**
	 * 
	 * @param keywordToSearch
	 */
	public static JSONObject APIcall(String keywordToSearch, String apiURL) {
		JSONObject jsonObject;
		
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com" + apiURL)
		.basicAuth("mlai962", "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")
		.header("accept", "application/vnd.github.v3+json")
		.queryString("q", keywordToSearch+" language:java")
		.queryString("page", 1)
		.queryString("per_page", 100)
		.asJson();
		
		System.out.println(jsonResponse.getBody().toPrettyString());
		
		jsonObject = jsonResponse.getBody().getObject();
		
		return jsonObject;
	}
	
	/**
	 * 
	 */
	public static void findFlakyness() {
		JSONObject jsonObject = APIcall("flaky", "/search/issues");
//		JSONObject jsonObject = APIcall("flaky", "/search/pr");
		
		
		JSONArray jsonarray = jsonObject.getJSONArray("items");
		for (int i = 0; i < jsonarray.length(); i++) {
			JSONObject obj = jsonarray.getJSONObject(i);
			System.out.println(obj.get("repository_url"));
		}
	}
}