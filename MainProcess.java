import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainProcess implements Runnable {
	private ServerSocket SocketListening;
	private String peerID;
	Socket remoteSocket;
	Thread sendingProcess;

	public MainProcess(ServerSocket socket, String peerID) {
		this.SocketListening = socket;
		this.peerID = peerID;
	}

	public void run() {
		while (true) {
			try {
				remoteSocket = SocketListening.accept();
				// instantiates thread for handling individual remote peer
				sendingProcess = new Thread(new RemotePeerHandler(remoteSocket, 0, peerID));

				PeerProcess.sendingProcess.add(sendingProcess);
				sendingProcess.start();
			} catch (Exception e) {
				PeerProcess.writeToLogFile(this.peerID + " Exception in connection: " + e.toString());
			}
		}
	}

	public void releaseSocket() {
		try {
			if (!remoteSocket.isClosed())
				remoteSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
