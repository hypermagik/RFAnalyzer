package com.sdr.bladerf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Utils {
    public static short getShort(byte[] buffer, int offset, int length) {
        return ByteBuffer.wrap(buffer, offset, length).order(ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static int getInt(byte[] buffer) {
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static int getInt(byte[] buffer, int offset, int length) {
        return ByteBuffer.wrap(buffer, offset, length).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static long getLong(byte[] buffer, int offset, int length) {
        return ByteBuffer.wrap(buffer, offset, length).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes();

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int v = bytes[i] & 0xFF;
            hexChars[i * 2 + 0] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0xf];
        }
        return new String(hexChars);
    }
}
