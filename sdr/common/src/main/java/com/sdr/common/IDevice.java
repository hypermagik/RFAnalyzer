package com.sdr.common;

import android.content.Context;
import android.hardware.usb.UsbRequest;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public interface IDevice {
    DeviceType getType();
    String getName();

    boolean open(Context context, Function<String, Void> callback);
    void close();
    boolean isOpen();

    int getPacketSize();

    long getMinFrequency();
    long getMaxFrequency();

    int getMinSampleRate();
    int getMaxSampleRate();
    int[] getSupportedSampleRates();

    int getSampleRate();
    void setSampleRate(int sampleRate);

    long getFrequency();
    void setFrequency(long frequency, boolean silent);

    boolean getManualGain();
    void setManualGain(boolean enable);

    int getGain();
    void setGain(int gain);

    void enableRx();
    void disableRx();

    UsbRequest[] initializeUSBRequests();
    void cancelUSBRequests(UsbRequest[] requests);
    UsbRequest getSamples() throws TimeoutException;
    boolean queueUSBRequest(UsbRequest request, byte[] bufferArray);
}
