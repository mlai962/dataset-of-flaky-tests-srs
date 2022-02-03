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
				= Unirest.get(apiURL)
				.basicAuth("mlai962", "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")
				.header("accept", "application/vnd.github.v3+json")
				.asJson();
		
		return responseBody;
	}
	
	public static HttpResponse<JsonNode> apiCall (String apiURL, Map<String, Object> queryMap) {
		HttpResponse<JsonNode> responseBody
				= Unirest.get(apiURL)
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
	public static ArrayList<String> searchPattern(String stringToSearch, String stringPattern) {
		ArrayList<String> results = new ArrayList<>();
		
		// Searching the input string for a string that starts with a "/", contains "test"
		// and ends with "java"
		Pattern pattern = Pattern.compile(stringPattern, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(stringToSearch);
		
		// If the above searching finds a match then return the test ID, otherwise
		// return nothing
		while (matcher.find()) {
			String match = matcher.group(0);
			
			results.add(match);
		}
		
		return results;
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
	
	public static ArrayList<String> getDiffChangedLines(String pullRequestDiff, String testClass) {
		ArrayList<String> lineNums = new ArrayList<>();
		
		String lineNum = null;
		
		String[] splitDiff = pullRequestDiff.split("\\-\\-\\-");
		
		for(String diff : splitDiff) {
			if (diff.contains(testClass)) {
				Pattern pattern = Pattern.compile("@@\\s-[0-9]+,[0-9]+\\s\\+[0-9]+,[0-9]+\\s@@\\s[(public)(private)(void)]");
				Matcher matcher = pattern.matcher(diff);
				
				while (matcher.find()) {
					String line = matcher.group(0).replace("@@", "").replace("-", "").replace("+", "").replace(" ", "");
					lineNum = line.split(",")[0];
					
					lineNums.add(lineNum);
				}
			}
		}
		
		return lineNums;
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
		int pageNum = 7;
		
		do {
			jsonResponse = searchKeyword(keyword, pageNum);
			
			jsonObject = jsonResponse.getBody().getObject();

			if (jsonObject.length() == 3) {
				jsonArray = jsonObject.getJSONArray("items");

				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject currentProject = jsonArray.getJSONObject(i);

					String projectURL = currentProject.getString("repository_url");

					if (currentProject.has("pull_request")) {
						String pullRequestHash = getPullRequestCommitHash(currentProject);
						
						String commitHash = getPullRequestParentCommitHash(currentProject, pullRequestHash);
						
						String pullRequestDiff = getPullRequestDiff(currentProject);

						ArrayList<String> testClasses = searchPattern(pullRequestDiff, "\\/[^\\s]*test[^\\s]*java");

						Project project = new Project(projectURL, commitHash, null, null);
						
						HashMap<String, List<String>> allTestNames = new HashMap<>();

						if (!(testClasses.isEmpty())) {
							project.setClasses(testClasses);

							boolean isCloned = RepoUtil.cloneRepo(project);

							if (isCloned) {
								for (String testClass : testClasses) {
									ArrayList<String> changedLines = getDiffChangedLines(pullRequestDiff, testClass);

									Boolean isClass = RepoUtil.checkClassExists(project, testClass);

									if (isClass) {
										if (!(changedLines.isEmpty())) {
											ArrayList<String> testNames = RepoUtil.findTestName(project, changedLines);

											if (!testNames.isEmpty()) {
												allTestNames.put(testClass, testNames);
												
												project.setTestNames(allTestNames);
												
												if (!(project.getTestNames() == null)) {
													System.out.println(project.getProjectName());
													System.out.println(project.getCommitHash());
													for (String className : project.getTestNames().keySet()) {
														for (String Name : project.getTestNames().get(className)) {
															System.out.println(className + " " + Name);
														}
													}
													
												}
												System.out.println(project.getSkipReason());
											} 
											
											if (allTestNames.isEmpty()) {
												project.setSkipReason("no test names found in any class");
											}
										} else {
											project.setSkipReason("no changed lines in any test methods");
										}
									} else {
										project.setSkipReason("no test classes found after cloning");
									}
								}
							} else {
								project.setSkipReason("unsuccessful clone");
							} 
						} else {
							project.setSkipReason("no test classes found in diff");
						}

						projects.add(project);
						
							

					} else {
						JSONArray branchNames = getBranchNames(currentProject);

						String commitHash = getIssueCommitHash(currentProject, branchNames);

						Project project = new Project(projectURL, commitHash, null, null);

						String issueTitle = getIssueTitle(currentProject);

						String issueBody = getIssueBody(currentProject);

						HashMap<String, List<String>> allTestNames = new HashMap<>();

						ArrayList<String> classDotTests
						= searchPattern(issueTitle, "[a-zA-Z]*(test)[a-zA-z]*\\.[a-zA-Z]*(test)[a-zA-z]*");

						if (classDotTests.isEmpty()) {
							classDotTests = searchPattern(issueBody, "[a-zA-Z]*(test)[a-zA-z]*\\.[a-zA-Z]*(test)[a-zA-z]*");
						}

						if (!classDotTests.isEmpty()) {
							boolean isCloned = RepoUtil.cloneRepo(project);

							if (isCloned) {
								for (String classDotTest : classDotTests) {
									String testClass = null;
									String testName = null;

									testClass = classDotTest.split("\\.")[0];
									testName = classDotTest.split("\\.")[1];
									boolean[] array = RepoUtil.checkIssueClassExistsAndIfTestClass(project, testClass, testName);
									boolean isClass = array[0];
									boolean isTestClass = array[1];
									boolean hasTestName = array[2];

									if (isClass) {
										if (isTestClass) {
											if (hasTestName) {
												List<String> temp = new ArrayList<>();

												if (allTestNames.containsKey(testClass)) {
													temp = allTestNames.get(testClass);
													temp.add(testName);

													allTestNames.put(testClass, temp);

													if (!(project.getTestNames() == null)) {
														System.out.println(project.getProjectName());
														System.out.println(project.getCommitHash());
														for (String className : project.getTestNames().keySet()) {
															for (String Name : project.getTestNames().get(className)) {
																System.out.println(className + " " + Name);
															}
														}
														
													}
													System.out.println(project.getSkipReason());
												} else {
													temp.add(testName);
													allTestNames.put(testClass, temp);
												}
											} else {
												project.setSkipReason("test not found in test class");
											}
										} else {
											project.setSkipReason("not a test class");
										}
									} else {
										project.setSkipReason("no test classes found after cloning");
									}
								}
							} else {
								project.setSkipReason("unsuccessful clone");
							}
						} else {
							project.setSkipReason("no test classes found in issue");
						}

						projects.add(project);
					}
				}
			}
			
			pageNum++;
			
			break;
		} while (jsonResponse.getStatus() == 200);
		
		return projects;
	}
}