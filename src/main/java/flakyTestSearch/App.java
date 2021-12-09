package flakyTestSearch;

import java.net.http.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	kong.unirest.HttpResponse<JsonNode> jsonResponse
		= Unirest.get("https://api.github.com/users/Neville-Loh")
		.asJson();

		System.out.println(jsonResponse.getBody().toPrettyString());
    }
}
