import java.io.*;
import java.util.*;

/*
 * The StartRemotePeers class begins remote peer processes. 
 * It reads configuration file PeerInfo.cfg and starts remote peer processes.
 * You must modify this program a little bit if your peer processes are written in C or C++.
 * Please look at the lines below the comment saying IMPORTANT.
 */
public class startRemotePeers {
	public ArrayList<RemotePeerInfo> peerInfoList = new ArrayList<>();
	public ArrayList<Process> peerProcesses = new ArrayList<>();

	public void getConfiguration() {
		String st;
		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));
			int i = 0;
			while ((st = in.readLine()) != null) {
				String[] tokens = st.split("\\s+");
				peerInfoList.add(new RemotePeerInfo(tokens[0], tokens[1], tokens[2], i));
				i++;
			}
			in.close();
		} catch (Exception ex) {
			System.out.println("Exception:" + ex.toString());
		}
	}

	/**
	 * Checks if all peer has down loaded the file
	 */
	public static synchronized boolean downloadComplete() {

		String line;
		int hasFileCount = 1;

		try {
			BufferedReader in = new BufferedReader(new FileReader("PeerInfo.cfg"));

			while ((line = in.readLine()) != null) {
				hasFileCount = hasFileCount * Integer.parseInt(line.trim().split("\\s+")[3]);
			}
			if (hasFileCount == 0) {
				in.close();
				return false;
			} else {
				in.close();
				return true;
			}

		} catch (Exception e) {

			return false;
		}

	}

	/**
	 * @param args
	 **/
	public static void main(String[] args) {
		try {
			startRemotePeers myStart = new startRemotePeers();
			myStart.getConfiguration();

			// get current path
			String path = System.getProperty("user.dir");

			// start clients at remote hosts
			for (int i = 0; i < myStart.peerInfoList.size(); i++) {
				RemotePeerInfo pInfo = (RemotePeerInfo) myStart.peerInfoList.get(i);

				System.out.println("Start remote peer " + pInfo.peerId + " at " + pInfo.peerAddress);
				String command = "ssh " + pInfo.peerAddress + " cd " + path + "; java PeerProcess " + pInfo.peerId;
				myStart.peerProcesses.add(Runtime.getRuntime().exec(command));
				System.out.println(command);
			}

			System.out.println("Download in progress....");

			boolean downloadComplete = false;
			while (true) {
				// checks for termination
				downloadComplete = downloadComplete();
				if (downloadComplete) {
					System.out.println("All the peers are terminated successfully");
					break;
				} else {
					try {
						Thread.currentThread();
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
					}
				}
			}

		} catch (Exception ex) {
			System.out.println("Exception: " + ex.toString());
		}
	}
}