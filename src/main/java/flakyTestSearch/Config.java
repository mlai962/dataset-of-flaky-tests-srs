package main.java.flakyTestSearch;

public class Config {
	public static long TIMEOUT_CLONING = 300; // Time in seconds before repo cloning times out
	public static long TIMEOUT_SEARCHING = 300000; // Time in milliseconds before repo searching times out
	public static int STARTING_SEARCH_PAGE = 2; // The first page of github api results to retrieve (1-10)
	public static String TEMP_DIR = "/home/student/Downloads/tempRepo"; // The directory used for cloning repos
}
