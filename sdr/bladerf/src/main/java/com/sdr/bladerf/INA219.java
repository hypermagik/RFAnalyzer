package com.sdr.bladerf;

import android.util.Log;

public class INA219 {
    private static final String LOGTAG = "bladeRF-INA219";

    private final NIOS nios;

    static final float shunt = 0.001F;

    INA219(NIOS nios) {
        this.nios = nios;
    }

    private Short read(byte command) {
        return nios.read8x16(Constants.NIOS_PKT_8x16_TARGET_INA219, command);
    }

    private boolean write(byte command, short value) {
        return nios.write8x16(Constants.NIOS_PKT_8x16_TARGET_INA219, command, value);
    }

    public boolean initialize() {
        Log.i(LOGTAG, "Resetting INA219");

        short value = (short) 0x8000;
        if (!write(Constants.INA219_REG_CONFIGURATION, value)) {
            Log.e(LOGTAG, "INA219 soft reset error");
            return false;
        }

        // Poll until we're out of reset
        while ((value & 0x8000) != 0) {
            Short result = read(Constants.INA219_REG_CONFIGURATION);
            if (result == null) {
                Log.e(LOGTAG, "INA219 soft reset poll error");
                return false;
            }
            value = result;
        }

        // Write configuration register
        // BRNG   (13) = 0 for 16V FSR
        // PG  (12-11) = 00 for 40mV
        // BADC (10-7) = 0011 for 12-bit / 532uS
        // SADC  (6-3) = 0011 for 12-bit / 532uS
        // MODE  (2-0) = 111 for continuous shunt & bus
        value = 0x019f;
        if (!write(Constants.INA219_REG_CONFIGURATION, value)) {
            Log.e(LOGTAG, "INA219 configuration error");
            return false;
        }

        Log.i(LOGTAG, String.format("Configuration register: 0x%04x", value));

        // Write calibration register
        // Current_LSB = 0.001 A / LSB
        // Calibration = 0.04096 / (Current_LSB * r_shunt)
        value = (short) ((0.04096 / (0.001 * shunt)) + 0.5);
        if (!write(Constants.INA219_REG_CALIBRATION, value)) {
            Log.e(LOGTAG, "INA219 calibration error");
            return false;
        }

        Log.i(LOGTAG, String.format("Calibration register: 0x%04x", value));

        return true;
    }
}
