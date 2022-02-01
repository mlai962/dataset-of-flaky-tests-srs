package flakyTestSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class SearchGitHub {
	public static HttpResponse<JsonNode> apiCall (String apiURL) {
		HttpResponse<JsonNode> responseBody
				=Unirest.get(apiURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.asJson();
		
		return responseBody;
	}
	
	public static HttpResponse<JsonNode> apiCall (String apiURL, Map<String, Object> queryMap) {
		HttpResponse<JsonNode> responseBody
				=Unirest.get(apiURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.queryString(queryMap)
				.asJson();
		
		return responseBody;
	}
	
	public static JSONArray getBranchNames (JSONObject currentProject) {
		return apiCall(currentProject.getString("repository_url")+"/branches").getBody().getArray();
	}
	
	public static String getIssueCreationDate (JSONObject currentProject) {
		return currentProject.getString("created_at");
	}
	
	public static String getIssueTitle (JSONObject currentProject) {
		return currentProject.getString("title");
	}
	
	public static String getIssueBody (JSONObject currentProject) {
		return currentProject.getString("body");
	}
	
	public static String searchIssueBranchName(JSONObject currentProject, JSONArray branchNames) {
		Boolean hasMaster = false;
		Boolean hasMain = false;
		
		for (int i = 0; i < branchNames.length(); i++) {
			Pattern pattern = Pattern.compile(branchNames.getJSONObject(i).getString("name"), Pattern.LITERAL);
			Matcher matcher = pattern.matcher(currentProject.getString("title"));
			
			if (branchNames.getJSONObject(i).getString("name").equals("master")) {
				hasMaster = true;
			} else if (branchNames.getJSONObject(i).getString("name").equals("main")) {
				hasMain = true;
			}
			
			if (matcher.find()) {
				return matcher.group(0);
			}
			
			if (!currentProject.isNull("name")) {
				matcher = pattern.matcher(currentProject.getString("body"));
				
				if (matcher.find()) {
					return matcher.group(0);
				}
			}
		}
		
		if (hasMaster) {
			return "master";
		} else if (hasMain) {
			return "main";
		} else {
			return null;
		}
		
	}
	
	public static String getIssueCommitHash (JSONObject currentProject, JSONArray branchNames) {
		HashMap<String, Object> queryMap = new HashMap<>();
		queryMap.put("sha", searchIssueBranchName(currentProject, branchNames));
		queryMap.put("until", getIssueCreationDate(currentProject));
		
		JSONArray issueCommits
				= apiCall(currentProject.getString("repository_url")+"/commits", queryMap).getBody().getArray();
		
		if (!issueCommits.isEmpty()) {
			String issueHash
					= issueCommits
					.getJSONObject(0)
					.getString("sha");
			
			return issueHash;
		}
		
		return null;
	}
	
	public static String getPullRequestURL (JSONObject currentProject) {
		return currentProject.getJSONObject("pull_request").getString("url");
	}
	
	public static String getPullRequestDiff (JSONObject currentProject) {
		String pullRequestDiff
				= Unirest.get(getPullRequestURL(currentProject)) // Using the pull request URL to find its diff
				.basicAuth("mlai962", "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")
				.header("Accept", "application/vnd.github.v3.diff") // Getting the diff
				.asString()
				.getBody();
		
		return pullRequestDiff;
	}
	
	/**
	 * Takes the current project and finds the pull request URL
	 * and then finds the diff related to that pull request
	 * and searches it for any name of a test which may or may
	 * not be the name of the flaky test.
	 * 
	 * @param currentProject: a project containing a pull request fixing flakyness
	 * @return pullRequestTestID: the name a test or null if none are found
	 */
	public static String searchPattern(String stringToSearch, String stringPattern) {
		// Searching the input string for a string that starts with a "/", contains "test"
		// and ends with "java"
		Pattern pattern = Pattern.compile(stringPattern, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(stringToSearch);
		
		// If the above searching finds a match then return the test ID, otherwise
		// return nothing
		if (matcher.find()) {
			String testClass = matcher.group(0);
			
			return testClass;
		}
		
		return null;
	}
	
	public static String getPullRequestCommitHash(JSONObject currentProject) {
		// Using the pull request URL to find its commit hash
		String pullRequestHash
				= apiCall(getPullRequestURL(currentProject)).getBody().getObject()
				.getJSONObject("head")
				.getString("sha"); // Getting the commit hash

		return pullRequestHash;
	}
	
	/**
	 * Takes a project JSON object as input, finds the pull
	 * request URL in order to find the commit hash of that pull 
	 * request and then the hash of the parent commit to that
	 * pull request (the commit with the flakyness).
	 * 
	 * @param currentProject: a project containing a pull request fixing flakyness
	 * @return pullRequestParentHash: the hash of the commit containing flakyness
	 */
	public static String getPullRequestParentCommitHash(JSONObject currentProject, String pullRequestHash) {
		// Set the latest commit in the API response to be the PR's commit
		HashMap<String, Object> queryMap = new HashMap<>();
		queryMap.put("sha", pullRequestHash);
		
		// Search the current repository's commits
		JSONArray pullRequestCommit
				= apiCall(currentProject.getString("repository_url")+"/commits", queryMap).getBody().getArray();
		
		String pullRequestParentHash
				= pullRequestCommit // The commit API call response is a JSON array
				.getJSONObject(0) // The first JSON object in the array is the commit we entered in the query
				.getJSONArray("parents") // We are looking for the parent hash
				.getJSONObject(0) // There is only one JSON object in the parents array
				.getString("sha"); // This gets the hash of the parent of the commit we queried 
		
		return pullRequestParentHash;
	}
	
	public static String getDiffChangedLine(String pullRequestDiff, String testClass) {
		String lineNum = null;
		
		String[] splitDiff = pullRequestDiff.split("\\-\\-\\-");
		
		for(String diff : splitDiff) {
			if (diff.contains(testClass)) {
				Pattern pattern = Pattern.compile("@@\\s-[0-9]+,[0-9]+\\s\\+[0-9]+,[0-9]+\\s@@\\s[(public)(private)(void)]");
				Matcher matcher = pattern.matcher(diff);
				
				if (matcher.find()) {
					String line = matcher.group(0).replace("@@", "").replace("-", "").replace("+", "").replace(" ", "");
					lineNum = line.split(",")[0];
					return lineNum;
				}
			}
		}
		
		return lineNum;
	}
	
	/**
	 * Searches GitHub for the input keyword and returns the 
	 * response in JSON format
	 * 
	 * @param keywordToSearch: the keyword we are searching GitHub for
	 * @param pageNum: the page number of results to return
	 * @return jsonResponse: the API response in JSON format
	 */
	public static HttpResponse<JsonNode> searchKeyword(String keyword, int pageNum) {
		HashMap<String, Object> queryMap = new HashMap<>();
		queryMap.put("q", keyword + " language:java");
		queryMap.put("page", pageNum);
		queryMap.put("per_page", 100);

		HttpResponse<JsonNode> jsonResponse = apiCall("https://api.github.com/search/issues", queryMap);
		
		return jsonResponse;
	}
	
	/**
	 * Calls all the relevant methods to get a list of projects
	 * from GitHub that have issues or pull requests related to
	 * test flakyness.
	 * 
	 * @return projects: a list of projects with their URLs, hashes and test IDs
	 */
	public static List<Project> getProjectList(String keyword) {
		HttpResponse<JsonNode> jsonResponse;
		JSONObject jsonObject;
		JSONArray jsonArray;
		List<Project> projects = new ArrayList<>();
		int pageNum = 2;
		
		do {
			jsonResponse = searchKeyword(keyword, pageNum);
			
			jsonObject = jsonResponse.getBody().getObject();
			
			System.out.println(jsonResponse.getHeaders().toString());
			System.out.println(jsonResponse.getBody().toPrettyString());

			if (jsonObject.length() == 3) {
				jsonArray = jsonObject.getJSONArray("items");

				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject currentProject = jsonArray.getJSONObject(i);

					String projectURL = currentProject.getString("repository_url");

					if (currentProject.has("pull_request")) {
//						String pullRequestHash = getPullRequestCommitHash(currentProject);
//						
//						String commitHash = getPullRequestParentCommitHash(currentProject, pullRequestHash);
//						
//						String pullRequestDiff = getPullRequestDiff(currentProject);
//						
//						System.out.println(pullRequestDiff);
//						System.out.println(pullRequestHash);
//
//						String testClass = searchPattern(pullRequestDiff, "\\/[^\\s]*test[^\\s]*java");
//
//						Project project = new Project(projectURL, commitHash, null, null);
//						
//						if (!(testClass == null)) {
//							project.setClassName(testClass);
//							
//							String changedLine = getDiffChangedLine(pullRequestDiff, testClass);
//							System.out.println(changedLine);
//
//							if (!(changedLine == null)) {
//								boolean isCloned = RepoUtil.cloneRepo(project);
//								
//								System.out.println(isCloned + " cloned");
//
//								if (isCloned) {
//									Boolean isClass = RepoUtil.checkClassExists(project);
//									
//									System.out.println(isClass + " class");
//
//									if (isClass) {
//										ArrayList<String> testNames = RepoUtil.findTestName(project, changedLine);
//										
//										if (!testNames.isEmpty()) {
//											String testID = testNames.get(testNames.size()-1);
//											System.out.println(testID);
//											project.setTestID(testID);
//										} else {
//											project.setSkipReason("no test name found in class");
//										}
//									} else {
//										project.setSkipReason("no test class found after cloning");
//									}
//								} else {
//									project.setSkipReason("unsuccessful clone");
//								}
//							} else {
//								project.setSkipReason("no changed lines in test method");
//							}
//						} else {
//							project.setSkipReason("no test class found");
//						}
//						
//						projects.add(project);
					} else {
						JSONArray branchNames = getBranchNames(currentProject);

						String commitHash = getIssueCommitHash(currentProject, branchNames);
						
						Project project = new Project(projectURL, commitHash, null, null);
						
						String issueTitle = getIssueTitle(currentProject);
						
						String issueBody = getIssueBody(currentProject);
						
						String classDotTest = searchPattern(issueTitle, "[a-zA-Z]*(test)[a-zA-z]*\\.[a-zA-Z]*(test)[a-zA-z]*");
						
						if (classDotTest == null) {
							classDotTest = searchPattern(issueBody, "[a-zA-Z]*(test)[a-zA-z]*\\.[a-zA-Z]*(test)[a-zA-z]*");
						} else {
							System.out.println(classDotTest);
						}
						
						String testClass = null;
						String testID = null;
						
						if (!(classDotTest == null)) {
							System.out.println(classDotTest);
							
							testClass = classDotTest.split("\\.")[0];
							testID = classDotTest.split("\\.")[1];
							
							project.setClassName(testClass);
							project.setTestID(testID);
							
							boolean isCloned = RepoUtil.cloneRepo(project);
							
							if (isCloned) {
								System.out.println("true clone");
								
								boolean isClass = RepoUtil.checkIssueClassExists(project);
								
								if (isClass) {
									System.out.println("true class");
								} else {
									System.out.println("false class");
									project.setSkipReason("no test class found after cloning");
								}
							} else {
								System.out.println("false clone");
								project.setSkipReason("unsuccessful clone");
							}
						} else {
							project.setSkipReason("no test class found");
						}
						
						System.out.println("----------------------------TITLE----------------------------");
						System.out.println(issueTitle);
						System.out.println("----------------------------BODY----------------------------");
						System.out.println(issueBody);
						System.out.println("----------------------------END----------------------------");

						projects.add(project);
					}
				}
			}
			
			pageNum++;
		} while (jsonResponse.getStatus() == 200);
		
		return projects;
	}
}