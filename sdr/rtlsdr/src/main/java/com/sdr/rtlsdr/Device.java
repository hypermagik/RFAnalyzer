package com.sdr.rtlsdr;

import android.content.Context;
import android.hardware.usb.UsbRequest;

import com.sdr.common.DeviceType;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;

public class Device implements com.sdr.common.IDevice {
    @Override
    public DeviceType getType() {
        return DeviceType.RTLSDR;
    }

    @Override
    public String getName() {
        return "RTL-SDR";
    }

    @Override
    public boolean open(Context context, Function<String, Void> callback) {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public int getPacketSize() {
        return 0;
    }

    @Override
    public long getMinFrequency() {
        return 0;
    }

    @Override
    public long getMaxFrequency() {
        return 0;
    }

    @Override
    public int getMinSampleRate() {
        return 0;
    }

    @Override
    public int getMaxSampleRate() {
        return 0;
    }

    @Override
    public int[] getSupportedSampleRates() {
        return new int[0];
    }

    @Override
    public int getSampleRate() {
        return 0;
    }

    @Override
    public void setSampleRate(int sampleRate) {

    }

    @Override
    public long getFrequency() {
        return 0;
    }

    @Override
    public void setFrequency(long frequency, boolean silent) {

    }

    @Override
    public boolean getManualGain() {
        return false;
    }

    @Override
    public void setManualGain(boolean enable) {

    }

    @Override
    public int getGain() {
        return 0;
    }

    @Override
    public void setGain(int gain) {

    }

    @Override
    public void enableRx() {

    }

    @Override
    public void disableRx() {

    }

    @Override
    public UsbRequest[] initializeUSBRequests() {
        return new UsbRequest[0];
    }

    @Override
    public void cancelUSBRequests(UsbRequest[] requests) {

    }

    @Override
    public UsbRequest getSamples() throws TimeoutException {
        return null;
    }

    @Override
    public boolean queueUSBRequest(UsbRequest request, byte[] bufferArray) {
        return false;
    }
}
