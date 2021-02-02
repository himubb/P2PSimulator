import java.util.Date;

public class RemotePeerInfo implements Comparable<RemotePeerInfo> {
	public String peerId;
	public String peerAddress;
	public String peerPort;
	public int isFirstPeer;
	public double dataRate = 0;
	public int isInterested = 1;
	public int isPreferredNeighbor = 0;
	public int isOptUnchokedNeighbor = 0;
	public int isChoked = 1;
	public MessagePayload bitField;
	public int state = -1;
	public int peerIndex;
	public int isCompleted = 0;
	public int isHandShaked = 0;
	public Date startTime;
	public Date finishTime;

	public RemotePeerInfo(String pId, String pAddress, String pPort, int pIndex) {
		peerId = pId;
		peerAddress = pAddress;
		peerPort = pPort;
		bitField = new MessagePayload();
		peerIndex = pIndex;
	}

	public RemotePeerInfo(String pId, String pAddress, String pPort, int pIsFirstPeer, int pIndex) {
		peerId = pId;
		peerAddress = pAddress;
		peerPort = pPort;
		isFirstPeer = pIsFirstPeer;
		bitField = new MessagePayload();
		peerIndex = pIndex;
	}

	public String getPeerId() {
		return peerId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}

	public String getPeerAddress() {
		return peerAddress;
	}

	public void setPeerAddress(String peerAddress) {
		this.peerAddress = peerAddress;
	}

	public String getPeerPort() {
		return peerPort;
	}

	public void setPeerPort(String peerPort) {
		this.peerPort = peerPort;
	}

	public int getIsFirstPeer() {
		return isFirstPeer;
	}

	public void setIsFirstPeer(int isFirstPeer) {
		this.isFirstPeer = isFirstPeer;
	}

	public int compareTo(RemotePeerInfo o1) {

		if (this.dataRate > o1.dataRate)
			return 1;
		else if (this.dataRate == o1.dataRate)
			return 0;
		else
			return -1;
	}

}
