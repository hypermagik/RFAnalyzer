package com.sdr.bladerf;

import android.util.Log;

public class RFIC {
    private static final String LOGTAG = "bladeRF-RFIC";

    private final NIOS nios;

    RFIC(NIOS nios) {
        this.nios = nios;
    }

    private Long read(byte command, byte channel) {
        return nios.read16x64(Constants.NIOS_PKT_16x64_TARGET_RFIC, command, channel);
    }

    private int getWriteQueueLength() {
        final Long result = nios.read16x64(Constants.NIOS_PKT_16x64_TARGET_RFIC, Constants.RFIC_CMD_STATUS, Constants.CHANNEL_INVALID, true);
        if (result == null) {
            return -1;
        }
        final boolean initialized = (result & 1) == 1;
        if (!initialized) {
            return 255;
        }
        return (int) ((result >> 8) & 0xff);
    }

    private boolean write(byte command, byte channel, long value) {
        final boolean result = nios.write16x64(Constants.NIOS_PKT_16x64_TARGET_RFIC, command, channel, value);

        int tries = 50;
        final int delay = 100000;

        while (tries-- > 0 && getWriteQueueLength() != 0) {
            try {
                Thread.sleep(0, delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    public boolean open() {
        Log.i(LOGTAG, "Opening RFIC");
        return write(Constants.RFIC_CMD_INIT, Constants.CHANNEL_INVALID, Constants.RFIC_STATE_ON);
    }

    public boolean close() {
        Log.i(LOGTAG, "Closing RFIC");
        return write(Constants.RFIC_CMD_INIT, Constants.CHANNEL_INVALID, Constants.RFIC_STATE_OFF);
    }

    public boolean enable(int state) {
        Log.i(LOGTAG, state == 1 ? "Enabling RFIC Rx" : "Disabling RFIC Rx");
        return write(Constants.RFIC_CMD_ENABLE, Constants.CHANNEL_RX0, state);
    }

    public long getSampleRate() {
        Log.i(LOGTAG, "Reading RFIC sample rate");
        return read(Constants.RFIC_CMD_SAMPLERATE, Constants.CHANNEL_RX0);
    }

    public boolean setSampleRate(long sampleRate) {
        Log.i(LOGTAG, "Setting RFIC sample rate to " + sampleRate);
        if (write(Constants.RFIC_CMD_SAMPLERATE, Constants.CHANNEL_RX0, sampleRate)) {
            Log.i(LOGTAG, "RFIC sample rate set to " + getSampleRate());
            return true;
        }
        return false;
    }

    public long getBandwidth() {
        Log.i(LOGTAG, "Reading RFIC bandwidth");
        return read(Constants.RFIC_CMD_BANDWIDTH, Constants.CHANNEL_RX0);
    }

    public boolean setBandwidth(long bandwidth) {
        Log.i(LOGTAG, "Setting RFIC bandwidth to " + bandwidth);
        return write(Constants.RFIC_CMD_BANDWIDTH, Constants.CHANNEL_RX0, bandwidth);
    }

    public long getGainMode() {
        Log.i(LOGTAG, "Reading RFIC gain mode");
        return read(Constants.RFIC_CMD_GAINMODE, Constants.CHANNEL_RX0);
    }

    public boolean setGainMode(long mode) {
        Log.i(LOGTAG, "Setting RFIC gain mode to " + mode);
        return write(Constants.RFIC_CMD_GAINMODE, Constants.CHANNEL_RX0, mode);
    }

    public int getGain() {
        Log.i(LOGTAG, "Reading RFIC gain");
        return read(Constants.RFIC_CMD_GAIN, Constants.CHANNEL_RX0).intValue();
    }

    public boolean setGain(int gain) {
        Log.i(LOGTAG, "Setting RFIC gain to " + gain);
        return write(Constants.RFIC_CMD_GAIN, Constants.CHANNEL_RX0, gain);
    }

    public long getFrequency() {
        Log.i(LOGTAG, "Reading RFIC frequency");
        return read(Constants.RFIC_CMD_FREQUENCY, Constants.CHANNEL_RX0);
    }

    public boolean setFrequency(long frequency, boolean quiet) {
        if (!quiet) {
            Log.i(LOGTAG, "Setting RFIC frequency to " + frequency);
        }
        return write(Constants.RFIC_CMD_FREQUENCY, Constants.CHANNEL_RX0, frequency);
    }

    public long getRxFilter() {
        Log.i(LOGTAG, "Reading RFIC Rx filter");
        return read(Constants.RFIC_CMD_FILTER, Constants.CHANNEL_RX0);
    }

    public boolean setRxFilter(long filter) {
        Log.i(LOGTAG, "Setting RFIC Rx filter to " + filter);
        return write(Constants.RFIC_CMD_FILTER, Constants.CHANNEL_RX0, filter);
    }

    public boolean setTxFilter(long filter) {
        Log.i(LOGTAG, "Setting RFIC Tx filter to " + filter);
        return write(Constants.RFIC_CMD_FILTER, Constants.CHANNEL_TX0, filter);
    }

    public void setTxMute() {
        Log.i(LOGTAG, "Setting RFIC Tx mute");
        write(Constants.RFIC_CMD_TXMUTE, Constants.CHANNEL_TX0, 1);
        write(Constants.RFIC_CMD_TXMUTE, Constants.CHANNEL_TX1, 1);
    }
}
