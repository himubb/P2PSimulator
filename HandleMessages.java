import java.util.Date;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;

public class HandleMessages implements Runnable {
	RandomAccessFile randomAccessFile;

	private int writeToOutputStream(Socket socket, byte[] encodedMessagePayload) {
		try {
			OutputStream outputStream = socket.getOutputStream();
			outputStream.write(encodedMessagePayload);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public void getStatus(String dataType, int state) {
		PeerProcess.writeToLogFile("msgType = " + dataType + " State = " + state);
	}

	public void run() {
		Message message;
		FinalMessage dataWrapper;
		String msgType;
		String remotePeerId;

		while (true) {
			dataWrapper = PeerProcess.removeFromMessageQueue();
			while (dataWrapper == null) {
				Thread.currentThread();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				dataWrapper = PeerProcess.removeFromMessageQueue();
			}

			message = dataWrapper.getDataMsg();

			msgType = message.getMessageTypeString();
			remotePeerId = dataWrapper.getFromPeerID();
			int state = PeerProcess.peerInfoTable.get(remotePeerId).state;

			if (msgType.equals(MessageType.HAVE) && state != 14) {
				// RESULT 7: Receive Message from peer
				// MessagePayload.unpackMsg(message.getPayload());

				int index = PeerProcess.localMessagePayload
						.returnFirstDiff(PeerProcess.peerInfoTable.get(remotePeerId).bitField);
				if (index > 0)
					PeerProcess.writeToLogFile(PeerProcess.peerID + " received the 'have' message from "
							+ remotePeerId + " for the piece " + index + ".");
				if (isInterested(message, remotePeerId)) {

					sendInterested(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
					PeerProcess.peerInfoTable.get(remotePeerId).state = 9;
				} else {

					sendNotInterested(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
					PeerProcess.peerInfoTable.get(remotePeerId).state = 13;
				}
			} else {
				switch (state) {

				case 2:
					if (msgType.equals(MessageType.BITFIELD)) {
						PeerProcess.writeToLogFile(
								PeerProcess.peerID + " received the 'bitfield' message from " + remotePeerId);
						sendMessagePayload(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
						PeerProcess.peerInfoTable.get(remotePeerId).state = 3;
					}
					break;

				case 3:
					if (msgType.equals(MessageType.NOTINTERESTED)) {
						// RESULT 9:
						PeerProcess.writeToLogFile(PeerProcess.peerID + " received the 'not interested' message from "
								+ remotePeerId + ".");
						PeerProcess.peerInfoTable.get(remotePeerId).isInterested = 0;
						PeerProcess.peerInfoTable.get(remotePeerId).state = 5;
						PeerProcess.peerInfoTable.get(remotePeerId).isHandShaked = 1;
					} else if (msgType.equals(MessageType.INTERESTED)) {
						// RESULT 8:
						PeerProcess.writeToLogFile(
								PeerProcess.peerID + " received the 'interested' message from  " + remotePeerId + ".");
						PeerProcess.peerInfoTable.get(remotePeerId).isInterested = 1;
						PeerProcess.peerInfoTable.get(remotePeerId).isHandShaked = 1;

						if (!PeerProcess.preferredNeighborsTable.containsKey(remotePeerId)
								&& !PeerProcess.unchokedNeighborsTable.containsKey(remotePeerId)) {
							chokingPeers(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).isChoked = 1;
							PeerProcess.peerInfoTable.get(remotePeerId).state = 6;
						} else {
							PeerProcess.peerInfoTable.get(remotePeerId).isChoked = 0;
							unchokingPeers(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).state = 4;
						}
					}
					break;

				case 4:
					if (msgType.equals(MessageType.REQUEST)) {
						// PeerProcess.writeToLogFile(PeerProcess.peerID + " received the 'request' message
						// from " + remotePeerId);
						sendPiece(PeerProcess.socketTable.get(remotePeerId), message, remotePeerId);
						if (!PeerProcess.preferredNeighborsTable.containsKey(remotePeerId)
								&& !PeerProcess.unchokedNeighborsTable.containsKey(remotePeerId)) {
							chokingPeers(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).isChoked = 1;
							PeerProcess.peerInfoTable.get(remotePeerId).state = 6;
						}
					}
					break;

				case 8:
					if (msgType.equals(MessageType.BITFIELD)) {
						if (isInterested(message, remotePeerId)) {
							// PeerProcess.writeToLogFile(PeerProcess.peerID + " is interested in " +
							// remotePeerId);
							sendInterested(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).state = 9;
						} else {
							// PeerProcess.writeToLogFile(PeerProcess.peerID + " is not interested in "
							// +
							// remotePeerId);
							sendNotInterested(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).state = 13;
						}
					}
					break;

				case 9:
					if (msgType.equals(MessageType.CHOKE)) {
						// RESULT 6:
						PeerProcess.writeToLogFile(PeerProcess.peerID + " is choked by " + remotePeerId + ".");
						PeerProcess.peerInfoTable.get(remotePeerId).state = 14;
					} else if (msgType.equals(MessageType.UNCHOKE)) {
						// RESULT 5:
						PeerProcess.writeToLogFile(PeerProcess.peerID + " is unchoked by " + remotePeerId + ".");
						int changePresent = PeerProcess.localMessagePayload
								.returnFirstDiff(PeerProcess.peerInfoTable.get(remotePeerId).bitField);
						if (changePresent == -1) {
							PeerProcess.peerInfoTable.get(remotePeerId).state = 13;

						} else
							sendRequest(PeerProcess.socketTable.get(remotePeerId), changePresent, remotePeerId);
						PeerProcess.writeToLogFile(PeerProcess.peerID + " 'requests' for piece " + changePresent
								+ " from " + remotePeerId);

						PeerProcess.peerInfoTable.get(remotePeerId).state = 11;
						PeerProcess.peerInfoTable.get(remotePeerId).startTime = new Date();
					}
					break;

				case 11:
					if (msgType.equals(MessageType.PIECE)) {
						byte[] buffer = message.getPayload();
						PeerProcess.peerInfoTable.get(remotePeerId).finishTime = new Date();
						long timeLapse = PeerProcess.peerInfoTable.get(remotePeerId).finishTime.getTime()
								- PeerProcess.peerInfoTable.get(remotePeerId).startTime.getTime();

						PeerProcess.peerInfoTable.get(
								remotePeerId).dataRate = ((double) (buffer.length + 4 + 1) / (double) timeLapse) * 100;

						Piece p = Piece.decodePiece(buffer);
						PeerProcess.localMessagePayload.updateMessagePayload(remotePeerId, p);

						int toGetPieceIndex = PeerProcess.localMessagePayload
								.returnFirstDiff(PeerProcess.peerInfoTable.get(remotePeerId).bitField);
						if (toGetPieceIndex != -1) {

							sendRequest(PeerProcess.socketTable.get(remotePeerId), toGetPieceIndex, remotePeerId);
							PeerProcess.writeToLogFile(PeerProcess.peerID + " 'requests' for piece " + toGetPieceIndex
									+ " from " + remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).state = 11;
							PeerProcess.peerInfoTable.get(remotePeerId).startTime = new Date();
						} else
							PeerProcess.peerInfoTable.get(remotePeerId).state = 13;

						PeerProcess.updatePeerInfo();

						Enumeration<String> keys = PeerProcess.peerInfoTable.keys();
						while (keys.hasMoreElements()) {
							String key = (String) keys.nextElement();
							RemotePeerInfo pref = PeerProcess.peerInfoTable.get(key);

							if (key.equals(PeerProcess.peerID))
								continue;

							if (pref.isCompleted == 0 && pref.isChoked == 0 && pref.isHandShaked == 1) {

								sendHave(PeerProcess.socketTable.get(key), key);
								PeerProcess.peerInfoTable.get(key).state = 3;

							}

						}

						buffer = null;
						message = null;

					} else if (msgType.equals(MessageType.CHOKE)) {
						// RESULT 6:
						PeerProcess.writeToLogFile(PeerProcess.peerID + " is choked by " + remotePeerId + ".");
						PeerProcess.peerInfoTable.get(remotePeerId).state = 14;
					}
					break;

				case 14:
					if (msgType.equals(MessageType.HAVE)) {
						if (isInterested(message, remotePeerId)) {

							sendInterested(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).state = 9;
						} else {

							sendNotInterested(PeerProcess.socketTable.get(remotePeerId), remotePeerId);
							PeerProcess.peerInfoTable.get(remotePeerId).state = 13;
						}
					} else if (msgType.equals(MessageType.UNCHOKE)) {
						// RESULT 5:
						PeerProcess.writeToLogFile(PeerProcess.peerID + " is unchoked by " + remotePeerId);
						PeerProcess.peerInfoTable.get(remotePeerId).state = 14;
					}
					break;

				}
			}

		}
	}

	private byte[] intTobyte(int counter) {
		return new byte[] { (byte) ((counter >> 24) & 0xFF), (byte) ((counter >> 16) & 0xFF),
				(byte) ((counter >> 8) & 0xFF), (byte) (counter & 0xFF) };
	}

	private int byteToint(byte[] b1) {
		return b1[3] & 0xFF | (b1[2] & 0xFF) << 8 | (b1[1] & 0xFF) << 16 | (b1[0] & 0xFF) << 24;
	}

	private void sendRequest(Socket socket, int pieceNo, String presentPeerId) {

		// Byte2int....
		byte[] pieceByte = new byte[4];
		for (int counter = 0; counter < 4; counter++) {
			pieceByte[counter] = 0;
		}

		byte[] pieceIndexByte = Helper.intToByteArray(pieceNo);
		System.arraycopy(pieceIndexByte, 0, pieceByte, 0, pieceIndexByte.length);
		Message message = new Message(MessageType.REQUEST, pieceByte);
		byte[] b = Message.packMsg(message);
		writeToOutputStream(socket, b);

		pieceByte = null;
		pieceIndexByte = null;
		b = null;
		message = null;
	}

	private void sendPiece(Socket socket, Message message, String presentPeerId) // message == requestmessage
	{
		byte[] bytePieceIndex = message.getPayload();
		int pieceIndex = Helper.byteArrayToInt(bytePieceIndex);

		PeerProcess.writeToLogFile(PeerProcess.peerID + " is sending the 'piece' message for piece " + pieceIndex
				+ " to " + presentPeerId);

		byte[] byteRead = new byte[ConfigurationProperties.pieceSize];
		int noBytesRead = 0;

		File file = new File(PeerProcess.peerID, ConfigurationProperties.fileName);
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
			randomAccessFile.seek(pieceIndex * ConfigurationProperties.pieceSize);
			noBytesRead = randomAccessFile.read(byteRead, 0, ConfigurationProperties.pieceSize);
		} catch (IOException e) {
			PeerProcess.writeToLogFile(PeerProcess.peerID + " ERROR in reading the file : " + e.toString());
		}
		if (noBytesRead == 0) {
			PeerProcess.writeToLogFile(PeerProcess.peerID + " ERROR :  Zero bytes read from the file !");
		} else if (noBytesRead < 0) {
			PeerProcess.writeToLogFile(PeerProcess.peerID + " ERROR : File could not be read properly.");
		}

		byte[] buffer = new byte[noBytesRead + 4];
		System.arraycopy(bytePieceIndex, 0, buffer, 0, 4);
		System.arraycopy(byteRead, 0, buffer, 4, noBytesRead);

		Message sendMessage = new Message(MessageType.PIECE, buffer);
		byte[] b = Message.packMsg(sendMessage);
		writeToOutputStream(socket, b);

		buffer = null;
		byteRead = null;
		b = null;
		bytePieceIndex = null;
		sendMessage = null;

		try {
			randomAccessFile.close();
		} catch (Exception e) {
		}
	}

	private void sendNotInterested(Socket socket, String presentPeerId) {
		PeerProcess
				.writeToLogFile(PeerProcess.peerID + " is sending the 'not interested' message to  " + presentPeerId);
		Message message = new Message(MessageType.NOTINTERESTED);
		byte[] msgByte = Message.packMsg(message);
		writeToOutputStream(socket, msgByte);
	}

	private void sendInterested(Socket socket, String presentPeerId) {
		PeerProcess.writeToLogFile(PeerProcess.peerID + " is sending the 'interested' message to " + presentPeerId);
		Message message = new Message(MessageType.INTERESTED);
		byte[] msgByte = Message.packMsg(message);
		writeToOutputStream(socket, msgByte);
	}

	private boolean isInterested(Message message, String remotePeerId) {

		MessagePayload b = MessagePayload.unpackMsg(message.getPayload());
		PeerProcess.peerInfoTable.get(remotePeerId).bitField = b;

		if (PeerProcess.localMessagePayload.compare(b))
			return true;
		return false;
	}

	private void unchokingPeers(Socket socket, String presentPeerId) {

		PeerProcess.writeToLogFile(PeerProcess.peerID + " is sending the 'unchoke' message to " + presentPeerId);
		Message message = new Message(MessageType.UNCHOKE);
		byte[] msgByte = Message.packMsg(message);
		writeToOutputStream(socket, msgByte);
	}

	private void chokingPeers(Socket socket, String presentPeerId) {
		PeerProcess.writeToLogFile(PeerProcess.peerID + " is sending the 'choke' message to " + presentPeerId);
		Message message = new Message(MessageType.CHOKE);
		byte[] msgByte = Message.packMsg(message);
		writeToOutputStream(socket, msgByte);
	}

	private void sendMessagePayload(Socket socket, String presentPeerId) {

		PeerProcess.writeToLogFile(PeerProcess.peerID + " is sending the 'bitfield' message to " + presentPeerId);
		byte[] encodedMessagePayload = PeerProcess.localMessagePayload.encode();

		Message message = new Message(MessageType.BITFIELD, encodedMessagePayload);
		writeToOutputStream(socket, Message.packMsg(message));

		encodedMessagePayload = null;
	}

	private void sendHave(Socket socket, String presentPeerId) {

		PeerProcess.writeToLogFile(PeerProcess.peerID + " is sending the 'have' message to " + presentPeerId);
		byte[] encodedMessagePayload = PeerProcess.localMessagePayload.encode();
		Message message = new Message(MessageType.HAVE, encodedMessagePayload);
		writeToOutputStream(socket, Message.packMsg(message));

		encodedMessagePayload = null;
	}

	public static String print(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		for (byte b : bytes) {
			sb.append(String.format("0x%02X ", b));
		}
		sb.append("]");
		return sb.toString();
	}

	private static String currentPeerId = null;

	public HandleMessages() {
		currentPeerId = null;
	}

	public HandleMessages(String currentPeerId2) {
		currentPeerId = currentPeerId2;
	}
}
