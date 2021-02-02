import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;

public class MessagePayload {
	public Piece[] pieces;
	public int size;

	public MessagePayload() {
		size = (int) Math
				.ceil(((double) ConfigurationProperties.fileSize / (double) ConfigurationProperties.pieceSize));
		this.pieces = new Piece[size];

		for (int counter = 0; counter < this.size; counter++)
			this.pieces[counter] = new Piece();

	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Piece[] getPieces() {
		return pieces;
	}

	public void setPieces(Piece[] pieces) {
		this.pieces = pieces;
	}

	public byte[] encode() {
		return this.getBytes();
	}

	public static MessagePayload unpackMsg(byte[] b) {
		MessagePayload returnMessagePayload = new MessagePayload();
		for (int counter = 0; counter < b.length; counter++) {
			int count = 7;
			while (count >= 0) {
				int test = 1 << count;
				if (counter * 8 + (8 - count - 1) < returnMessagePayload.size) {
					if ((b[counter] & (test)) != 0)
						returnMessagePayload.pieces[counter * 8 + (8 - count - 1)].hasPiece = 1;
					else
						returnMessagePayload.pieces[counter * 8 + (8 - count - 1)].hasPiece = 0;
				}
				count--;
			}
		}

		return returnMessagePayload;
	}

	public synchronized boolean compare(MessagePayload yourMessagePayload) {
		int yourSize = yourMessagePayload.getSize();

		for (int counter = 0; counter < yourSize; counter++) {
			if (yourMessagePayload.getPieces()[counter].getHasPiece() == 1
					&& this.getPieces()[counter].getHasPiece() == 0) {
				return true;
			} else
				continue;
		}

		return false;
	}

	public synchronized int returnFirstDiff(MessagePayload yourMessagePayload) {
		int mySize = this.getSize();
		int yourSize = yourMessagePayload.getSize();

		if (mySize >= yourSize) {
			for (int counter = 0; counter < yourSize; counter++) {
				if (yourMessagePayload.getPieces()[counter].getHasPiece() == 1
						&& this.getPieces()[counter].getHasPiece() == 0) {
					return counter;
				}
			}
		} else {
			for (int counter = 0; counter < mySize; counter++) {
				if (yourMessagePayload.getPieces()[counter].getHasPiece() == 1
						&& this.getPieces()[counter].getHasPiece() == 0) {
					return counter;
				}
			}
		}

		return -1;
	}

	public byte[] getBytes() {
		int s = this.size / 8;
		if (size % 8 != 0)
			s = s + 1;
		byte[] iP = new byte[s];
		int tempInt = 0;
		int count = 0;
		int Cnt;
		for (Cnt = 1; Cnt <= this.size; Cnt++) {
			int tempP = this.pieces[Cnt - 1].hasPiece;
			tempInt = tempInt << 1;
			if (tempP == 1) {
				tempInt = tempInt + 1;
			} else
				tempInt = tempInt + 0;

			if (Cnt % 8 == 0 && Cnt != 0) {
				iP[count] = (byte) tempInt;
				count++;
				tempInt = 0;
			}

		}
		if ((Cnt - 1) % 8 != 0) {
			int tempShift = ((size) - (size / 8) * 8);
			tempInt = tempInt << (8 - tempShift);
			iP[count] = (byte) tempInt;
		}
		return iP;
	}

	static String byteArrayToHexString(byte inputByteArray[]) {
		byte ch = 0x00;

		int counter = 0;

		if (inputByteArray == null || inputByteArray.length <= 0)
			return null;
		String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

		StringBuffer out = new StringBuffer(inputByteArray.length * 2);

		while (counter < inputByteArray.length) {
			ch = (byte) (inputByteArray[counter] & 0xF0);
			ch = (byte) (ch >>> 4);

			ch = (byte) (ch & 0x0F);

			out.append(pseudo[(int) ch]);

			ch = (byte) (inputByteArray[counter] & 0x0F);

			out.append(pseudo[(int) ch]);
			counter++;
		}

		String hex = new String(out);

		return hex;
	}

	public void initOwnBitfield(String myPeerId, int hasFile) {

		if (hasFile == 1) {
			for (int counter = 0; counter < this.size; counter++) {
				this.pieces[counter].setHasPiece(1);
				this.pieces[counter].setRemotePeerIdFrom(myPeerId);
				// If the file not exists

			}
		} else {

			for (int counter = 0; counter < this.size; counter++) {
				this.pieces[counter].setHasPiece(0);
				this.pieces[counter].setRemotePeerIdFrom(myPeerId);
			}

		}

	}

	public synchronized void updateMessagePayload(String peerId, Piece piece) {
		try {
			if (PeerProcess.localMessagePayload.pieces[piece.pieceIndex].hasPiece == 1) {
				PeerProcess.writeToLogFile(peerId + " has the piece already.");
			} else {
				String fileName = ConfigurationProperties.fileName;
				File file = new File(PeerProcess.peerID, fileName);
				int off = piece.pieceIndex * ConfigurationProperties.pieceSize;
				RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
				byte[] byteWrite;
				byteWrite = piece.pieceData;

				randomAccessFile.seek(off);
				randomAccessFile.write(byteWrite);

				this.pieces[piece.pieceIndex].setHasPiece(1);
				this.pieces[piece.pieceIndex].setRemotePeerIdFrom(peerId);
				randomAccessFile.close();

				PeerProcess.writeToLogFile(PeerProcess.peerID + " has downloaded the piece " + piece.pieceIndex
						+ " from " + peerId + ". Now the number of pieces it has is "
						+ PeerProcess.localMessagePayload.ownPieces());

				if (PeerProcess.localMessagePayload.isCompleted()) {
					PeerProcess.peerInfoTable.get(PeerProcess.peerID).isInterested = 0;
					PeerProcess.peerInfoTable.get(PeerProcess.peerID).isCompleted = 1;
					PeerProcess.peerInfoTable.get(PeerProcess.peerID).isChoked = 0;
					updatePeerInfo(PeerProcess.peerID, 1);

					PeerProcess.writeToLogFile(PeerProcess.peerID + " has downloaded the complete file.");
				}
			}

		} catch (Exception e) {
			PeerProcess.writeToLogFile(PeerProcess.peerID + " ERROR in updating bitfield " + e.getMessage());
		}

	}

	public int ownPieces() {
		int count = 0;
		for (int counter = 0; counter < this.size; counter++)
			if (this.pieces[counter].hasPiece == 1)
				count++;

		return count;
	}

	public boolean isCompleted() {

		for (int counter = 0; counter < this.size; counter++) {
			if (this.pieces[counter].hasPiece == 0) {
				return false;
			}
		}
		return true;
	}

	public void updatePeerInfo(String clientID, int hasFile) {
		BufferedWriter out = null;
		BufferedReader in = null;

		try {
			in = new BufferedReader(new FileReader("PeerInfo.cfg"));

			String line;
			StringBuffer buffer = new StringBuffer();

			while ((line = in.readLine()) != null) {
				if (line.trim().split("\\s+")[0].equals(clientID)) {
					buffer.append(line.trim().split("\\s+")[0] + " " + line.trim().split("\\s+")[1] + " "
							+ line.trim().split("\\s+")[2] + " " + hasFile);
				} else {
					buffer.append(line);

				}
				buffer.append("\n");
			}

			in.close();

			out = new BufferedWriter(new FileWriter("PeerInfo.cfg"));
			out.write(buffer.toString());

			out.close();
		} catch (Exception e) {
			PeerProcess.writeToLogFile(clientID + " Error in updating the PeerInfo.cfg " + e.getMessage());
		}
	}

}
