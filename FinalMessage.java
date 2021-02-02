
public class FinalMessage {
	Message dataMessage;
	String targetPeerId;

	public FinalMessage() {
		dataMessage = new Message();
		targetPeerId = null;
	}

	public void setFromPeerID(String targetPeerId) {
		this.targetPeerId = targetPeerId;
	}

	public void setDataMsg(Message dataMessage) {
		this.dataMessage = dataMessage;
	}

	public Message getDataMsg() {
		return dataMessage;
	}

	public String getFromPeerID() {
		return targetPeerId;
	}

}
