import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class WritingLog {
	static OutputStreamWriter outputStreamWriter;
	static FileOutputStream file;

	public static void start(String fileName) throws IOException {
		file = new FileOutputStream(fileName);
		outputStreamWriter = new OutputStreamWriter(file, "UTF-8");
	}

	public static void closeWrite() {
		try {

			outputStreamWriter.flush();
			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeLog(String content) {
		try {
			outputStreamWriter.write(content + '\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
