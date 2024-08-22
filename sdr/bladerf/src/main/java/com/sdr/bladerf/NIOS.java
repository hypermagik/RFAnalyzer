package com.sdr.bladerf;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

public class NIOS {
    private static final String LOGTAG = "bladeRF-NIOS";
    private final boolean dumpMessages;

    private final UsbDeviceConnection connection;
    private final UsbEndpoint endpointIn;
    private final UsbEndpoint endpointOut;

    private static final int timeout = 1000;

    NIOS(UsbDeviceConnection connection, UsbEndpoint endpointIn, UsbEndpoint endpointOut, boolean dumpMessages) {
        this.connection = connection;
        this.endpointIn = endpointIn;
        this.endpointOut = endpointOut;
        this.dumpMessages = dumpMessages;
    }

    private boolean access(byte[] buffer) {
        return access(buffer, timeout, false);
    }

    private boolean access(byte[] buffer, int timeout, boolean quiet) {
        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS request sending " + Utils.bytesToHex(buffer));
        }

        int status = connection.bulkTransfer(endpointOut, buffer, buffer.length, timeout);
        if (status < 0) {
            if (!quiet) {
                Log.e(LOGTAG, "USB peripheral bulk out transfer failure " + status);
            }
            return false;
        }

        status = connection.bulkTransfer(endpointIn, buffer, buffer.length, timeout);
        if (status < 0) {
            if (!quiet) {
                Log.e(LOGTAG, "USB peripheral bulk in transfer failure " + status);
            }
            return false;
        }

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS request returned " + Utils.bytesToHex(buffer));
        }

        return true;
    }

    public Short read8x16(byte target, byte command) {
        byte[] buffer = new byte[16];

        buffer[Constants.NIOS_PKT_8x16_IDX_MAGIC] = Constants.NIOS_PKT_8x16_MAGIC;
        buffer[Constants.NIOS_PKT_8x16_IDX_TARGET_ID] = target;
        buffer[Constants.NIOS_PKT_8x16_IDX_FLAGS] = 0;
        buffer[Constants.NIOS_PKT_8x16_IDX_ADDR] = command;

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS 8x16 read" +
                    ", target=" + target +
                    ", command=" + command +
                    ", data=" + Utils.bytesToHex(buffer));
        }

        if (!access(buffer)) {
            return null;
        }

        return Utils.getShort(buffer, Constants.NIOS_PKT_8x16_IDX_DATA, 2);
    }

    public boolean write8x16(byte target, byte command, short data) {
        byte[] buffer = new byte[16];

        buffer[Constants.NIOS_PKT_8x16_IDX_MAGIC] = Constants.NIOS_PKT_8x16_MAGIC;
        buffer[Constants.NIOS_PKT_8x16_IDX_TARGET_ID] = target;
        buffer[Constants.NIOS_PKT_8x16_IDX_FLAGS] = 1;
        buffer[Constants.NIOS_PKT_8x16_IDX_ADDR] = command;
        buffer[Constants.NIOS_PKT_8x16_IDX_DATA + 0] = (byte) ((data >> 0) & 0xff);
        buffer[Constants.NIOS_PKT_8x16_IDX_DATA + 1] = (byte) ((data >> 8) & 0xff);

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS 8x16 write" +
                    ", target=" + target +
                    ", command=" + command +
                    ", data=" + Utils.bytesToHex(buffer));
        }

        return access(buffer) && (buffer[Constants.NIOS_PKT_8x16_IDX_FLAGS] & 1) != 0;
    }

    public Integer read8x32(byte target, byte command) {
        byte[] buffer = new byte[16];

        buffer[Constants.NIOS_PKT_8x32_IDX_MAGIC] = Constants.NIOS_PKT_8x32_MAGIC;
        buffer[Constants.NIOS_PKT_8x32_IDX_TARGET_ID] = target;
        buffer[Constants.NIOS_PKT_8x32_IDX_FLAGS] = 0;
        buffer[Constants.NIOS_PKT_8x32_IDX_ADDR] = command;

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS 8x32 read" +
                    ", target=" + target +
                    ", command=" + command +
                    ", data=" + Utils.bytesToHex(buffer));
        }

        if (!access(buffer)) {
            return null;
        }

        return Utils.getInt(buffer, Constants.NIOS_PKT_8x32_IDX_DATA, 4);
    }

    public boolean write8x32(byte target, byte command, int data) {
        byte[] buffer = new byte[16];

        buffer[Constants.NIOS_PKT_8x32_IDX_MAGIC] = Constants.NIOS_PKT_8x32_MAGIC;
        buffer[Constants.NIOS_PKT_8x32_IDX_TARGET_ID] = target;
        buffer[Constants.NIOS_PKT_8x32_IDX_FLAGS] = 1;
        buffer[Constants.NIOS_PKT_8x32_IDX_ADDR] = command;
        buffer[Constants.NIOS_PKT_8x32_IDX_DATA + 0] = (byte) ((data >> 0) & 0xff);
        buffer[Constants.NIOS_PKT_8x32_IDX_DATA + 1] = (byte) ((data >> 8) & 0xff);
        buffer[Constants.NIOS_PKT_8x32_IDX_DATA + 2] = (byte) ((data >> 16) & 0xff);
        buffer[Constants.NIOS_PKT_8x32_IDX_DATA + 3] = (byte) ((data >> 24) & 0xff);

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS 8x32 write" +
                    ", target=" + target +
                    ", command=" + command +
                    ", data=" + Utils.bytesToHex(buffer));
        }

        return access(buffer) && (buffer[Constants.NIOS_PKT_8x32_IDX_FLAGS] & 1) != 0;
    }

    public Long read16x64(byte target, byte command, byte channel) {
        return read16x64(target, command, channel, false);
    }

    public Long read16x64(byte target, byte command, byte channel, boolean fastAndQuiet) {
        byte[] buffer = new byte[16];

        buffer[Constants.NIOS_PKT_16x64_IDX_MAGIC] = Constants.NIOS_PKT_16x64_MAGIC;
        buffer[Constants.NIOS_PKT_16x64_IDX_TARGET_ID] = target;
        buffer[Constants.NIOS_PKT_16x64_IDX_FLAGS] = 0;
        buffer[Constants.NIOS_PKT_16x64_IDX_ADDR + 0] = command;
        buffer[Constants.NIOS_PKT_16x64_IDX_ADDR + 1] = channel;

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS 16x64 read" +
                    ", target=" + target +
                    ", command=" + command +
                    ", channel=" + channel +
                    ", data=" + Utils.bytesToHex(buffer));
        }

        if (!access(buffer, fastAndQuiet ? timeout / 10 : timeout, fastAndQuiet)) {
            return null;
        }

        return Utils.getLong(buffer, Constants.NIOS_PKT_16x64_IDX_DATA, 8);
    }

    public boolean write16x64(byte target, byte command, byte channel, long data) {
        byte[] buffer = new byte[16];

        buffer[Constants.NIOS_PKT_16x64_IDX_MAGIC] = Constants.NIOS_PKT_16x64_MAGIC;
        buffer[Constants.NIOS_PKT_16x64_IDX_TARGET_ID] = target;
        buffer[Constants.NIOS_PKT_16x64_IDX_FLAGS] = 1;
        buffer[Constants.NIOS_PKT_16x64_IDX_ADDR + 0] = command;
        buffer[Constants.NIOS_PKT_16x64_IDX_ADDR + 1] = channel;
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 0] = (byte) ((data >> 0) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 1] = (byte) ((data >> 8) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 2] = (byte) ((data >> 16) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 3] = (byte) ((data >> 24) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 4] = (byte) ((data >> 32) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 5] = (byte) ((data >> 40) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 6] = (byte) ((data >> 48) & 0xff);
        buffer[Constants.NIOS_PKT_16x64_IDX_DATA + 7] = (byte) ((data >> 56) & 0xff);

        if (dumpMessages) {
            Log.d(LOGTAG, "NIOS 16x64 write" +
                    ", target=" + target +
                    ", command=" + command +
                    ", channel=" + channel +
                    ", data=" + Utils.bytesToHex(buffer));
        }

        return access(buffer) && (buffer[Constants.NIOS_PKT_16x64_IDX_FLAGS] & 1) != 0;
    }

    public String getFPGAVersion() {
        Log.i(LOGTAG, "Reading FPGA version");

        final Integer result = read8x32(Constants.NIOS_PKT_8x32_TARGET_VERSION, (byte) 0);
        if (result == null) {
            return null;
        }

        final int major = (result >> 0) & 0xff;
        final int minor = (result >> 8) & 0xff;
        final int patch = (result >> 16) & 0xffff;
        return major + "." + minor + "." + patch;
    }

    public Short getVCTCXOTrim() {
        Log.i(LOGTAG, "Reading VCTCXO trim");
        return read8x16(Constants.NIOS_PKT_8x16_TARGET_AD56X1_DAC, (byte) 0);
    }

    public boolean setVCTCXOTrim(short value) {
        Log.i(LOGTAG, "Setting VCTCXO trim to " + value);
        return write8x16(Constants.NIOS_PKT_8x16_TARGET_AD56X1_DAC, (byte) 0, value);
    }

    public Integer getGPIO() {
        return read8x32(Constants.NIOS_PKT_8x32_TARGET_CONTROL, (byte) 0);
    }

    public Integer getRFFECSR() {
        return read8x32(Constants.NIOS_PKT_8x32_TARGET_RFFE_CSR, (byte) 0);
    }
}
