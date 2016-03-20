package SessionManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ReaderTest {
	public static void main(String[] args) throws IOException{
		try {
			BufferedReader reader = new BufferedReader(new FileReader("resource/index.html"));
			String indexContent = "";
			String line = reader.readLine();
			while(line != null){
				indexContent += line;
				line = reader.readLine();
			}
			System.out.println(indexContent);
			
			
			String s = "$";
			System.out.println();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
