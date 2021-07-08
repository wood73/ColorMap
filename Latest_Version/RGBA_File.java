package wood;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;


public class RGBA_File {
	
	public String getHashStatus() {
		return String.valueOf(hashTableEntries / ((double)hashTableSize / 11));
	}
	
	public File f;
	public long hashTableSize;
	
	//number of current entries into the hash table
	public long hashTableEntries = 0;
	
	//size in bytes
	public int metadataSize = 500, rgbaComponentSize = 256;
	public long fileSize;
	
	public int hashTableStartIndex = metadataSize + rgbaComponentSize * 4;
	
	//size of hashtable stored in bytes 0-10 inclusive - make sure it's 0 padded left to take up 11 characters
	String metadataHashTableSize = String.format("%11s", String.valueOf(hashTableSize)).replace(" ", "0");
	
	//lists of r, g, b, and a values that refer which values exist inside the RGBA part of the hash table
	short[] possible_r_values = new short[512], possible_g_values = new short[512], possible_b_values = new short[512],
			possible_a_values = new short[512];
	
	//what to add to a byte value to get it in the range 0-255
	int byteOffset = 128;
	
	//file that takes every inserted rgba value, and densely writes it to an additional file
	File rgbaFileDense;
	
	public ArrayList<String> asciiFormatBuffer = null;
	public File binaryFile;
	boolean noBinaryFile;
	OutputStream os;
	
	public RGBA_File(String filePath, long initialHashTableIndexSize, String rgbaFilePath, ArrayList<String> asciiFormatBuffer, String binPath) {
		this.hashTableSize = initialHashTableIndexSize * 11;
		this.f = new File(filePath);
		this.rgbaFileDense = new File(rgbaFilePath);
		this.asciiFormatBuffer = asciiFormatBuffer; //shallow copy
		
		fileSize = metadataSize + rgbaComponentSize * 4 + hashTableSize;
		
		//create dense file if it doesn't exist, and simultaneously remove all contents inside if it exists
		try(PrintWriter pw = new PrintWriter(rgbaFileDense)) {
			pw.print("00000000000");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		noBinaryFile = binPath.trim().length() == 0;
		if(!noBinaryFile) {
			binaryFile = new File(binPath);
			if(!binaryFile.exists()) {
				try {
					binaryFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				os = new FileOutputStream(binaryFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		initializeFile();
	}
	
	public void initializeFile() {
		
		PrintWriter pw = null;
		try {
			
			pw = new PrintWriter(f);
			
			//Create a String representing every byte that will be entered into the txt file
			//note risk running out of memory with Strings over 500 million characters
			String zeros = RGBA_File_Util.createSingularString('0', fileSize);
			
			//String size hardly has an impact on the speed of String.substring()
			//set first 11 bytes to hold size of the hash table
			zeros = String.format("%11s", String.valueOf(hashTableSize)).replace(" ", "0") + zeros.substring(11);
			
			//write the data to file (overwrites if the file exists)
			pw.print(zeros);
			
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			//try { is.close(); br.close(); isr.close(); } catch (IOException e) { e.printStackTrace(); }
			pw.close();
		}
		
		//each possible r, g, b & a values, these arrays are stored in sets of 2, where the first element
		//in a set is 0 if a value has not been entered into that set yet, and will switch to
		//1 if a value gets entered into that set
		for(int i = 0; i < 512; i++) {
			if(i % 2 == 0) {
				possible_r_values[i] = (short)0;
				possible_g_values[i] = (short)0;
				possible_b_values[i] = (short)0;
				possible_a_values[i] = (short)0;
			}
			else {
				int index = (int)Math.floor(i/2);
				possible_r_values[i] = (short)index;
				possible_g_values[i] = (short)index;
				possible_b_values[i] = (short)index;
				possible_a_values[i] = (short)index;
			}
		}
	}
	
	public void write(String data, long byteIndex) {
		//syntax for resource try block - automatically closed resources
		try (AsynchronousFileChannel afc = AsynchronousFileChannel.open(Paths.get(f.getAbsolutePath()), StandardOpenOption.WRITE) ){
			ByteBuffer bb = ByteBuffer.allocate(data.length());
			for(char c : data.toCharArray())
				bb.put((byte)c);
			
			bb.limit(bb.position());
			bb.position(0);
			Future<Integer> future = afc.write(bb, byteIndex);
			
			//get() makes sure the file write completes before moving on
			//if not called (might be risky), on a small scale, tested to save time by 0.62124 %
			future.get();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public String read(long byteIndex, int length) {
		String data = "";
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(f);
			fin.skip(byteIndex);
			byte[] byteData = new byte[length];
			fin.read(byteData);
			
			for(byte b : byteData) {
				data += (char)RGBA_File_Util.decodeByte(b);
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
	
	/**Hashes the RGBA value converted to base 256, then applies a mix of linear & quadratic probing to find
	 * an available index within the hash table
	 * 
	 * @return true if the RGBA value was not already found in the hash table, and thus inserted into it.
	 * false if the same RGBA value was found in the hash table, and thus not inserted
	 */
	public boolean insertRGBA(int r, int g, int b, int a, int r2, int ra, int g2, int ga, int b2, int ba) {
		
		
		String asciiFormat = String.format("%c%c%c%c%c%c%c%c%c%c", 
				(char)r, (char)g, (char)b, (char)a, (char)r2, (char)ra, (char)g2, (char)ga, (char)b2, (char)ba);
		
		long rgbaValue = -1;
		try {
			rgbaValue = Util.baseConversionToBase10(256, r, g, b, a);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		long hash = RGBA_File_Util.hash(rgbaValue, hashTableSize/11, this);
		long byteIndexAtHash = hashTableStartIndex + hash * 11;
		
		//return false if a duplicate rgba value was found in the hash table
		if(hash == -1)
			return false;
		
		
		possible_r_values[r*2] = 1;
		possible_g_values[g*2] = 1;
		possible_b_values[b*2] = 1;
		possible_a_values[a*2] = 1;
		
		write("1" + asciiFormat, byteIndexAtHash);
		if(rgbaFileDense != null) {
			asciiFormatBuffer.add(asciiFormat);
			if(asciiFormatBuffer.size() > 200000) {
				emptyBuffer();
			}
		}
		
		hashTableEntries++;
		
		return true;
	}
	
	public void emptyBuffer() {
		RGBA_File_Util.appendToFile(asciiFormatBuffer, rgbaFileDense);
		if(!noBinaryFile)
			RGBA_File_Util.appendBinaryToFile(os, asciiFormatBuffer, binaryFile);
		asciiFormatBuffer = new ArrayList<String>();
	}
	
	public void close() {
		try {
			if(!noBinaryFile)
				os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
