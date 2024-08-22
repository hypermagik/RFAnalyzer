package com.mantz_it.rfanalyzer;

import android.content.Context;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.sdr.bladerf.Device;

public class BladeRFSource implements IQSourceInterface, Runnable {
    private static final String LOGTAG = "bladeRF-Source";

    private int sampleRate = 0;
    private long frequency = 0;
    private boolean agc = true;
    private int gain = -1000;

    private final IQConverter iqConverter = new Signed12BitIQConverter();

    private ArrayBlockingQueue<byte[]> queue = null;
    private ArrayBlockingQueue<byte[]> pool = null;

    private static final int queueSize = 1024;

    private Device device = null;
    private Thread usbThread = null;

    private boolean isSampling = false;

    @Override
    public boolean open(Context context, com.mantz_it.rfanalyzer.IQSourceInterface.Callback callback) {
        queue = new ArrayBlockingQueue<>(queueSize);
        pool = new ArrayBlockingQueue<>(queueSize);

        Function<String, Void> openCallback = error -> {
            if (error == null) {
                if (sampleRate == 0) {
                    sampleRate = device.getSampleRate();
                }
                if (frequency == 0) {
                    frequency = device.getFrequency();
                }
                if (gain == -1000) {
                    agc = !device.getManualGain();
                    gain = device.getGain();
                }
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> handler.post(() -> {
                if (error == null) {
                    callback.onIQSourceReady(this);
                } else {
                    callback.onIQSourceError(this, error);
                }
            }));
            return null;
        };

        device = new Device();
        return device.open(context, openCallback);
    }

    @Override
    public boolean isOpen() {
        return device != null && device.isOpen();
    }

    @Override
    public boolean close() {
        stopSampling();

        device.close();
        device = null;

        return true;
    }

    @Override
    public String getName() {
        return "bladeRF";
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void setSampleRate(int sampleRate) {
        if (sampleRate != this.sampleRate) {
            this.sampleRate = sampleRate;
            if (device != null && isSampling) {
                device.setSampleRate(sampleRate);
            }
            iqConverter.setSampleRate(sampleRate);
        }
    }

    @Override
    public long getFrequency() {
        return frequency;
    }

    @Override
    public void setFrequency(long frequency) {
        if (frequency % 2 == 1) {
            frequency += 1;
        }

        if (frequency != this.frequency) {
            this.frequency = frequency;
            if (device != null) {
                device.setFrequency(frequency, true);
            }
            iqConverter.setFrequency(frequency);
        }
    }

    @Override
    public long getMaxFrequency() {
        return device != null ? device.getMaxFrequency() : 0;
    }

    @Override
    public long getMinFrequency() {
        return device != null ? device.getMinFrequency() : 0;
    }

    @Override
    public int getMaxSampleRate() {
        return device != null ? device.getMaxSampleRate() : 0;
    }

    @Override
    public int getMinSampleRate() {
        return device != null ? device.getMinSampleRate() : 0;
    }

    @Override
    public int getNextHigherOptimalSampleRate(int sampleRate) {
        if (device == null) {
            return sampleRate;
        }

        final int[] sampleRates = getSupportedSampleRates();

        for (int rate : sampleRates) {
            if (rate > sampleRate) {
                return rate;
            }
        }
        return sampleRates[sampleRates.length - 1];
    }

    @Override
    public int getNextLowerOptimalSampleRate(int sampleRate) {
        if (device == null) {
            return sampleRate;
        }

        final int[] sampleRates = getSupportedSampleRates();

        for (int i = 1; i < sampleRates.length; i++) {
            if (sampleRates[i] > sampleRate) {
                return sampleRates[i - 1];
            }
        }
        return sampleRates[sampleRates.length - 1];
    }

    @Override
    public int[] getSupportedSampleRates() {
        return device != null ? device.getSupportedSampleRates() : new int[] { 0 };
    }

    @Override
    public int getPacketSize() {
        return device.getPacketSize();
    }

    @Override
    public byte[] getPacket(int timeout) {
        try {
            return queue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Interrupted while waiting on sample queue");
            return null;
        }
    }

    @Override
    public void returnPacket(byte[] buffer) {
        if (buffer.length == getPacketSize()) {
            pool.offer(buffer);
        } else {
            Log.w(LOGTAG, "Got a buffer with wrong size " + buffer.length);
        }
    }

    @Override
    public void startSampling() {
        Log.i(LOGTAG, "Starting reception");

        queue.clear();

        if (device == null) {
            return;
        }

        device.setFrequency(frequency, false);
        device.setSampleRate(sampleRate);
        device.setManualGain(!agc);
        device.setGain(gain);
        device.enableRx();

        isSampling = true;

        usbThread = new Thread(this);
        usbThread.start();
    }

    @Override
    public void run() {
        UsbRequest[] usbRequests = null;

        try {
            usbRequests = device.initializeUSBRequests();

            if (usbRequests.length == 0) {
                isSampling = false;
            }

            int timeouts = 0;

            while (isSampling) {
                UsbRequest request;

                try {
                    request = device.getSamples();
                    timeouts = 0;
                } catch (TimeoutException e) {
                    if (++timeouts == 10) {
                        Log.d(LOGTAG, "Rx timeout");
                        break;
                    }
                    continue;
                }

                ByteBuffer buffer = (ByteBuffer) request.getClientData();

                if (!queue.offer(buffer.array())) {
                    //Log.w(LOGTAG, "Sample queue is full");
                }

                byte[] bufferArray = pool.poll();
                if (bufferArray == null) {
                    //Log.w(LOGTAG, "Sample pool is empty");
                    bufferArray = new byte[getPacketSize()];
                }

                if (!device.queueUSBRequest(request, bufferArray)) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Error while setting up USB buffers: " + e);
        }

        device.cancelUSBRequests(usbRequests);

        Log.d(LOGTAG, "USB thread finished");
    }

    @Override
    public void stopSampling() {
        if (!isSampling) {
            return;
        }

        Log.i(LOGTAG, "Stopping reception");

        device.disableRx();

        try {
            usbThread.join();
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Error while stopping USB thread");
        }

        isSampling = false;
    }

    @Override
    public int fillPacketIntoSamplePacket(byte[] packet, SamplePacket samplePacket) {
        return this.iqConverter.fillPacketIntoSamplePacket(packet, samplePacket);
    }

    @Override
    public int mixPacketIntoSamplePacket(byte[] packet, SamplePacket samplePacket, long channelFrequency) {
        return this.iqConverter.mixPacketIntoSamplePacket(packet, samplePacket, channelFrequency);
    }

    public int getGain() {
        return gain;
    }

    public void setGain(int gain) {
        if (gain != this.gain) {
            this.gain = gain;
            if (device != null) {
                device.setGain(gain);
            }
        }
    }

    public boolean getAGC() {
        return agc;
    }

    public void setAGC(boolean agc) {
        if (agc != this.agc) {
            this.agc = agc;
            if (device != null) {
                device.setManualGain(!agc);
            }
        }
    }
}
