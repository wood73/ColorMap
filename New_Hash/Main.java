package wood;

import java.awt.Color;
import java.io.File;

public class Main {
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		//File f = new File("E:\\seam\\TestColorMap3\\op32_backup\\gpuHash.bin");
		//f.delete();
		//long indexesInHash = (long)(52L * 52 * 52 * 52 * 1.3);
		//long bytesInHash = 15 * indexesInHash;
		//System.out.println(indexesInHash);System.exit(0);
		//rgbSize specifies how many possible values of r, g, and b there can be when the RA, GA, BA values are combined
		//rgbSize & opacitySize values will be evenly distributed between 0-255 inclusive
		//setting rgbSize = 1 will result in all r,g,b values in the ra_ga_ba combinations equating 255
		int rgbSize = 1, opacitySize = 12;
		
		//if true, then there output RGBA as a result of mixing ra,ga,ba values will have it's brightness
		//randomly scaled - a 70% chance for it to be randomly upscaled, 30% chance that brightness will be randomly downscaled
		boolean randomlyScale_ra_ga_ba_combo_outputs = true;
		
		//number of possible values between each r,g,b (a) that can be combined, which will each map to the closest RGBA_ra_ga_ba values
		int possibleValues = 12;
		
		//whether alpha values should be iterated over possibleValues # of times | alpha distance currently weighted to 1.3 inside ColorMap.java
		boolean useAlpha = false;
		
		//file of hash table output, linking possibleColors to nearest value in rgba_ra,ga,ba file | overwrites file if it exists
		String hashTablePath = "hash_table.txt";
		
		//file linking combinations of ra,ga,ba colors to the resulting RGBA color | overwrites file if it exists
		//can save file to later plug into the ColorMap class
		String ra_ga_ba_rgba_FilePath = "temp.txt";
		
		//set this to an empty string "" to skip creating this file
		//a .bin file path to the file used by CUDA
		String cudaBinaryFilePath = "";
		
		//creates the file linking combinations of ra,ga,ba colors to the resulting RGBA color
		new DistanceImg(rgbSize, opacitySize, ra_ga_ba_rgba_FilePath, randomlyScale_ra_ga_ba_combo_outputs, cudaBinaryFilePath);
		
		if(false) {
			//will create empty binary hash table for usage with GPU computation
			String gpuHash = "gpuHash.bin";
			boolean gpuAlpha = false;
			Util.createGPUHash(gpuHash, gpuAlpha, 21810380);
		}
		
		//boolean useRandomBrightnessScalingInPlotFile = true;
		//new GeneratePlotFile(rgbSize, opacitySize, "pythonFile.txt", "humanFile.txt", useRandomBrightnessScalingInPlotFile);
		
		//creates the file linking possibleColors to the nearest RGBA color, along with ra,ga,ba colors
		new ColorMap(ra_ga_ba_rgba_FilePath, hashTablePath, possibleValues, useAlpha);
		
	}
}
