
public class Helper {
	public static byte[] intToByteArray(int value) {
		byte[] b = new byte[4];
		for (int counter = 0; counter < 4; counter++) {
			int offset = (b.length - 1 - counter) * 8;
			b[counter] = (byte) ((value >>> offset) & 0xFF);
		}
		return b;
	}

	public static int byteArrayToInt(byte[] b) {
		return byteArrayToInt(b, 0);
	}

	static String byteArrayToHexString(byte inputByteArray[]) {
		byte ch = 0x00;

		int counter = 0;

		if (inputByteArray == null || inputByteArray.length <= 0)
			return null;
		String hexDigits[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

		StringBuffer out = new StringBuffer(inputByteArray.length * 2);

		for (counter = 0; counter < inputByteArray.length; counter++) {
			ch = (byte) (inputByteArray[counter] & 0xF0);
			ch = (byte) (ch >>> 4);
			// shift the bits down

			ch = (byte) (ch & 0x0F);
			// must do this is high order bit is on!

			out.append(hexDigits[(int) ch]);

			ch = (byte) (inputByteArray[counter] & 0x0F);

			out.append(hexDigits[(int) ch]);

		}

		String hex = new String(out);

		return hex;
	}

	public static int byteArrayToInt(byte[] b, int offset) {
		int value = 0;
		for (int counter = 0; counter < 4; counter++) {
			int shift = (3 - counter) * 8;
			value += (b[counter + offset] & 0x000000FF) << shift;
		}
		return value;
	}
}
