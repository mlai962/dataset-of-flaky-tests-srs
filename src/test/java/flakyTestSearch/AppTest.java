package flakyTestSearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	/**
	 * Rigorous Test :-)
	 */
	@Test
	public void shouldAnswerWithTrue()
	{
		assertTrue( true );
	}

	@Test
	public void getAuthenticatedUserDetails() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse 
		= Unirest.get("https://api.github.com/user")
		.basicAuth("mlai962", "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")
		.asJson();

		System.out.println(jsonResponse.getHeaders().toString());
		System.out.println(jsonResponse.getBody().toPrettyString());
		
		String etag = jsonResponse.getHeaders().getFirst("Etag");
		System.out.println(etag);

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
		
		kong.unirest.HttpResponse<JsonNode> jsonResponse2 
		= Unirest.get("https://api.github.com/user")
		.basicAuth("mlai962", "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")
		.header("If-None-Match", etag.substring(3, etag.length()-2))
		.asJson();
		
		assertEquals(304, jsonResponse2.getStatus());
	}

	@Test
	public void getRepoDetails() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/repos/twbs/bootstrap")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getAuthenticatedUserRepos() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/user/repos")
		.basicAuth("mlai962", "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")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getUserRepos() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/users/octocat/repos")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getOrganisationRepos() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/orgs/octo-org/repos")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getUserOwnedRepos() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/user/repos")
		.basicAuth("mlai962", "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")
		.queryString("type", "owner")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getAuthenticatedUserIssues() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/issues")
		.basicAuth("mlai962", "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")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getOrganisationIssues() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/orgs/SOFTENG206-2021/issues")
		.basicAuth("mlai962", "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")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}

	@Test
	public void getRepoIssues() {
		kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/repos/rails/rails/issues")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());

		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}
	
	@Test
	public void getIssues() {
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
//			System.out.println(obj.get("body"));
		}
		
		assertNotNull(jsonResponse.getBody());
		assertEquals(200, jsonResponse.getStatus());
	}
}
