package wood;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class DistanceImg {
	
	private long programStartTime = System.currentTimeMillis();
	
	public ArrayList<String> asciiFormatBuffer = new ArrayList<String>();
	
	public DistanceImg(int rgbSize, int opacitySize, String outputFilePath, boolean multiply_ra_ga_ba_combo_outputs, String binaryFilePath) {
		
		//range of 0 to these values, where even distributions of rgb & opacity values will be generated.  
		//shouldn't adjust these
		int maxRGB = 255, maxOpacity = 255;
		
		//file of temporary hex table to detect & remove duplicate RGBA values before filtering into outputFilePath
		String tempFilePath = "temp429879.txt";

		long tempHashTableIndexSize = (long)(Math.pow(rgbSize, 3)*Math.pow(opacitySize, 3) * 1.3);
		
		//holds rgbSize number of possible values that r, g, and b can be.  these values are evenly distributed between 0-255 inclusive
		
		int[] evenlyDistributedRGBValues = new int[rgbSize];
		if(rgbSize != 1) {
			double increment = maxRGB/(double)(rgbSize-1);
			for(int i = 0; i < rgbSize; i++) {
				evenlyDistributedRGBValues[i] = (int)Math.round(i*increment);
			}
		}
		else
			evenlyDistributedRGBValues[0] = 255;
		
		int[] opacity_values = new int[opacitySize];
		
		double increment = maxOpacity/(double)(opacitySize-1);
		for(int i = 0; i < opacitySize; i++) {
			opacity_values[i] = (int)Math.round(i*increment);
		}
		
		long iterator = 0;
		long num_RA_GA_BA_combinations = (long)(Math.pow(rgbSize, 3)*Math.pow(opacitySize, 3));
		RGBA_File rgbaFile = new RGBA_File(tempFilePath, tempHashTableIndexSize, outputFilePath, asciiFormatBuffer, binaryFilePath);
		
		for(int r = 0; r < rgbSize; r++) {
			for(int ra = 0; ra < opacitySize; ra++) {
				for(int g = 0; g < rgbSize; g++) {
					for(int ga = 0; ga < opacitySize; ga++) {
						for(int b = 0; b < rgbSize; b++) {
							for(int ba = 0; ba < opacitySize; ba++, iterator++) {
								
								Color rgba = Util.mixColorsWithAlpha(new Color(evenlyDistributedRGBValues[r], 0, 0, opacity_values[ra]), 
																new Color(0, evenlyDistributedRGBValues[g], 0, opacity_values[ga]), 
																multiply_ra_ga_ba_combo_outputs);
								
								rgba = Util.mixColorsWithAlpha(rgba, new Color(0, 0, evenlyDistributedRGBValues[b], opacity_values[ba]), 
										multiply_ra_ga_ba_combo_outputs);
								
								
								System.out.println(iterator + "/" + num_RA_GA_BA_combinations + " iterations complete");
								
								//System.out.println(rgba.getRed() + ", " + rgba.getGreen() + ", " + rgba.getBlue());
								rgbaFile.insertRGBA(rgba.getRed(), rgba.getGreen(), rgba.getBlue(), rgba.getAlpha(), evenlyDistributedRGBValues[r], 
										opacity_values[ra], evenlyDistributedRGBValues[g], opacity_values[ga], evenlyDistributedRGBValues[b], opacity_values[ba]);
								
							}
						}
					}
				}
			}
		}
		rgbaFile.emptyBuffer();
		rgbaFile.f.delete();
		rgbaFile.close();
		
		Util.write(rgbaFile.rgbaFileDense, String.format("%11s", rgbaFile.hashTableEntries * 10).replace(' ', '0'), 0);
		
		
		System.out.println("Program run time: " + ((System.currentTimeMillis() - programStartTime)/1000.) + " seconds.");
		System.out.println("Number entries in hash table: " + rgbaFile.hashTableEntries);
	}
	
	public void graphPoint(int x, int y, int z) {
		
	}
	
}