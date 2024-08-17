package com.sdr.bladerf;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class Device {
    private static final String LOGTAG = "bladeRF-Device";
    private static final boolean dumpMessages = false;

    private UsbDevice usbDevice = null;
    private UsbInterface usbInterface = null;
    private UsbDeviceConnection usbConnection = null;
    private UsbEndpoint usbSampleEndpointIn = null;
    private UsbEndpoint usbSampleEndpointOut = null;

    private static final int usbTimeout = 1000;
    private static final int usbTimeoutForSamples = 10;

    private static final int packetSize = 8192;
    private static final int nofTransfers = 32;

    private static final int minSampleRate = 520834;
    private static final int maxSampleRate = 61440000;
    private static final int firSampleRate = 2083334;

    private static final int[] supportedSampleRates = {520834, 1000000, 2000000, 4000000, 8000000, 10000000, 20000000, 30000000, 40000000, 61440000};

    private static final long minFrequency = 70000000L;
    private static final long maxFrequency = 6000000000L;

    private NIOS nios = null;
    private RFIC rfic = null;

    public Device() {
    }

    public boolean open(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            Log.e(LOGTAG, "Couldn't get USB manager");
            return false;
        }

        final HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (deviceList == null) {
            Log.e(LOGTAG, "Couldn't get list of USB devices");
            return false;
        }

        Log.i(LOGTAG, "Found " + deviceList.size() + " USB devices");

        for (UsbDevice device : deviceList.values()) {
            Log.i(LOGTAG, device.toString());

            if (device.getVendorId() == 11504 && device.getProductId() == 21072) {
                Log.i(LOGTAG, "Found bladeRF at " + device.getDeviceName());
                usbDevice = device;
                break;
            }
        }

        if (usbDevice == null) {
            Log.e(LOGTAG, "No bladeRF device found");
            return false;
        }

        try {
            usbConnection = usbManager.openDevice(usbDevice);
        } catch (Exception e) {
            Log.e(LOGTAG, "Couldn't open USB device: " + e.getMessage());
            return false;
        }

        try {
            usbInterface = usbDevice.getInterface(Constants.INTERFACE_RF_LINK);
        } catch (Exception e) {
            Log.e(LOGTAG, "Couldn't get USB interface: " + e.getMessage());
            usbConnection.close();
            return false;
        }

        if (!usbConnection.claimInterface(usbInterface, true)) {
            Log.e(LOGTAG, "Couldn't claim USB interface");
            usbConnection.close();
            return false;
        }

        if (!usbConnection.setInterface(usbInterface)) {
            Log.e(LOGTAG, "Couldn't set USB interface");
            usbConnection.close();
            return false;
        }

        UsbEndpoint usbPeripheralEndpointIn;
        UsbEndpoint usbPeripheralEndpointOut;

        try {
            usbSampleEndpointIn = usbInterface.getEndpoint(0);
            usbSampleEndpointOut = usbInterface.getEndpoint(1);
            usbPeripheralEndpointIn = usbInterface.getEndpoint(2);
            usbPeripheralEndpointOut = usbInterface.getEndpoint(3);
        } catch (Exception e) {
            Log.e(LOGTAG, "Couldn't get USB endpoints: " + e.getMessage());
            usbConnection.close();
            return false;
        }

        Log.i(LOGTAG, "Rx endpoint address: " + usbSampleEndpointIn.getAddress()
                + " attributes: " + usbSampleEndpointIn.getAttributes() + " direction: " + usbSampleEndpointIn.getDirection()
                + " max_packet_size: " + usbSampleEndpointIn.getMaxPacketSize());

        if (!isFirmwareReady()) {
            Log.e(LOGTAG, "Device firmware is not ready, resetting device");
            reset();
            close();
            return false;
        }
        Log.i(LOGTAG, "Device firmware is ready");

        final String firmwareVersion = getFirmwareVersion();
        if (firmwareVersion == null) {
            Log.e(LOGTAG, "Could not get firmware version");
            close();
            return false;
        }
        Log.i(LOGTAG, "Firmware version is " + firmwareVersion);

        if (!isFPGALoaded()) {
            Log.e(LOGTAG, "FPGA is not loaded");
            close();
            return false;
        }
        Log.i(LOGTAG, "FPGA is loaded");

        nios = new NIOS(usbConnection, usbPeripheralEndpointIn, usbPeripheralEndpointOut, dumpMessages);

        final String fpgaVersion = nios.getFPGAVersion();
        if (fpgaVersion == null) {
            Log.e(LOGTAG, "Could not get FPGA version");
        }
        Log.i(LOGTAG, "FPGA version is " + fpgaVersion);

        rfic = new RFIC(nios);
        if (!rfic.open()) {
            Log.e(LOGTAG, "Could not initialize RFIC");
            rfic = null;
            close();
            return false;
        }

        rfic.setTxMute();

        INA219 ina219 = new INA219(nios);
        if (!ina219.initialize()) {
            Log.e(LOGTAG, "Could not initialize INA219");
            close();
            return false;
        }

        Log.i(LOGTAG, "Sample rate is " + rfic.getSampleRate());
        Log.i(LOGTAG, "Frequency is " + rfic.getFrequency());
        Log.i(LOGTAG, "Bandwidth is " + rfic.getBandwidth());
        Log.i(LOGTAG, "Gain mode is " + rfic.getGainMode());
        Log.i(LOGTAG, "Gain is " + rfic.getGain());
        Log.i(LOGTAG, "Rx filter is " + rfic.getRxFilter());
        Log.i(LOGTAG, "VCTCXO trim is " + nios.getVCTCXOTrim());
        Log.i(LOGTAG, String.format("GPIO is 0x%08x", nios.getGPIO()));
        Log.i(LOGTAG, String.format("RFFE CSR is 0x%08x", nios.getRFFECSR()));

        if (!nios.setVCTCXOTrim((short) 0x1f3f)) {
            Log.e(LOGTAG, "Could not set VCTCXO trim");
        }

        rfic.setGainMode(Constants.GAIN_MGC);
        rfic.setGain(16);

        final String deviceSerialNumber = usbDevice.getSerialNumber();
        Log.i(LOGTAG, "Device with serial number " + deviceSerialNumber + " is ready");

        return true;
    }

    public void close() {
        if (rfic != null) {
            rfic.close();
            rfic = null;
        }

        if (nios != null) {
            nios = null;
        }

        if (usbConnection != null) {
            usbConnection.releaseInterface(usbInterface);
            usbConnection.close();
            usbConnection = null;
        }
    }

    public boolean isOpen() {
        return rfic != null;
    }

    public int getPacketSize() {
        return packetSize;
    }

    private int controlTransfer(int request, byte[] buffer) {
        int len = buffer == null ? 0 : buffer.length;

        int endpoint = UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_VENDOR;

        len = usbConnection.controlTransfer(endpoint, request, 0, 0, buffer, len, usbTimeout);
        if (len < 0) {
            Log.e(LOGTAG, "USB control transfer failed"
                    + ", endpoint=" + endpoint
                    + ", request=" + request
                    + ", result=" + len);
        }

        return len;
    }

    private boolean isFirmwareReady() {
        Log.i(LOGTAG, "Reading firmware ready state");

        byte[] result = new byte[4];
        int n = controlTransfer(Constants.USB_CMD_QUERY_DEVICE_READY, result);
        if (n != 4) {
            Log.e(LOGTAG, "Response length mismatch");
            return false;
        }

        return Utils.getInt(result) == 1;
    }

    private String getFirmwareVersion() {
        Log.i(LOGTAG, "Reading firmware version");

        byte[] result = new byte[4];
        int n = controlTransfer(Constants.USB_CMD_QUERY_VERSION, result);
        if (n != 4) {
            Log.e(LOGTAG, "Response length mismatch");
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        final int major = buffer.getShort();
        final int minor = buffer.getShort();
        return major + "." + minor;
    }

    private boolean isFPGALoaded() {
        Log.i(LOGTAG, "Reading FPGA ready state");

        byte[] result = new byte[4];
        int n = controlTransfer(Constants.USB_CMD_QUERY_FPGA_STATUS, result);
        if (n != 4) {
            Log.e(LOGTAG, "Response length mismatch");
            return false;
        }

        return Utils.getInt(result) == 1;
    }

    private int controlTransferWithResult(int request, int value) {
        byte[] result = new byte[4];

        final int endpoint = UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_VENDOR;

        int len = usbConnection.controlTransfer(endpoint, request, value, 0, result, result.length, usbTimeout);
        if (len < 0 && request != Constants.USB_CMD_RESET) {
            Log.e(LOGTAG, "USB control transfer failed"
                    + ", endpoint=" + endpoint
                    + ", request=" + request
                    + ", result=" + len);
            return -1;
        }

        return Utils.getInt(result);
    }

    private void reset() {
        controlTransferWithResult(Constants.USB_CMD_RESET, 1);
    }

    private boolean toggleRx(int state) {
        return controlTransferWithResult(Constants.USB_CMD_RF_RX, state) == 0;
    }

    public long getMinFrequency() {
        return minFrequency;
    }

    public long getMaxFrequency() {
        return maxFrequency;
    }

    public int getMinSampleRate() {
        return minSampleRate;
    }

    public int getMaxSampleRate() {
        return maxSampleRate;
    }

    public int[] getSupportedSampleRates() {
        return supportedSampleRates;
    }

    public void setSampleRate(int sampleRate) {
        if (rfic != null) {
            if (sampleRate < firSampleRate) {
                if (!rfic.setRxFilter(Constants.RFIC_RXFIR_DEC4) || !rfic.setTxFilter(Constants.RFIC_TXFIR_INT4)) {
                    Log.e(LOGTAG, "Failed to set FIR filters to 4x decimate/interpolate");
                }
            } else {
                if (rfic.getSampleRate() < firSampleRate) {
                    rfic.setSampleRate(firSampleRate);
                }
                if (sampleRate <= 61440000 / 2) {
                    if (!rfic.setRxFilter(Constants.RFIC_RXFIR_DEC2) || !rfic.setTxFilter(Constants.RFIC_TXFIR_INT2)) {
                        Log.e(LOGTAG, "Failed to set FIR filters to 2x decimate/interpolate");
                    }
                } else if (!rfic.setRxFilter(Constants.RFIC_RXFIR_DEC1) || !rfic.setTxFilter(Constants.RFIC_TXFIR_INT1)) {
                    Log.e(LOGTAG, "Failed to set FIR filters to 1x decimate/interpolate");
                }
            }
            rfic.setSampleRate(sampleRate);
            rfic.setBandwidth((long) (sampleRate * 0.9));
        }
    }

    public void setFrequency(long frequency, boolean silent) {
        if (rfic != null) {
            rfic.setFrequency(frequency, silent);
        }
    }

    public boolean getManualGain() {
        return rfic != null && rfic.getGainMode() == Constants.GAIN_MGC;
    }

    public void setManualGain(boolean enable) {
        if (rfic != null) {
            rfic.setGainMode(enable ? Constants.GAIN_MGC : Constants.GAIN_SLOWATTACK_AGC);
        }
    }

    public int getGain() {
        if (rfic != null) {
            return rfic.getGain();
        }
        return 0;
    }

    public void setGain(int gain) {
        if (rfic != null) {
            rfic.setGain(gain);
        }
    }

    public void enableRx() {
        if (rfic == null) {
            return;
        }

        if (toggleRx(1)) {
            Log.i(LOGTAG, "Rx enabled");
        } else {
            Log.e(LOGTAG, "Failed to enable Rx (firmware error)");
        }

        if (!rfic.enable(Constants.RFIC_STATE_ON)) {
            Log.e(LOGTAG, "Failed to enable Rx (RFIC error)");
        }
    }

    public void disableRx() {
        if (rfic == null) {
            return;
        }

        rfic.enable(Constants.RFIC_STATE_OFF);

        toggleRx(0);
    }

    public UsbRequest[] initializeUSBRequests() {
        UsbRequest[] requests = new UsbRequest[nofTransfers];

        boolean error = false;

        for (int i = 0; i < requests.length; i++) {
            UsbRequest request = requests[i] = new UsbRequest();

            if (!request.initialize(usbConnection, usbSampleEndpointIn)) {
                Log.e(LOGTAG, "Couldn't queue USB Request");
                error = true;
                break;
            }

            if (!queueUSBRequest(request, new byte[packetSize])) {
                error = true;
                break;
            }
        }

        if (error) {
            cancelUSBRequests(requests);
            return new UsbRequest[0];
        }

        return requests;
    }

    public void cancelUSBRequests(UsbRequest[] requests) {
        if (requests != null) {
            for (UsbRequest request : requests) {
                if (request != null) {
                    request.cancel();
                }
            }
        }
    }

    public UsbRequest getSamples() throws TimeoutException {
        while (true) {
            UsbRequest request = usbConnection.requestWait(usbTimeoutForSamples);

            if (request.getEndpoint() != usbSampleEndpointIn) {
                Log.w(LOGTAG, "Received USB request for wrong endpoint");
                continue;
            }

            return request;
        }
    }

    public boolean queueUSBRequest(UsbRequest request, byte[] bufferArray) {
        ByteBuffer buffer = ByteBuffer.wrap(bufferArray);
        request.setClientData(buffer);

        if (!request.queue(buffer)) {
            Log.e(LOGTAG, "Couldn't queue USB request");
            return false;
        }

        return true;
    }
}