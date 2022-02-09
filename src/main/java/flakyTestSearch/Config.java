package main.java.flakyTestSearch;

public class Config {
	public static long TIMEOUT_CLONING = 600; // Time in seconds before repo cloning times out
	public static long TIMEOUT_SEARCHING = 600000; // Time in milliseconds before repo searching times out
	public static int STARTING_SEARCH_PAGE = 8; // The first page of github api results to retrieve (1-10)
	public static String TEMP_DIR = "C:\\Users\\Matthew\\Downloads\\Documents\\tempRepo";
}
