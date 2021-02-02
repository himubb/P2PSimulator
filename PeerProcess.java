import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PeerProcess {
	public ServerSocket listeningSocket = null;
	public int portNumberForListening;
	public static String peerID;
	public int currentPeerIndex;
	public Thread receivingProcess; // Thread for listening to remote clients
	public static boolean downloadComplete = false;
	public static MessagePayload localMessagePayload = null;

	public static void createEmptyFile() {
		try {
			File dir = new File(peerID);
			dir.mkdir();

			File newfile = new File(peerID, ConfigurationProperties.fileName);
			OutputStream os = new FileOutputStream(newfile, true);
			byte b = 0;

			for (int counter = 0; counter < ConfigurationProperties.fileSize; counter++)
				os.write(b);
			os.close();
		} catch (Exception e) {
			writeToLogFile(peerID + " ERROR in creating the file : " + e.getMessage());
		}

	}

	public static void getPeerInfo() {
		try {
			File config = new File("PeerInfo.cfg");
			Scanner read = new Scanner(config);
			int counter = 0;
			while (read.hasNextLine()) {
				String data = read.nextLine().trim();
				String[] map = data.split(" ");
				peerInfoTable.put(map[0],
						new RemotePeerInfo(map[0], map[1], map[2], Integer.parseInt(map[3]), counter));
				counter++;
			}
			read.close();
		} catch (Exception ex) {
			writeToLogFile(peerID + "peer info" + ex.toString());
		}
	}

	public static void updatePeerInfo() {
		try {
			File config = new File("PeerInfo.cfg");
			Scanner read = new Scanner(config);

			while (read.hasNextLine()) {
				String data = read.nextLine();
				String[] params = data.trim().split("\\s+");
				String peerID = params[0];
				int isCompleted = Integer.parseInt(params[3]);
				if (isCompleted == 1) {
					peerInfoTable.get(peerID).isCompleted = 1;
					peerInfoTable.get(peerID).isInterested = 0;
					peerInfoTable.get(peerID).isChoked = 0;
				}
			}
			read.close();
		} catch (Exception e) {
			writeToLogFile(peerID + "peerinfo" + e.toString());
		}
	}

	public static volatile Timer timerCheck;
	public static volatile Hashtable<String, RemotePeerInfo> peerInfoTable = new Hashtable<String, RemotePeerInfo>();
	public static volatile Hashtable<String, RemotePeerInfo> preferredNeighborsTable = new Hashtable<String, RemotePeerInfo>();
	public static volatile Hashtable<String, RemotePeerInfo> unchokedNeighborsTable = new Hashtable<String, RemotePeerInfo>();
	public static volatile Queue<FinalMessage> finalMessageQueue = new LinkedList<FinalMessage>();
	public static Hashtable<String, Socket> socketTable = new Hashtable<String, Socket>();
	public static Vector<Thread> receivingProcessList = new Vector<Thread>();
	public static Vector<Thread> sendingProcess = new Vector<Thread>();
	public static Thread handleMessages;


	public static class NeighborSelection extends TimerTask {
		public void run() {
			// updates peerInfoTable
			updatePeerInfo();
			Enumeration<String> keys = peerInfoTable.keys();
			int countInterested = 0;
			String strPref = "";
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				RemotePeerInfo pref = peerInfoTable.get(key);
				if (key.equals(peerID))
					continue;
				if (pref.isCompleted == 0 && pref.isHandShaked == 1) {
					countInterested++;
				} else if (pref.isCompleted == 1) {
					try {
						preferredNeighborsTable.remove(key);
					} catch (Exception e) {
					}
				}
			}
			if (countInterested > ConfigurationProperties.preferredNeighborCount) {
				boolean flag = preferredNeighborsTable.isEmpty();
				if (!flag)
					preferredNeighborsTable.clear();
				List<RemotePeerInfo> peerValues = new ArrayList<RemotePeerInfo>(peerInfoTable.values());
				Collections.sort(peerValues, new Comparator<RemotePeerInfo>() {
					public int compare(RemotePeerInfo peer1, RemotePeerInfo peer2) {
						if (peer1 == null && peer2 == null)
							return 0;

						if (peer1 == null)
							return 1;

						if (peer2 == null)
							return -1;

						// Compare downloading speed logic in compareTo
						if (peer1 instanceof Comparable) {
							return peer2.compareTo(peer1);
						} else {
							return peer2.toString().compareTo(peer1.toString());

						}
					}
				});
				int count = 0;
				for (int counter = 0; counter < peerValues.size(); counter++) {
					if (count > ConfigurationProperties.preferredNeighborCount - 1)
						break;
					if (peerValues.get(counter).isHandShaked == 1 && !peerValues.get(counter).peerId.equals(peerID)
							&& peerInfoTable.get(peerValues.get(counter).peerId).isCompleted == 0) {
						peerInfoTable.get(peerValues.get(counter).peerId).isPreferredNeighbor = 1;
						preferredNeighborsTable.put(peerValues.get(counter).peerId,
								peerInfoTable.get(peerValues.get(counter).peerId));

						count++;

						strPref = strPref + peerValues.get(counter).peerId + ", ";

						if (peerInfoTable.get(peerValues.get(counter).peerId).isChoked == 1) {
							unchokingPeers(PeerProcess.socketTable.get(peerValues.get(counter).peerId),
									peerValues.get(counter).peerId);
							PeerProcess.peerInfoTable.get(peerValues.get(counter).peerId).isChoked = 0;
							sendHave(PeerProcess.socketTable.get(peerValues.get(counter).peerId),
									peerValues.get(counter).peerId);
							PeerProcess.peerInfoTable.get(peerValues.get(counter).peerId).state = 3;
						}

					}
				}
			} else

			{
				keys = peerInfoTable.keys();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					RemotePeerInfo pref = peerInfoTable.get(key);
					if (key.equals(peerID))
						continue;

					if (pref.isCompleted == 0 && pref.isHandShaked == 1) {
						if (!preferredNeighborsTable.containsKey(key)) {
							strPref = strPref + key + ", ";
							preferredNeighborsTable.put(key, peerInfoTable.get(key));
							peerInfoTable.get(key).isPreferredNeighbor = 1;
						}
						if (pref.isChoked == 1) {
							unchokingPeers(PeerProcess.socketTable.get(key), key);
							PeerProcess.peerInfoTable.get(key).isChoked = 0;
							sendHave(PeerProcess.socketTable.get(key), key);
							PeerProcess.peerInfoTable.get(key).state = 3;
						}

					}

				}
			}
			// RESULT 3: Preferred Neighbors
			if (strPref != "") {
				strPref = strPref.replaceAll(", $", "");
				PeerProcess.writeToLogFile(PeerProcess.peerID + " has the preferred neighbors " + strPref + ".");
			}

		}

	}

	private static void unchokingPeers(Socket socket, String remotePeerID) {
		writeToLogFile(peerID + " is sending the 'unchoke' message to " + remotePeerID);
		Message d = new Message(MessageType.UNCHOKE);
		byte[] msgByte = Message.packMsg(d);
		writeToOutputStream(socket, msgByte);
	}

	private static void sendHave(Socket socket, String remotePeerID) {
		byte[] encodedMessagePayload = PeerProcess.localMessagePayload.encode();

		writeToLogFile(peerID + " is sending the 'have' message to " + remotePeerID);
		Message d = new Message(MessageType.HAVE, encodedMessagePayload);
		writeToOutputStream(socket, Message.packMsg(d));
		encodedMessagePayload = null;
	}

	private static int writeToOutputStream(Socket socket, byte[] encodedMessagePayload) {
		try {
			OutputStream out = socket.getOutputStream();
			out.write(encodedMessagePayload);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	
	public static class OptimisticallyUnchokingNeighbors extends TimerTask {

		public void run() {
			// updates peerInfoTable
			updatePeerInfo();
			if (!unchokedNeighborsTable.isEmpty())
				unchokedNeighborsTable.clear();
			Enumeration<String> keys = peerInfoTable.keys();
			Vector<RemotePeerInfo> peers = new Vector<RemotePeerInfo>();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				RemotePeerInfo pref = peerInfoTable.get(key);
				if (pref.isChoked == 1 && !key.equals(peerID) && pref.isCompleted == 0 && pref.isHandShaked == 1)
					peers.add(pref);
			}

			// Randomize the vector elements
			if (peers.size() > 0) {
				Collections.shuffle(peers);
				RemotePeerInfo p = peers.firstElement();

				peerInfoTable.get(p.peerId).isOptUnchokedNeighbor = 1;
				unchokedNeighborsTable.put(p.peerId, peerInfoTable.get(p.peerId));
				// RESULT 4:
				PeerProcess.writeToLogFile(
						PeerProcess.peerID + " has the optimistically unchoked neighbor " + p.peerId + ".");

				if (peerInfoTable.get(p.peerId).isChoked == 1) {
					PeerProcess.peerInfoTable.get(p.peerId).isChoked = 0;
					unchokingPeers(PeerProcess.socketTable.get(p.peerId), p.peerId);
					sendHave(PeerProcess.socketTable.get(p.peerId), p.peerId);
					PeerProcess.peerInfoTable.get(p.peerId).state = 3;
				}
			}

		}

	}

	public static void startUnChokedNeighbors() {
		timerCheck = new Timer();
		timerCheck.schedule(new OptimisticallyUnchokingNeighbors(), 0,
				ConfigurationProperties.optimisticUnchokingInterval * 1000);
	}

	public static void stopUnChokedNeighbors() {
		timerCheck.cancel();
	}

	public static void startPreferredNeighbors() {
		timerCheck = new Timer();
		timerCheck.schedule(new NeighborSelection(), 0, ConfigurationProperties.unchokingInterval * 1000);
	}

	public static void stopPreferredNeighbors() {
		timerCheck.cancel();
	}

	public static String getTime() {
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
		return date.format(calendar.getTime());

	}

	public static void writeToLogFile(String message) {

		WritingLog.writeLog(getTime() + ": Peer " + message);
		System.out.println(getTime() + ": Peer " + message);
	}


	public static void getCommonProperties() {

		try {
			File config = new File("Common.cfg");
			Scanner read = new Scanner(config);
			while (read.hasNextLine()) {
				String data = read.nextLine();
				String[] params = data.split("\\s+");
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
			writeToLogFile(peerID + "common config" + e.toString());
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) {
		PeerProcess currentPeerProcess = new PeerProcess();
		peerID = args[0];

		try {
			// starts saving standard output to log file
			WritingLog.start("log_peer_" + peerID + ".log");
			writeToLogFile(peerID + " is started");

			// reads Common.cfg file and populates ConfigurationProperties class
			getCommonProperties();

			// reads PeerInfo.cfg file and populates RemotePeerInfo class
			getPeerInfo();
			initializePrefferedNeighbours();

			boolean isFirstPeer = false;

			Enumeration<String> e = peerInfoTable.keys();

			while (e.hasMoreElements()) {
				RemotePeerInfo peerInfo = peerInfoTable.get(e.nextElement());
				if (peerInfo.peerId.equals(peerID)) {
					// checks if the peer is the first peer or not
					currentPeerProcess.portNumberForListening = Integer.parseInt(peerInfo.peerPort);
					currentPeerProcess.currentPeerIndex = peerInfo.peerIndex;
					if (peerInfo.getIsFirstPeer() == 1) {
						isFirstPeer = true;
						break;
					}
				}
			}

			// Initialize the Bit field class
			localMessagePayload = new MessagePayload();
			localMessagePayload.initOwnBitfield(peerID, isFirstPeer ? 1 : 0);

			handleMessages = new Thread(new HandleMessages(peerID));
			handleMessages.start();

			if (isFirstPeer) {
				try {
					currentPeerProcess.listeningSocket = new ServerSocket(currentPeerProcess.portNumberForListening);

					// instantiates and starts Listening Thread
					currentPeerProcess.receivingProcess = new Thread(
							new MainProcess(currentPeerProcess.listeningSocket, peerID));
					currentPeerProcess.receivingProcess.start();
				} catch (SocketTimeoutException tox) {
					writeToLogFile(peerID + " gets time out expetion: " + tox.toString());
					WritingLog.closeWrite();
					System.exit(0);
				} catch (IOException ex) {
					writeToLogFile(peerID + " gets exception in Starting Listening thread: "
							+ currentPeerProcess.portNumberForListening + ex.toString());
					WritingLog.closeWrite();
					System.exit(0);
				}
			}
			// Not the first peer
			else {
				createEmptyFile();

				e = peerInfoTable.keys();
				while (e.hasMoreElements()) {
					RemotePeerInfo peerInfo = peerInfoTable.get(e.nextElement());
					if (currentPeerProcess.currentPeerIndex > peerInfo.peerIndex) {
						Thread tempThread = new Thread(new RemotePeerHandler(peerInfo.getPeerAddress(),
								Integer.parseInt(peerInfo.getPeerPort()), 1, peerID));
						receivingProcessList.add(tempThread);
						tempThread.start();
					}
				}

				// Spawns a listening thread
				try {
					currentPeerProcess.listeningSocket = new ServerSocket(currentPeerProcess.portNumberForListening);
					currentPeerProcess.receivingProcess = new Thread(
							new MainProcess(currentPeerProcess.listeningSocket, peerID));
					currentPeerProcess.receivingProcess.start();
				} catch (SocketTimeoutException x) {
					writeToLogFile(
							peerID + " gets time out exception in Starting the listening thread: " + x.toString());
					WritingLog.closeWrite();
					System.exit(0);
				} catch (IOException ex) {
					writeToLogFile(peerID + " gets exception in Starting the listening thread: "
							+ currentPeerProcess.portNumberForListening + " " + ex.toString());
					WritingLog.closeWrite();
					System.exit(0);
				}
			}

			startPreferredNeighbors();
			startUnChokedNeighbors();

			while (true) {
				// checks for termination
				downloadComplete = downloadComplete();
				if (downloadComplete) {
					writeToLogFile("All peers have completed downloading the file.");

					stopPreferredNeighbors();
					stopUnChokedNeighbors();

					try {
						Thread.currentThread();
						Thread.sleep(2000);
					} catch (InterruptedException ex) {
					}

					if (currentPeerProcess.receivingProcess.isAlive())
						currentPeerProcess.receivingProcess.stop();

					if (handleMessages.isAlive())
						handleMessages.stop();

					for (int counter = 0; counter < receivingProcessList.size(); counter++)
						if (receivingProcessList.get(counter).isAlive())
							receivingProcessList.get(counter).stop();

					for (int counter = 0; counter < sendingProcess.size(); counter++)
						if (sendingProcess.get(counter).isAlive())
							sendingProcess.get(counter).stop();

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
			writeToLogFile(peerID + " Exception in ending : " + ex.getMessage());
		} finally {
			writeToLogFile(peerID + " process has been terminated");
			WritingLog.closeWrite();
			System.exit(0);
		}
	}

	private static void initializePrefferedNeighbours() {
		Enumeration<String> keys = peerInfoTable.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if (!key.equals(peerID)) {
				preferredNeighborsTable.put(key, peerInfoTable.get(key));
			}
		}
	}

	
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
			writeToLogFile(e.toString());
			return false;
		}

	}

	public static synchronized void addToMessageQueue(FinalMessage message) {
		finalMessageQueue.add(message);
	}

	public static synchronized FinalMessage removeFromMessageQueue() {
		FinalMessage message = null;
		if (!finalMessageQueue.isEmpty()) {
			message = finalMessageQueue.remove();
		}
		return message;
	}

}
