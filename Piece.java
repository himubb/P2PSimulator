import java.util.Objects;

public class Piece {
	public int hasPiece;
	public String targetPeerId;

	public int pieceIndex;
	public byte[] pieceData;

	public Piece(int hasPiece, String targetPeerId, int pieceIndex, byte[] pieceData) {
		this.hasPiece = hasPiece;
		this.targetPeerId = targetPeerId;
		this.pieceIndex = pieceIndex;
		this.pieceData = pieceData;
	}

	public int getHasPiece() {
		return this.hasPiece;
	}

	public void setHasPiece(int hasPiece) {
		this.hasPiece = hasPiece;
	}

	public String getRemotePeerIdFrom() {
		return this.targetPeerId;
	}

	public void setRemotePeerIdFrom(String targetPeerId) {
		this.targetPeerId = targetPeerId;
	}

	public int getPieceIndex() {
		return this.pieceIndex;
	}

	public void setPieceIndex(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	public byte[] getPieceData() {
		return this.pieceData;
	}

	public void setPieceData(byte[] pieceData) {
		this.pieceData = pieceData;
	}

	public Piece hasPiece(int hasPiece) {
		this.hasPiece = hasPiece;
		return this;
	}

	public Piece targetPeerId(String targetPeerId) {
		this.targetPeerId = targetPeerId;
		return this;
	}

	public Piece pieceIndex(int pieceIndex) {
		this.pieceIndex = pieceIndex;
		return this;
	}

	public Piece pieceData(byte[] pieceData) {
		this.pieceData = pieceData;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Piece)) {
			return false;
		}
		Piece piece = (Piece) o;
		return hasPiece == piece.hasPiece && Objects.equals(targetPeerId, piece.targetPeerId)
				&& pieceIndex == piece.pieceIndex && Objects.equals(pieceData, piece.pieceData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hasPiece, targetPeerId, pieceIndex, pieceData);
	}

	@Override
	public String toString() {
		return "{" + " hasPiece='" + getHasPiece() + "'" + ", targetPeerId='" + getRemotePeerIdFrom() + "'"
				+ ", pieceIndex='" + getPieceIndex() + "'" + ", pieceData='" + getPieceData() + "'" + "}";
	}

	public Piece() {
		targetPeerId = null;
		pieceIndex = -1;
		hasPiece = 0;
		pieceData = new byte[ConfigurationProperties.pieceSize];

	}

	public static Piece decodePiece(byte[] payload) {
		byte[] byteIndex = new byte[4];
		Piece piece = new Piece();
		System.arraycopy(payload, 0, byteIndex, 0, 4);
		piece.pieceIndex = Helper.byteArrayToInt(byteIndex);
		piece.pieceData = new byte[payload.length - 4];
		System.arraycopy(payload, 4, piece.pieceData, 0, payload.length - 4);
		return piece;
	}
}
