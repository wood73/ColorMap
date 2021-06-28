package wood;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

public class RGBA_File_Util {
	
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
	
	public static long hash(long rgbaValue, long hashTableIndexSize, RGBA_File rf) {
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
			
			long byteIndexAtHash = rf.hashTableStartIndex + hashed * 11;
			boolean isUnusedIndex = rf.read(byteIndexAtHash, 1).equals("0") ? true : false;
			
			if(isUnusedIndex) {
				foundHash = true;
			} else {
				String rgba = rf.read(byteIndexAtHash+1, 4);
				try {
					
					foundDuplicateRGBA = Util.baseConversionToBase10(256, (int)rgba.charAt(0), (int)rgba.charAt(1),
							(int)rgba.charAt(2), (int)rgba.charAt(3)) == rgbaValue;
					
					if(foundDuplicateRGBA)
						return -1;
					
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
	
	
	public static Color mixColorsWithAlpha(Color color1, Color color2)
	{	
		double normalizedC1Alpha = normalize(color1.getAlpha(), 255, 1),
				normalizedC2Alpha = normalize(color2.getAlpha(), 255, 1);
		
	    double factor = 1 - (1 - normalizedC2Alpha) * (1 - normalizedC1Alpha);
	    int red = (int)Math.round((color2.getRed() * normalizedC2Alpha / factor) + (color1.getRed() * normalizedC1Alpha * (1 - normalizedC2Alpha) / factor));
	    int green = (int)Math.round((color2.getGreen() * normalizedC2Alpha / factor) + (color1.getGreen() * normalizedC1Alpha * (1 - normalizedC2Alpha) / factor));
	    int blue = (int)Math.round((color2.getBlue() * normalizedC2Alpha / factor) + (color1.getBlue() * normalizedC1Alpha * (1 - normalizedC2Alpha) / factor));
	    return new Color(red, green, blue, (int)normalize(factor, 1, 255));
	}
	
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
	
	public static void appendToFile(String data, File writeTo) {
		//syntax for resource try block - automatically closed resources
		try(FileWriter writer = new FileWriter(new File(writeTo.getAbsolutePath()), true)) {

			writer.append(data);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void appendToFile(ArrayList<String> data, File writeTo) {
		//syntax for resource try block - automatically closed resources
		try(FileWriter writer = new FileWriter(new File(writeTo.getAbsolutePath()), true)) {
			
			int chunkSize = 100000;
			String toWrite = "";
			for(int i = 0; i < data.size(); i++) {
				toWrite += data.get(i);
				if(toWrite.length() > chunkSize || i == data.size() - 1) {
					writer.append(toWrite);
					toWrite = "";
				}
			}
			
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
	
}
