package wood;

public class Main {
	
	public static void main(String[] args) {
		
		//rgbSize specifies how many possible values of r, g, and b there can be when the RA, GA, BA values are combined
		//rgbSize & opacitySize values will be evenly distributed between 0-255 inclusive
		int rgbSize = 3, opacitySize = 4;
		
		//number of possible values between each r,g,b (a) that can be combined, which will each map to the closest RGBA_ra_ga_ba values
		int possibleValues = 10;
		
		//whether alpha values should be iterated over possibleValues # of times | alpha distance calculation currently not supported
		boolean useAlpha = false;
		
		//file of hash table output, linking possibleColors to nearest 
		String hashTablePath = "hash_table.txt";
		
		//file linking combinations of ra,ga,ba colors to the resulting RGBA color
		String ra_ga_ba_rgba_FilePath = "temp.txt";
		
		//creates the file linking combinations of ra,ga,ba colors to the resulting RGBA color
		new DistanceImg(rgbSize, opacitySize, ra_ga_ba_rgba_FilePath);
		
		//creates the file linking possibleColors to the nearest RGBA color, along with ra,ga,ba colors
		new ColorMap(ra_ga_ba_rgba_FilePath, hashTablePath, possibleValues, useAlpha);
		
	}
}
