package asutosh.google;

import org.apache.commons.codec.binary.Base64;

import java.io.*;

public class FileToBase64AndSaveToFile {
    public static void main(String[] args) {
        // Specify the input file path
        String inputFilePath = "C:\\Users\\asmaharana\\Documents\\Asutosh\\Google\\asutosh.drive\\target\\build\\asutosh.drive.esa";

        // Specify the output file path
        String outputFilePath = "C:\\Users\\asmaharana\\Documents\\Asutosh\\Google\\asutosh.drive\\target\\build\\asutosh.drive-Base64.txt";

        try {
            // Read the input file into a byte array
            File inputFile = new File(inputFilePath);
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            byte[] fileBytes = new byte[(int) inputFile.length()];
            fileInputStream.read(fileBytes);
            fileInputStream.close();

            // Convert the byte array to Base64
            byte[] base64Bytes = Base64.encodeBase64(fileBytes);
            String base64String = new String(base64Bytes);

            // Write the Base64 content to a new file
            File outputFile = new File(outputFilePath);
            FileWriter fileWriter = new FileWriter(outputFile);
            fileWriter.write(base64String);
            fileWriter.close();

            System.out.println("File converted to Base64 and saved to " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
