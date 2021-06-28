package wood;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

public class ColorMap {
	
	
	long netClosestColorCalc = 0, hashTime = 0;
	
	boolean loadDenseIntoRAM = true;
	long startTime = System.currentTimeMillis();
	
	//public File newHashTable = new File("/home/ec2-user/caramel.txt");
	//public File denseFile = new File("/home/ec2-user/crispy_apple.txt");
	public File newHashTable = null;
	public File denseFile = null;
	
	
	public int outputBytesPerIndex = 15;
	public int outputHashMetadataBytes = 11;
	
	public ColorMap(String denseFilePath, String newHashTablePath, int possibleValues, boolean useAlpha, boolean colorScaling) {
		
		newHashTable = new File(newHashTablePath);
		denseFile = new File(denseFilePath);
		if(!denseFile.exists()) {
			System.out.println("Error, file " + denseFilePath + " is not found.");
			System.exit(0);
		}
		
		short[][] dense = null;
		
		long entryByteSize = Long.parseLong(read(denseFile, 0, 11));
		long entryIndexSize = entryByteSize / 10;
		
		if(loadDenseIntoRAM) {
			System.out.println("Loading dense file into RAM..");
			dense = new short[(int)entryIndexSize][10];
			
			for(int i = 0; i < entryIndexSize; i++) {
				long bytePointer = 11 + i * 10;
				
				String rgbaDense = read(denseFile, bytePointer, 10);
				
				dense[i][0] = (short)Util.charToASCII(rgbaDense.charAt(0)); 
				dense[i][1] = (short)Util.charToASCII(rgbaDense.charAt(1)); 
				dense[i][2] = (short)Util.charToASCII(rgbaDense.charAt(2)); 
				dense[i][3] = (short)Util.charToASCII(rgbaDense.charAt(3));
				dense[i][4] = (short)Util.charToASCII(rgbaDense.charAt(4)); 
				dense[i][5] = (short)Util.charToASCII(rgbaDense.charAt(5)); 
				dense[i][6] = (short)Util.charToASCII(rgbaDense.charAt(6)); 
				dense[i][7] = (short)Util.charToASCII(rgbaDense.charAt(7));
				dense[i][8] = (short)Util.charToASCII(rgbaDense.charAt(8)); 
				dense[i][9] = (short)Util.charToASCII(rgbaDense.charAt(9));
				
			}
			System.out.println("Dense file loaded into RAM");
		}
		
		long outputHashTableFilledIndexSize = (long)Math.pow(possibleValues, (useAlpha ? 4 : 3));
		long outputFileIndexSize = (long)(outputHashTableFilledIndexSize * 1.3);
		long outputFileByteSize = outputFileIndexSize * outputBytesPerIndex + outputHashMetadataBytes;
		long outputHashTableByteSize = outputFileIndexSize * outputBytesPerIndex;
		
		long totalNumberIterations = entryIndexSize * (long)Math.pow(possibleValues, (useAlpha ? 4 : 3));;
		
		Util.zeroPadFile(newHashTable, outputFileByteSize);
		
		//1. iterate through every combination of 32 values for rgba - make 256 round down to 255
		long iterator = 0, duplicateBrightnessNormalizedCounter = 0;
		double multiplier = (double)255 / (possibleValues-1);
		
		LinkedHashSet<Long> hashSet = new LinkedHashSet<Long>();
		ArrayList<Long> nonDuplicateList = new ArrayList<Long>();
		for(int r = 0; r < possibleValues; r++) {
			int rVal = (int)Math.round(r*multiplier);
			
			for(int g = 0; g < possibleValues; g++) {
				int gVal = (int)Math.round(g*multiplier);
				
				for(int b = 0; b < possibleValues; b++) {
					int bVal = (int)Math.round(b*multiplier);
					
					for(int a = 0; a < (useAlpha ? possibleValues : 1); a++) {
						int aVal = useAlpha ? (int)Math.round(a*multiplier) : 255;
						
						int r2Val = -1, g2Val = -1, b2Val = -1;
						if(colorScaling) {
						   int maxColor = Math.max(rVal, Math.max(gVal, bVal));
						   	double scaleMultiplier = 255/(double)maxColor;
						   	r2Val = (int)Math.min(Math.round(rVal*scaleMultiplier), 255);
						   	g2Val = (int)Math.min(Math.round(gVal*scaleMultiplier), 255);
						   	b2Val = (int)Math.min(Math.round(bVal*scaleMultiplier), 255);
						}
						
						//2. for each value, iterate through 50mb hash table to find closest color
						long comboRGBA = -1;
						try {
							if(colorScaling)
								comboRGBA = (long)r2Val * 16777216 + g2Val * 65536 + b2Val * 256 + aVal;
							else
								comboRGBA = (long)rVal * 16777216 + gVal * 65536 + bVal * 256 + aVal;
							
						} catch(Exception e) { e.printStackTrace(); }
						
						int[] closestRGBA = new int[] {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
						double closestDelta = Double.MAX_VALUE;
						
						long colorCalcLoopStart = System.currentTimeMillis();
						for(int i = 0; i < entryIndexSize; i++, iterator++) {
							long bytePointer = 11 + i * 10;
							int[] rgba = null;
							double delta = -1;
							
							if(loadDenseIntoRAM) {
								if(colorScaling)
									delta = colourDistance(new int[] { r2Val, g2Val, b2Val }, new int[] { dense[i][0], dense[i][1], dense[i][2] });
								else
									delta = colourDistance(new int[] { rVal, gVal, bVal }, new int[] { dense[i][0], dense[i][1], dense[i][2] });
								//delta = Math.abs(dense[i][0] - rVal) + Math.abs(dense[i][1] - gVal) + Math.abs(dense[i][2] - bVal) +
										//.73 * Math.abs(dense[i][3] - aVal);
							}
							else {
								rgba = new int[] { Util.charToASCII(read(denseFile, bytePointer, 1).charAt(0)), 
										Util.charToASCII(read(denseFile, bytePointer + 1, 1).charAt(0)), 
										Util.charToASCII(read(denseFile, bytePointer + 2, 1).charAt(0)), 
										Util.charToASCII(read(denseFile, bytePointer + 3, 1).charAt(0)),
										Util.charToASCII(read(denseFile, bytePointer + 4, 1).charAt(0)),
										Util.charToASCII(read(denseFile, bytePointer + 5, 1).charAt(0)),
										Util.charToASCII(read(denseFile, bytePointer + 6, 1).charAt(0)),
										Util.charToASCII(read(denseFile, bytePointer + 7, 1).charAt(0)),
										Util.charToASCII(read(denseFile, bytePointer + 8, 1).charAt(0)),
										Util.charToASCII(read(denseFile, bytePointer + 9, 1).charAt(0))};
								
								if(colorScaling)
									delta = colourDistance(new int[] { r2Val, g2Val, b2Val }, new int[] { rgba[0], rgba[1], rgba[2] });
								else
									delta = colourDistance(new int[] { rVal, gVal, bVal }, new int[] { rgba[0], rgba[1], rgba[2] });
								//delta = Math.abs(rgba[0] - rVal) + Math.abs(rgba[1] - gVal) + Math.abs(rgba[2] - bVal) + 
										//.73 * Math.abs(rgba[3] - aVal);
							}
							
							if(delta < closestDelta) {
								closestDelta = delta;
								closestRGBA = loadDenseIntoRAM ? new int[] { dense[i][0], dense[i][1], dense[i][2], dense[i][3], dense[i][4],
										dense[i][5], dense[i][6], dense[i][7], dense[i][8], dense[i][9] } : rgba; //shallow copy
							}
							System.out.println(iterator + " / " + totalNumberIterations + " iterations complete");
						}
						netClosestColorCalc += (System.currentTimeMillis() - colorCalcLoopStart);
						
						//3. insert both rgba values into new hash table, size calculated by number of combinations from x values
						long hashStart = System.currentTimeMillis();
						long hash = Util.hash(comboRGBA, outputFileIndexSize, false, this);
						hashTime += (System.currentTimeMillis() - hashStart);
						
						//if it was a duplicate entry, try with another combination
						if(hash == -1) {
							duplicateBrightnessNormalizedCounter++;
							continue;
						}
						
						if(colorScaling) {
							write(String.format("1%c%c%c%c%c%c%c%c%c%c%c%c%c%c", (char)r2Val, (char)g2Val, (char)b2Val, (char)aVal, (char)closestRGBA[0],
									(char)closestRGBA[1], (char)closestRGBA[2], (char)closestRGBA[3], (char)closestRGBA[4], (char)closestRGBA[5], 
									(char)closestRGBA[6], (char)closestRGBA[7], (char)closestRGBA[8], (char)closestRGBA[9]), 
									hash*outputBytesPerIndex + outputHashMetadataBytes);
						}
						else {
							write(String.format("1%c%c%c%c%c%c%c%c%c%c%c%c%c%c", (char)rVal, (char)gVal, (char)bVal, (char)aVal, (char)closestRGBA[0],
									(char)closestRGBA[1], (char)closestRGBA[2], (char)closestRGBA[3], (char)closestRGBA[4], (char)closestRGBA[5], 
									(char)closestRGBA[6], (char)closestRGBA[7], (char)closestRGBA[8], (char)closestRGBA[9]), 
									hash*outputBytesPerIndex + outputHashMetadataBytes);
						}
					
					}
				}
			}
		}
		
		long programRunTime = System.currentTimeMillis() - startTime;
		System.out.println("Number duplicate brightness-ignoring colors: " + duplicateBrightnessNormalizedCounter);
		write(String.valueOf((char)possibleValues) + String.format("%10s", String.valueOf(outputHashTableByteSize)).replace(' ', '0'), 0);
		System.out.println("Program run time: " + (programRunTime/1000.) + " seconds.");
		System.out.println("Percentage of time spent in hash function: " + (hashTime / (double)programRunTime * 100));
		System.out.println("Percentage of time spent finding the nearest color: " + (netClosestColorCalc / (double)programRunTime * 100));
	}
	
	public String read(File f, long byteIndex, int length) {
		String data = "";
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(f);
			fin.skip(byteIndex);
			byte[] byteData = new byte[length];
			fin.read(byteData);
			
			for(byte b : byteData) {
				data += (char)Util.decodeByte(b);
			}
			
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			try { fin.close(); } catch (IOException e) { e.printStackTrace(); }
		}
		
		return data;
	}
	
	public void write(String data, long byteIndex) {
		if(!newHashTable.exists()) {
			try { newHashTable.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
		}
		//syntax for resource try block - automatically closed resources
		try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(Paths.get(newHashTable.getAbsolutePath()), StandardOpenOption.WRITE) ){
			ByteBuffer bb = ByteBuffer.allocate(data.length());
			for(char c : data.toCharArray())
				bb.put((byte)c);
			
			Future<Integer> future = afc.write(bb.flip(), byteIndex);
			
			//get() makes sure the file write completes before moving on
			//if not called (might be risky), on a small scale, tested to save time by 0.62124 %
			future.get();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public double colourDistance(int[] c1, int[] c2) {
		int rmean = (c1[0] + c2[0]) / 2;
		int r = c1[0] - c2[0];
		int g = c1[1] - c2[1];
		int b = c1[2] - c2[2];
		
		return Math.sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
	}
	
	/*
	 
	 
	 Color distance calculation in C code:
	 https://stackoverflow.com/a/9085524
	 
	 typedef struct {
     unsigned char r, g, b;
} RGB;

double ColourDistance(RGB e1, RGB e2)
{
    long rmean = ( (long)e1.r + (long)e2.r ) / 2;
    long r = (long)e1.r - (long)e2.r;
    long g = (long)e1.g - (long)e2.g;
    long b = (long)e1.b - (long)e2.b;
    return sqrt((((512+rmean)*r*r)>>8) + 4*g*g + (((767-rmean)*b*b)>>8));
}
	 
	 
	 */
	
}
