package wood;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

public class Util {
	
	/**Creates a String of length size, populated by only the char c,
	 * in a computationally efficient manner
	 * 
	 * To make String of 10,002,548 0's, it takes 46 iterations between 2 loops
	 * Risk running out of memory with Strings over 500 million characters (depends on hardware)
	 * 
	 * @param c character to populate the String with
	 * @param size number of characters in the String
	 * @return string of specified size & character
	 */
	public static String createSingularString(char c, long size) {
		//dynamic list of Strings that only hold char c, where each element holds 2^element number, number of characters
		ArrayList<String> stringBuilder = new ArrayList<String>();
		stringBuilder.add(String.valueOf(c));
		
		//string that will be returned
		String str = "";
		
		//loop until str is of the appropriate size
		while(str.length() != size) {
			String topStringBuilderElement = stringBuilder.get(stringBuilder.size()-1);
			
			//if str + the biggest stringBuilder element is too big, 
			//then keep deleting the top element until one that isn't too big is found
			if(str.length() + topStringBuilderElement.length() > size) {
				stringBuilder.remove(stringBuilder.size()-1);
				boolean foundStringToAdd = false;
				while(!foundStringToAdd) {
					if(str.length() + stringBuilder.get(stringBuilder.size()-1).length() <= size) {
						foundStringToAdd = true;
						str += stringBuilder.get(stringBuilder.size()-1);
					}
					else {
						stringBuilder.remove(stringBuilder.size()-1);
					}
				}
			}
			else {
				str += topStringBuilderElement;
				stringBuilder.add(topStringBuilderElement + topStringBuilderElement);
			}
		}
		
		return str;
	}
	
	//if not readingData, then will be in writingData mode
		public static long hash(long rgbaValue, long hashTableIndexSize, boolean readingData, ColorMap cm) {
			int chain = 0;
			
			boolean foundHash = false;
			boolean foundDuplicateRGBA = false;
			long initialHash = -1, hashed = -1;
			boolean exponentTooLarge = false;
			int maxExponent = -1;
			int linearIncrement = 1;
			
			for(int i = 1; !foundHash && !foundDuplicateRGBA; i++) {
				if(!exponentTooLarge) {
					
					if(initialHash == -1) {
						//add 2 to prevent RGBA values of 0 & 1 from creating an infinite loop
						initialHash = rgbaValue % hashTableIndexSize == 0 || rgbaValue % hashTableIndexSize == 1 ? 
								(rgbaValue + 2) % hashTableIndexSize : rgbaValue % hashTableIndexSize;
						//System.out.println("Init hash = " + initialHash + " | rgba: " + rgbaValue + " | " + "hash table index count: " + hashTableIndexSize);
						
						
					}
					
					long exponentiated = (long)Math.pow(initialHash, i);
					
					if(exponentiated > 100000000000000L) {
						exponentTooLarge = true;
					}
					else {
						maxExponent = i;
						hashed = exponentiated % hashTableIndexSize;
						//System.out.println("hash = " + hashed);
					}
				}
				
				if(exponentTooLarge) {
					hashed = ((long)Math.pow(initialHash, maxExponent) + linearIncrement) % hashTableIndexSize;
					linearIncrement++;
				}
				
				//System.out.println(hashed + " | " + rf.getHashStatus() + " | " + "Init hash = " + initialHash + " | rgba: " + rgbaValue + " | " + "hash table index count: " + hashTableIndexSize);
				
				long byteIndexAtHash = hashed * cm.outputBytesPerIndex + cm.outputHashMetadataBytes;
				boolean isUnusedIndex = cm.read(cm.newHashTable, byteIndexAtHash, 1).equals("0") ? true : false;
				
				if(isUnusedIndex && readingData) {
					JOptionPane.showMessageDialog(null, "Unused index found while looking up value in hash table.  Exiting program.", "", JOptionPane.ERROR_MESSAGE);
					System.exit(0);
				}
				else if(isUnusedIndex) {
					foundHash = true;
				} else {
					String rgba = cm.read(cm.newHashTable, byteIndexAtHash+1, 4);
					int[] rgbaInt = new int[] { Util.charToASCII(rgba.charAt(0)), Util.charToASCII(rgba.charAt(1)), Util.charToASCII(rgba.charAt(2)),
							Util.charToASCII(rgba.charAt(3)) };
					try {
						
						foundDuplicateRGBA = baseConversionToBase10(256, rgbaInt[0], rgbaInt[1], rgbaInt[2], rgbaInt[3]) == rgbaValue;
						
						if(foundDuplicateRGBA && !readingData)
							return -1;
						else if(foundDuplicateRGBA && readingData) {
							System.out.println("Hash table look-up success");
							return hashed;
						}
						else {
							chain++;
						}
						
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
			System.out.println("Hash complete");
			return hashed;
		}
	
	public static String getFileNum(String filePath) {
		String fileNumStr = "";
		
		boolean isNumericChar = Character.isDigit(filePath.charAt(filePath.lastIndexOf('.')-1));
		for(int j = filePath.lastIndexOf('.')-1, iterator = 1; isNumericChar && j >= 0; j--, iterator++) {
			fileNumStr = String.valueOf(filePath.charAt(filePath.lastIndexOf('.')-iterator)) + fileNumStr;
			isNumericChar = j > 0 ? Character.isDigit(filePath.charAt(
					filePath.lastIndexOf('.')-(iterator+1))) : false;
		}
		return fileNumStr;
	}
	
	public static String getFilePrefix(String path) {
		String sub = path.substring(path.lastIndexOf('\\')+1, path.lastIndexOf('.'));
		String prefix = "";
		boolean endOfPrefixFound = false;
		for(int i = 0; i < sub.length() && !endOfPrefixFound; i++) {
			if(Character.isDigit(sub.charAt(i))) {
				endOfPrefixFound = true;
			}
			else {
				prefix += sub.charAt(i);
			}
		}
		return prefix;
	}
	
	/**
	 * custom ASCII char casting, instead of java native casting.
	 * java native casting returns 0-127 for ascii values 0-127, then 128th value is mapped to -128, 129th mapped to -127.. etc
	 * This method will cast each char to it's ASCII value (0-255 inclusive)
	 * 
	 * @param c character to get ASCII value of
	 * @return ASCII value 0-255 inclusive
	 */
	public static int charToASCII(char c) {
		byte b = (byte)c;
		if(b >= 0) {
			return (int)b;
		}
		else {
			if(b <= -65) {
				int distance = (int)Math.abs(b + 65);
				return (int)Math.abs(b + distance * 2 + 1) + 127;
			}else {
				int distance = b + 64;
				return (int)Math.abs(b - distance * 2 - 1) + 127;
			}
		}
	}
	
	public static int decodeByte(byte b) {
		if(b >= 0) {
			return (int)b;
		}
		else {
			if(b <= -65) {
				int distance = (int)Math.abs(b + 65);
				return (int)Math.abs(b + distance * 2 + 1) + 127;
			}else {
				int distance = b + 64;
				return (int)Math.abs(b - distance * 2 - 1) + 127;
			}
		}
	}
	
	
	public static Color mixColorsWithAlpha(Color color1, Color color2, boolean multiply_combo_outputs) {	
		double normalizedC1Alpha = normalize(color1.getAlpha(), 255, 1),
				normalizedC2Alpha = normalize(color2.getAlpha(), 255, 1);
		
	    double factor = 1 - (1 - normalizedC2Alpha) * (1 - normalizedC1Alpha);
	    int red = (int)Math.round((color2.getRed() * normalizedC2Alpha / factor) + (color1.getRed() * normalizedC1Alpha * (1 - normalizedC2Alpha) / factor));
	    int green = (int)Math.round((color2.getGreen() * normalizedC2Alpha / factor) + (color1.getGreen() * normalizedC1Alpha * (1 - normalizedC2Alpha) / factor));
	    int blue = (int)Math.round((color2.getBlue() * normalizedC2Alpha / factor) + (color1.getBlue() * normalizedC1Alpha * (1 - normalizedC2Alpha) / factor));
	    if(multiply_combo_outputs) {
	    	
	    	if(Math.random() > .3) {
		    	int maxColor = Math.max(red, Math.max(green, blue));
		    	double maxScaling = 255/(double)maxColor;
		    	double scalingRange = maxScaling - 1;
		    	double randScale = (Math.random()*scalingRange) + 1;
	    		return new Color((int)Math.min(Math.round(red*randScale), 255), (int)Math.min(Math.round(green*randScale), 255), 
	    				(int)Math.min(Math.round(blue*randScale), 255), (int)normalize(factor, 1, 255));
	    	}
	    	else {
		    	int minColor = Math.min(red, Math.min(green, blue));
		    	double minScaling = 1 / (double)minColor;
		    	double scalingRange = 1 - minScaling;
		    	double randScale = (Math.random()*scalingRange) + minScaling;
		    	return new Color((int)Math.max(Math.round(red*randScale), 0), (int)Math.max(Math.round(green*randScale), 0), 
	    				(int)Math.max(Math.round(blue*randScale), 0), (int)normalize(factor, 1, 255));
	    	}
	    }
	    else
	    	return new Color(red, green, blue, (int)normalize(factor, 1, 255));
	}
	
	/*
	public static Color mixColorsWithAlpha2(Color color1, Color color2)
	{	
		
		double normalizedC1Alpha = normalize(color1.getAlpha(), 255, 1),
				normalizedC2Alpha = normalize(color2.getAlpha(), 255, 1);
		
	    double factor = (1 - normalizedC1Alpha) * normalizedC2Alpha + normalizedC1Alpha;
	    double red = (((1 - normalizedC1Alpha)*normalizedC2Alpha*color2.getRed() + 
	    		normalizedC1Alpha*color1.getRed()) / factor);
	    double green = (((1 - normalizedC1Alpha)*normalizedC2Alpha*color2.getGreen() +
	    		normalizedC1Alpha*color1.getGreen()) / factor);
	    double blue = (((1 - normalizedC1Alpha)*normalizedC2Alpha*color2.getBlue() +
	    		normalizedC1Alpha*color1.getBlue()) / factor);
	    
	    System.out.println("new formula RGBA: " + red + ", " + green + ", " + blue + ", " + factor);
	    return new Color((float)red, (float)green, (float)blue, (float)factor);
	}
	
	public static Color mixColorsWithAlpha3(Color color1, Color color2)
	{	
		
		double norm_c1_r = normalize(color1.getRed(), 255, 1), norm_c1_g = normalize(color1.getGreen(), 255, 1),
				norm_c1_b = normalize(color1.getBlue(), 255, 1), norm_c2_r = normalize(color2.getRed(), 255, 1),
				norm_c2_g = normalize(color2.getGreen(), 255, 1), norm_c2_b = normalize(color2.getBlue(), 255, 1);
		
		double normalizedC1Alpha = normalize(color1.getAlpha(), 255, 1),
				normalizedC2Alpha = normalize(color2.getAlpha(), 255, 1);
		
	    double factor = (1 - normalizedC1Alpha) * normalizedC2Alpha + normalizedC1Alpha;
	    double red = (((1 - normalizedC1Alpha)*normalizedC2Alpha*norm_c2_r + 
	    		normalizedC1Alpha*norm_c1_r) / factor);
	    double green = (((1 - normalizedC1Alpha)*normalizedC2Alpha*norm_c2_g +
	    		normalizedC1Alpha*norm_c1_g) / factor);
	    double blue = (((1 - normalizedC1Alpha)*normalizedC2Alpha*norm_c2_b +
	    		normalizedC1Alpha*norm_c1_b) / factor);
	    
	    System.out.println("new formula RGBA: " + red + ", " + green + ", " + blue + ", " + factor);
	    return new Color((float)red, (float)green, (float)blue, (float)factor);
	}
	*/
	
	/**
	 * normalizes a number from the range 0-max1 to the range 0-max2
	 * @param value, what will be normalized
	 * @param max1, max value of previous scale
	 * @param max2, max value of new scale
	 * @return normalized value
	 */
	public static double normalize(double value, double max1, double max2) {
		double percentage = value/max1;
		return percentage*max2;
	}
	
	/**
	 * finds the nearest possible r, g, b, or a value (depending on first parameter), and returns the distance
	 * to that index (1 index = 2 bytes)
	 * @param colorArray an array of possible r, g, b, or a values that can be found within the RGBA values in the hash table
	 * @param startIndex the index to search from
	 * @return the index of the set of 2 bytes in the array that's closest to the inputted index
	 */
	public static int findNearestPossibleRGBA_Value(short[] colorArray, int startIndex) {
		boolean foundValidIndexAbove = false, foundValidIndexBelow = false;
		int validIndexAbove = Integer.MAX_VALUE, validIndexBelow = Integer.MIN_VALUE;
		
		for(int i = startIndex; i >= 0 && !foundValidIndexBelow; i -= 2) {
			if(colorArray[i] == 1) {
				foundValidIndexBelow = true;
				validIndexBelow = i;
			}
		}
		for(int i = startIndex; i < 512 && !foundValidIndexAbove; i += 2) {
			if(colorArray[i] == 1) {
				foundValidIndexAbove = true;
				validIndexAbove = i;
			}
		}
		
		if(foundValidIndexAbove && foundValidIndexBelow) {
			return validIndexAbove - startIndex < startIndex - validIndexBelow ? validIndexAbove/2 : validIndexBelow/2;
		}
		else if(foundValidIndexAbove) {
			return validIndexAbove/2;
		}
		else if(foundValidIndexBelow) {
			return validIndexBelow/2;
		}
		else {
			JOptionPane.showMessageDialog(null, "Util.findNearestPossibleRGBA_Value(): expected to find valid value, none found.",
					"\\(-_0)~", JOptionPane.ERROR_MESSAGE);
			return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * 
	 * @param base10NumbersToMapToIndividualDigits  first argument is the base converting from, following arguments are the digits of that base
	 * @return base 10 converted value
	 * @throws Exception
	 */
	public static long baseConversionToBase10(int ...base10NumbersToMapToIndividualDigits) throws Exception {
		BigDecimal value = new BigDecimal(0);
		for(int i = 0; i < base10NumbersToMapToIndividualDigits.length - 1; i++) {
			int base10NumberToMap = base10NumbersToMapToIndividualDigits[i+1];
			if(base10NumberToMap < 0 || base10NumberToMap > 255) {
				throw new Exception("Error, invalid value of " + base10NumberToMap + " can\'t be converted to a digit in base256."
						+ " The range of valid inputs is 0-255 inclusive.");
			}
			
			value = value.add(new BigDecimal(Math.pow(base10NumbersToMapToIndividualDigits[0], base10NumbersToMapToIndividualDigits.length - i - 1 - 1) * base10NumberToMap));
			
		}
		return Long.parseLong((value.toString().contains(".") ? value.toString().substring(0, value.toString().indexOf(".")) : value.toString()));
	}
	
	
	public static void zeroPadFile(File f, long numZeros) {
		PrintWriter pw = null;
		try {
			
			pw = new PrintWriter(f);
			
			int chunkSize = 10000000;
			
			String zeroChunks = Util.createSingularString('0', chunkSize);
			
			while(numZeros > 0) {
				if(numZeros - chunkSize < 0) {
					String zeros = Util.createSingularString('0', numZeros);
					pw.print(zeros);
					numZeros = 0;
					
				}
				else {
					pw.print(zeroChunks);
					numZeros -= chunkSize;
				}
			}
			
			pw.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void appendToFile(String data, File writeTo) {
		//syntax for resource try block - automatically closed resources
		try(FileWriter writer = new FileWriter(new File(writeTo.getAbsolutePath()), true)) {

			writer.append(data);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void write(File f, String data, long byteIndex) {
		//syntax for resource try block - automatically closed resources
		try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(Paths.get(f.getAbsolutePath()), StandardOpenOption.WRITE) ){
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
	
	public static void createGPUHash(String path) {
		File f = new File(path);
		if(!f.exists()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//21810380 = (int)(256 * 256 * 256 * 1.3);
		int indexesInHash = (int)(256 * 256 * 256 * 1.3);
		int bytesInHash = 14 * indexesInHash;
		System.out.println(bytesInHash);
		
		try(OutputStream os = new FileOutputStream(f)) {
		
			int chunkSize = 10000000;
			
			int bytesToWrite = bytesInHash;
			
			while(bytesToWrite != 0) {
				
				if(bytesToWrite - chunkSize < 0) {
					
					byte[] toWrite = new byte[bytesToWrite];
					for(int i = 0; i < toWrite.length; i++) {
						toWrite[i] = 0;
					}
					os.write(toWrite);
					bytesToWrite = 0;
					
				}
				else {
					
					byte[] toWrite = new byte[chunkSize];
					for(int i = 0; i < toWrite.length; i++) {
						toWrite[i] = 0;
					}
					os.write(toWrite);
					bytesToWrite -= chunkSize;
					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
