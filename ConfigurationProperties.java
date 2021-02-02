import java.io.File;
import java.util.Scanner;

public class ConfigurationProperties {
	public static int preferredNeighborCount;
	public static int unchokingInterval;
	public static int optimisticUnchokingInterval;
	public static int fileSize;
	public static int pieceSize;
	public static String fileName;
	public static int numberOfPieces = (int) Math.ceil((double) fileSize / (double) pieceSize);

	public static void getCommonProperties() {

		try {
			File config = new File("Common.cfg");
			Scanner read = new Scanner(config);
			while (read.hasNextLine()) {
				String data = read.nextLine();
				String[] params = data.split(" ");
				if (params[0].equalsIgnoreCase("NumberOfPreferredNeighbors")) {
					ConfigurationProperties.preferredNeighborCount = Integer.parseInt(params[1]);
				} else if (params[0].equalsIgnoreCase("UnchokingInterval")) {
					ConfigurationProperties.unchokingInterval = Integer.parseInt(params[1]);
				} else if (params[0].equalsIgnoreCase("OptimisticUnchokingInterval")) {
					ConfigurationProperties.optimisticUnchokingInterval = Integer.parseInt(params[1]);
				} else if (params[0].equalsIgnoreCase("FileName")) {
					ConfigurationProperties.fileName = params[1];
				} else if (params[0].equalsIgnoreCase("FileSize")) {
					ConfigurationProperties.fileSize = Integer.parseInt(params[1]);
				} else if (params[0].equalsIgnoreCase("PieceSize")) {
					ConfigurationProperties.pieceSize = Integer.parseInt(params[1]);
				}
			}

			read.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
