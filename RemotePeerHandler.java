import java.net.*;
import java.io.*;

public class RemotePeerHandler implements Runnable {

	private InputStream inputStream;
	private OutputStream outputStream;
	private int connectionType;
	private Socket peerSocket = null;
	private Handshake handshakeMessage;

	String localPeerId, remotePeerId;

	public void run() {
		byte[] handshakeMsg = new byte[32];
		byte[] msgLength;
		byte[] msgType;
		byte[] dataWithoutPayload = new byte[5];

		FinalMessage finalMessage = new FinalMessage();

		try {
			if (this.connectionType == 1) {
				if (sendingHandshake()) {
					PeerProcess.writeToLogFile(localPeerId + " 'handshake' has been sent");

				} else {
					PeerProcess.writeToLogFile(localPeerId + " 'handshake' sending failed.");
					System.exit(0);
				}
				while (true) {
					inputStream.read(handshakeMsg);
					handshakeMessage = Handshake.populateMessage(handshakeMsg);
					if (handshakeMessage.getHeaderString().equals("P2PFILESHARINGPROJ")) {

						remotePeerId = handshakeMessage.getStringPeerId();
						// RESULT 1
						PeerProcess.writeToLogFile(localPeerId + " makes a connection to " + remotePeerId);
						// Showing Handshake
						PeerProcess.writeToLogFile(
								localPeerId + " received the 'handshake' message from " + remotePeerId);

						PeerProcess.socketTable.put(remotePeerId, this.peerSocket);
						// RESULT 2
						PeerProcess.writeToLogFile(localPeerId + " is connected from " + remotePeerId);
						break;
					} else {
						continue;
					}
				}

				Message msg = new Message(MessageType.BITFIELD, PeerProcess.localMessagePayload.encode());
				byte[] byteArray = Message.packMsg(msg);
				outputStream.write(byteArray);
				PeerProcess.peerInfoTable.get(remotePeerId).state = 8;
			} else {
				while (true) {
					inputStream.read(handshakeMsg);
					handshakeMessage = Handshake.populateMessage(handshakeMsg);
					if (handshakeMessage.getHeaderString().equals("P2PFILESHARINGPROJ")) {
						remotePeerId = handshakeMessage.getStringPeerId();

						PeerProcess.writeToLogFile(
								localPeerId + " received the 'handshake' message from " + remotePeerId);
						PeerProcess.socketTable.put(remotePeerId, this.peerSocket);
						PeerProcess.writeToLogFile(localPeerId + " is connected from " + remotePeerId);
						break;
					} else {
						continue;
					}
				}
				if (sendingHandshake()) {
					PeerProcess.writeToLogFile(localPeerId + " 'handshake' message has been sent successfully.");

				} else {
					PeerProcess.writeToLogFile(localPeerId + " 'handshake' message sending failed.");
					System.exit(0);
				}

				PeerProcess.peerInfoTable.get(remotePeerId).state = 2;
			}

			while (true) {

				int headerBytes = inputStream.read(dataWithoutPayload);

				if (headerBytes == -1)
					break;

				msgLength = new byte[4];
				msgType = new byte[1];
				System.arraycopy(dataWithoutPayload, 0, msgLength, 0, 4);
				System.arraycopy(dataWithoutPayload, 4, msgType, 0, 1);
				Message dataMessage = new Message();
				dataMessage.setMessageType(msgType);
				dataMessage.setMessageLength(msgLength);

				if (dataMessage.getMessageTypeString().equals(MessageType.CHOKE)
						|| dataMessage.getMessageTypeString().equals(MessageType.UNCHOKE)
						|| dataMessage.getMessageTypeString().equals(MessageType.INTERESTED)
						|| dataMessage.getMessageTypeString().equals(MessageType.NOTINTERESTED)) {
					finalMessage.dataMessage = dataMessage;
					finalMessage.targetPeerId = this.remotePeerId;
					PeerProcess.addToMessageQueue(finalMessage);
				} else {
					int bytesAlreadyRead = 0;
					int bytesRead;
					byte[] dataBuffPayload = new byte[dataMessage.getMessageLengthInt() - 1];
					while (bytesAlreadyRead < dataMessage.getMessageLengthInt() - 1) {
						bytesRead = inputStream.read(dataBuffPayload, bytesAlreadyRead,
								dataMessage.getMessageLengthInt() - 1 - bytesAlreadyRead);
						if (bytesRead == -1)
							return;
						bytesAlreadyRead += bytesRead;
					}

					byte[] dataBuffWithPayload = new byte[dataMessage.getMessageLengthInt() + 4];
					System.arraycopy(dataWithoutPayload, 0, dataBuffWithPayload, 0, 4 + 1);
					System.arraycopy(dataBuffPayload, 0, dataBuffWithPayload, 4 + 1, dataBuffPayload.length);

					Message dataMsgWithPayload = Message.populateMessage(dataBuffWithPayload);
					finalMessage.dataMessage = dataMsgWithPayload;
					finalMessage.targetPeerId = remotePeerId;
					PeerProcess.addToMessageQueue(finalMessage);
					dataBuffPayload = null;
					dataBuffWithPayload = null;
					bytesAlreadyRead = 0;
					bytesRead = 0;
				}
			}
		} catch (IOException e) {
			PeerProcess.writeToLogFile(localPeerId + " run exception in remote peer handler " + e);
		}

	}

	public void openClose(InputStream inputStream, Socket socket) {
		try {
			inputStream.close();
			inputStream = socket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public RemotePeerHandler(Socket peerSocket, int connectionType, String ownPeerID) {

		this.peerSocket = peerSocket;
		this.connectionType = connectionType;
		this.localPeerId = ownPeerID;
		try {
			inputStream = peerSocket.getInputStream();
			outputStream = peerSocket.getOutputStream();
		} catch (Exception ex) {
			PeerProcess.writeToLogFile(this.localPeerId + " Error " + ex.getMessage());
		}
	}

	public RemotePeerHandler(String add, int port, int connectionType, String ownPeerID) {
		try {
			this.connectionType = connectionType;
			this.localPeerId = ownPeerID;
			this.peerSocket = new Socket(add, port);
		} catch (UnknownHostException e) {
			PeerProcess.writeToLogFile(ownPeerID + " RemotePeerHandler " + e.getMessage());
		} catch (IOException e) {
			PeerProcess.writeToLogFile(ownPeerID + " RemotePeerHandler " + e.getMessage());
		}
		this.connectionType = connectionType;

		try {
			inputStream = peerSocket.getInputStream();
			outputStream = peerSocket.getOutputStream();
		} catch (Exception ex) {
			PeerProcess.writeToLogFile(ownPeerID + " RemotePeerHandler " + ex.getMessage());
		}
	}

	public boolean sendingHandshake() {
		try {
			outputStream.write(Handshake.packMsg(new Handshake("P2PFILESHARINGPROJ", this.localPeerId)));
		} catch (IOException e) {
			PeerProcess.writeToLogFile(this.localPeerId + " sendingHandshake  " + e.getMessage());
			return false;
		}
		return true;
	}

	public boolean receivePiece() {
		byte[] receivePiece = null;

		try {
			inputStream.read(receivePiece);
		} catch (IOException e) {
			PeerProcess.writeToLogFile(this.localPeerId + " receivePiece : " + e.getMessage());
			return false;
		}

		Message m = Message.populateMessage(receivePiece);
		if (m.getMessageTypeString().equals(MessageType.UNCHOKE)) {
			// RESULT 5:
			PeerProcess.writeToLogFile(localPeerId + " is unchoked by " + remotePeerId + ".");
			return true;
		} else
			return false;

	}

	public void releaseSocket() {
		try {
			if (this.connectionType == 0 && this.peerSocket != null) {
				this.peerSocket.close();
			}
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null)
				outputStream.close();
		} catch (IOException e) {
			PeerProcess.writeToLogFile(localPeerId + " Release socket IO exception: " + e);
		}
	}
}