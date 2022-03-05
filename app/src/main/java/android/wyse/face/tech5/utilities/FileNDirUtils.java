package android.wyse.face.tech5.utilities;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FileNDirUtils.java  - FileNDirUtils is utility class to perform file operations..
 * @author Shravan Nitta
 * @version 1.0
 */
public class FileNDirUtils {
	/**
	 * getFileBytes(String fileName) method is used to get the fileByte from file name.
	 * @param fileName is the name of the file.
	 * @return it returns the file as byte array.*/
	public static byte[] getFileBytes(String fileName) throws IOException {
		File file = new File(fileName);
		InputStream is = new FileInputStream(file);
		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			System.out.println("File is too large to process");
			return null;
		}
		byte[] bytes = new byte[(int)length];
		int offset = 0;
		int numRead ;
		while((offset < bytes.length)&&((numRead=is.read(bytes, offset, bytes.length-offset)) >= 0)) {
			offset += numRead;
		}
		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + file.getName());
		}
		/*String str = new String(bytes);*/
		
		is.close();
		return bytes;
	}
	/**
	 * getFileString(String fileName) method is used to get the file Strings from file name.
	 * @param fileName is the name of the file.
	 * @return it returns the file as String.*/
	public static String getFileString(String fileName) {
		if (!(new File(fileName)).exists()) return null;

		try {
			StringBuffer objFileBuffer = new StringBuffer();
			FileInputStream objFileInputStream = new FileInputStream(new File(fileName));
			byte bytBuffer[] = new byte[objFileBuffer.length()];
			int size = objFileInputStream.read(bytBuffer);
			while(size != -1)
			{
				objFileBuffer.append(new String(bytBuffer, 0, size));
				size = objFileInputStream.read(bytBuffer);
			}
			objFileInputStream.close();

			return objFileBuffer.toString();
		} 
		catch(IOException e) {
			System.out.println("ERROR: " + e);
		}
		return null;
	}
}