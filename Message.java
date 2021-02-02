
import java.io.UnsupportedEncodingException;

public class Message {
	private String messageType;
	private String messageLength;
	private int dataLength = 1;
	private byte[] type = null;
	private byte[] len = null;
	private byte[] payload = null;

	public Message(String msgType) {

		try {

			if (msgType == MessageType.CHOKE || msgType == MessageType.UNCHOKE || msgType == MessageType.INTERESTED
					|| msgType == MessageType.NOTINTERESTED) {
				this.setMessageLength(1);
				this.setMessageType(msgType);
				this.payload = null;
			} else
				throw new Exception("Wrong selection");

		} catch (Exception e) {
			PeerProcess.writeToLogFile(e.toString());
		}

	}

	public Message(String msgType, byte[] payload) {

		try {
			if (payload != null) {

				this.setMessageLength(payload.length + 1);
				if (this.len.length > 4)
					throw new Exception("Msg length exceeds limit length.");

				this.setPayload(payload);

			} else {
				if (msgType == MessageType.CHOKE || msgType == MessageType.UNCHOKE || msgType == MessageType.INTERESTED
						|| msgType == MessageType.NOTINTERESTED) {
					this.setMessageLength(1);
					this.payload = null;
				} else
					throw new Exception("Payload should not be null");

			}

			this.setMessageType(msgType);
			if (this.getMessageType().length > 1)
				throw new Exception("MessageType length exceeds limit length.");

		} catch (Exception e) {
			PeerProcess.writeToLogFile(e.toString());
		}

	}

	public Message() {

	}

	public void setMessageLength(int messageLength) {
		this.dataLength = messageLength;
		this.messageLength = ((Integer) messageLength).toString();
		this.len = Helper.intToByteArray(messageLength);
	}

	public void setMessageLength(byte[] len) {

		Integer l = Helper.byteArrayToInt(len);
		this.messageLength = l.toString();
		this.len = len;
		this.dataLength = l;
	}

	public byte[] getMessageLength() {
		return len;
	}

	public String getMessageLengthString() {
		return messageLength;
	}

	public int getMessageLengthInt() {
		return this.dataLength;
	}

	public void setMessageType(byte[] type) {
		try {
			this.messageType = new String(type, "UTF8");
			this.type = type;
		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
	}

	public void setMessageType(String messageType) {
		try {
			this.messageType = messageType.trim();
			this.type = this.messageType.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
	}

	public byte[] getMessageType() {
		return type;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getMessageTypeString() {
		return messageType;
	}

	public String toString() {
		String string = null;
		try {
			string = "[Message] : Message Length - " + this.messageLength + ", Message msgType - " + this.messageType
					+ ", Data - " + (new String(this.payload, "UTF8")).toString().trim();
		} catch (UnsupportedEncodingException e) {
			PeerProcess.writeToLogFile(e.toString());
		}
		return string;
	}
	// encodes the object Message to a byte array

	public static byte[] packMsg(Message msg) {
		byte[] msgStream = null;
		int msgType;

		try {

			msgType = Integer.parseInt(msg.getMessageTypeString());
			if (msg.getMessageLength().length > 4)
				throw new Exception("Wrong message length.");
			else if (msgType < 0 || msgType > 7)
				throw new Exception("Wrong message type.");
			else if (msg.getMessageType() == null)
				throw new Exception("Wrong message type.");
			else if (msg.getMessageLength() == null)
				throw new Exception("Wrong message length.");

			if (msg.getPayload() != null) {
				msgStream = new byte[4 + 1 + msg.getPayload().length];

				System.arraycopy(msg.getMessageLength(), 0, msgStream, 0, msg.getMessageLength().length);
				System.arraycopy(msg.getMessageType(), 0, msgStream, 4, 1);
				System.arraycopy(msg.getPayload(), 0, msgStream, 4 + 1, msg.getPayload().length);

			} else {
				msgStream = new byte[5];

				System.arraycopy(msg.getMessageLength(), 0, msgStream, 0, msg.getMessageLength().length);
				System.arraycopy(msg.getMessageType(), 0, msgStream, 4, 1);

			}

		} catch (Exception e) {
			PeerProcess.writeToLogFile(e.toString());
			msgStream = null;
		}

		return msgStream;
	}

	// decodes the byte array and send it to object Message

	public static Message populateMessage(byte[] Message) {

		Message msg = new Message();
		byte[] msgLength = new byte[4];
		byte[] msgType = new byte[1];
		byte[] payLoad = null;
		int len;

		try {

			if (Message == null)
				throw new Exception("Wrong data.");
			else if (Message.length < 4 + 1)
				throw new Exception("Byte array length is too small...");

			System.arraycopy(Message, 0, msgLength, 0, 4);
			System.arraycopy(Message, 4, msgType, 0, 1);

			msg.setMessageLength(msgLength);
			msg.setMessageType(msgType);

			len = Helper.byteArrayToInt(msgLength);

			if (len > 1) {
				payLoad = new byte[len - 1];
				System.arraycopy(Message, 4 + 1, payLoad, 0, Message.length - 4 - 1);
				msg.setPayload(payLoad);
			}

			payLoad = null;
		} catch (Exception e) {
			PeerProcess.writeToLogFile(e.toString());
			msg = null;
		}
		return msg;
	}

}
