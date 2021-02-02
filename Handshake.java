
import java.io.*;

// author: piyushC
public class Handshake {

	private byte[] consecutiveZeroes = new byte[10];
	private String messageHeader;
	private String messagePeerID;
	private byte[] header = new byte[18];
	private byte[] peerID = new byte[4];

	public Handshake() {

	}

	public Handshake(String msgHeader, String pId) {

		try {
			this.messageHeader = msgHeader;
			this.header = msgHeader.getBytes("UTF8");
			if (this.header.length > 18)
				throw new Exception("Header exceeds limit length.");

			this.messagePeerID = pId;
			this.peerID = pId.getBytes("UTF8");
			if (this.peerID.length > 18)
				throw new Exception("Peer ID exceeds limit length.");

			this.consecutiveZeroes = "0000000000".getBytes("UTF8");
		} catch (Exception e) {
			PeerProcess.writeToLogFile("Error in Handshake" + e.toString());
		}

	}

	public void setHeader(byte[] handShakeHeader) {
		try {
			this.messageHeader = (new String(handShakeHeader, "UTF8")).toString().trim();
			this.header = this.messageHeader.getBytes();
		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
	}

	public void setPeerID(byte[] peerID) {
		try {
			this.messagePeerID = (new String(peerID, "UTF8")).toString().trim();
			this.peerID = this.messagePeerID.getBytes();

		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
	}

	public void setPeerID(String messagePeerID) {
		try {
			this.messagePeerID = messagePeerID;
			this.peerID = messagePeerID.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
	}

	public void setHeader(String messageHeader) {
		try {
			this.messageHeader = messageHeader;
			this.header = messageHeader.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
	}

	public byte[] getHeader() {
		return header;
	}

	public byte[] getPeerID() {
		return peerID;
	}

	public byte[] getZeroBits() {
		return consecutiveZeroes;
	}

	public String getHeaderString() {
		return messageHeader;
	}

	public String getStringPeerId() {
		return messagePeerID;
	}

	public String toString() {
		return ("Handshake : Peer Id = " + this.messagePeerID + ", Header  =" + this.messageHeader);
	}

	public static Handshake populateMessage(byte[] receivedMessage) {

		Handshake handshakeMessage = null;
		byte[] messageHeader = null;
		byte[] messagePeerID = null;

		try {
			if (receivedMessage.length != 32)
				throw new Exception("Byte array length not matching.");
			messageHeader = new byte[18];
			messagePeerID = new byte[4];
			handshakeMessage = new Handshake();

			System.arraycopy(receivedMessage, 0, messageHeader, 0, 18);
			System.arraycopy(receivedMessage, 28, messagePeerID, 0, 4);

			handshakeMessage.setHeader(messageHeader);
			handshakeMessage.setPeerID(messagePeerID);

		} catch (Exception e) {
			PeerProcess.writeToLogFile(e.toString());
			handshakeMessage = null;
		}
		return handshakeMessage;
	}

	public static byte[] packMsg(Handshake handshakeMessage) {

		byte[] sendMessage = new byte[32];

		try {
			if (handshakeMessage.getHeader() == null) {
				throw new Exception("Wrong Header.");
			}
			if (handshakeMessage.getHeader().length > 18 || handshakeMessage.getHeader().length == 0) {
				throw new Exception("Wrong Header.");
			} else {
				System.arraycopy(handshakeMessage.getHeader(), 0, sendMessage, 0, handshakeMessage.getHeader().length);
			}

			if (handshakeMessage.getZeroBits() == null) {
				throw new Exception("Wrong zero bits field.");
			}
			if (handshakeMessage.getZeroBits().length > 10 || handshakeMessage.getZeroBits().length == 0) {
				throw new Exception("Wrong zero bits field.");
			} else {
				System.arraycopy(handshakeMessage.getZeroBits(), 0, sendMessage, 18, 9);
			}
			if (handshakeMessage.getPeerID() == null) {
				throw new Exception("Wrong peer id.");
			} else if (handshakeMessage.getPeerID().length > 4 || handshakeMessage.getPeerID().length == 0) {
				throw new Exception("Wrong peer id.");
			} else {
				System.arraycopy(handshakeMessage.getPeerID(), 0, sendMessage, 28, handshakeMessage.getPeerID().length);// packing
																														// done
			}

		} catch (Exception e) {
			PeerProcess.writeToLogFile(e.toString());
			sendMessage = null;
		}

		return sendMessage;
	}
}
