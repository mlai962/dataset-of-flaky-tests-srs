package main.java.flakyTestSearch;

public class Config {
	public static long TIMEOUT_CLONING = 300; // Time in seconds before repo cloning times out
	public static long TIMEOUT_SEARCHING = 120000; // Time in milliseconds before repo searching times out
	public static int STARTING_SEARCH_PAGE = 3; // The first page of github api results to retrieve (1-10)
	public static String TEMP_DIR = "/home/student/Downloads/tempRepo"; // The directory used for cloning repos
	public static String MVN_DIR = "/home/student/Downloads/apache-maven-3.8.4/bin/mvn"; // The full directory location of maven, used since my VM did not recognise maven commands
	public static int MAX_TESTS = 4; // Maximum number of tests to be used for test order dependency testing
	public static int SINGLE_TEST_RUNS = 10; // The number of times to run each test
	public static int MULTI_TEST_RUNS = 5; // The number of random test orders to run 
}
