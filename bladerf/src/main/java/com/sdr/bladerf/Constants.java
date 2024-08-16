package com.sdr.bladerf;

public class Constants {
    public static final int INTERFACE_NULL = 0;
    public static final int INTERFACE_RF_LINK = 1;

    public static final int USB_CMD_QUERY_VERSION = 0;
    public static final int USB_CMD_QUERY_FPGA_STATUS = 1;
    public static final int USB_CMD_RF_RX = 4;
    public static final int USB_CMD_RF_TX = 5;
    public static final int USB_CMD_QUERY_DEVICE_READY = 6;
    public static final int USB_CMD_RESET = 105;

    public static final int NIOS_PKT_8x16_MAGIC = 'B';
    public static final int NIOS_PKT_8x16_IDX_MAGIC = 0;
    public static final int NIOS_PKT_8x16_IDX_TARGET_ID = 1;
    public static final int NIOS_PKT_8x16_IDX_FLAGS = 2;
    public static final int NIOS_PKT_8x16_IDX_RESV1 = 3;
    public static final int NIOS_PKT_8x16_IDX_ADDR = 4;
    public static final int NIOS_PKT_8x16_IDX_DATA = 5;
    public static final int NIOS_PKT_8x16_IDX_RESV2 = 7;

    public static final byte NIOS_PKT_8x16_TARGET_VCTCXO_DAC = 0;
    public static final byte NIOS_PKT_8x16_TARGET_IQ_CORR = 1;
    public static final byte NIOS_PKT_8x16_TARGET_AGC_CORR = 2;
    public static final byte NIOS_PKT_8x16_TARGET_AD56X1_DAC = 3;
    public static final byte NIOS_PKT_8x16_TARGET_INA219 = 4;

    public static final int NIOS_PKT_8x32_MAGIC = 'C';
    public static final int NIOS_PKT_8x32_IDX_MAGIC = 0;
    public static final int NIOS_PKT_8x32_IDX_TARGET_ID = 1;
    public static final int NIOS_PKT_8x32_IDX_FLAGS = 2;
    public static final int NIOS_PKT_8x32_IDX_RESV1 = 3;
    public static final int NIOS_PKT_8x32_IDX_ADDR = 4;
    public static final int NIOS_PKT_8x32_IDX_DATA = 5;
    public static final int NIOS_PKT_8x32_IDX_RESV2 = 9;

    public static final byte NIOS_PKT_8x32_TARGET_VERSION = 0;
    public static final byte NIOS_PKT_8x32_TARGET_CONTROL = 1;
    public static final byte NIOS_PKT_8x32_TARGET_ADF4351 = 2;
    public static final byte NIOS_PKT_8x32_TARGET_RFFE_CSR = 3;
    public static final byte NIOS_PKT_8x32_TARGET_ADF400X = 4;
    public static final byte NIOS_PKT_8x32_TARGET_FASTLOCK = 5;

    public static final int NIOS_PKT_16x64_MAGIC = 'E';
    public static final int NIOS_PKT_16x64_IDX_MAGIC = 0;
    public static final int NIOS_PKT_16x64_IDX_TARGET_ID = 1;
    public static final int NIOS_PKT_16x64_IDX_FLAGS = 2;
    public static final int NIOS_PKT_16x64_IDX_RESV1 = 3;
    public static final int NIOS_PKT_16x64_IDX_ADDR = 4;
    public static final int NIOS_PKT_16x64_IDX_DATA = 6;
    public static final int NIOS_PKT_16x64_IDX_RESV2 = 14;

    public static final byte NIOS_PKT_16x64_TARGET_AD9361 = 0;
    public static final byte NIOS_PKT_16x64_TARGET_RFIC = 1;

    public static final byte INA219_REG_CONFIGURATION = 0;
    public static final byte INA219_REG_SHUNT_VOLTAGE = 1;
    public static final byte INA219_REG_BUS_VOLTAGE = 2;
    public static final byte INA219_REG_POWER = 3;
    public static final byte INA219_REG_CURRENT = 4;
    public static final byte INA219_REG_CALIBRATION = 5;

    public static final long PLL_VCTCXO_FREQUENCY = 38400000;
    public static final long PLL_REFIN_DEFAULT = 10000000;
    public static final long PLL_RESET_FREQUENCY = 70000000;

    public static final byte CHANNEL_RX0 = 0;
    public static final byte CHANNEL_TX0 = 1;
    public static final byte CHANNEL_RX1 = 2;
    public static final byte CHANNEL_TX1 = 3;
    public static final byte CHANNEL_INVALID = -1;

    public static final byte RFIC_CMD_STATUS = 0;
    public static final byte RFIC_CMD_INIT = 1;
    public static final byte RFIC_CMD_ENABLE = 2;
    public static final byte RFIC_CMD_SAMPLERATE = 3;
    public static final byte RFIC_CMD_FREQUENCY = 4;
    public static final byte RFIC_CMD_BANDWIDTH = 5;
    public static final byte RFIC_CMD_GAINMODE = 6;
    public static final byte RFIC_CMD_GAIN = 7;
    public static final byte RFIC_CMD_RSSI = 8;
    public static final byte RFIC_CMD_FILTER = 9;
    public static final byte RFIC_CMD_TXMUTE = 10;

    public static final int RFIC_STATE_OFF = 0;
    public static final int RFIC_STATE_ON = 1;
    public static final int RFIC_STATE_STANDBY = 2;

    public static final int RFIC_RXFIR_BYPASS = 0;
    public static final int RFIC_RXFIR_CUSTOM = 1;
    public static final int RFIC_RXFIR_DEC1 = 2;
    public static final int RFIC_RXFIR_DEC2 = 3;
    public static final int RFIC_RXFIR_DEC4 = 4;

    public static final int RFIC_TXFIR_BYPASS = 0;
    public static final int RFIC_TXFIR_CUSTOM = 1;
    public static final int RFIC_TXFIR_INT1 = 2;
    public static final int RFIC_TXFIR_INT2 = 3;
    public static final int RFIC_TXFIR_INT4 = 4;

    public static final int GAIN_DEFAULT = 0;
    public static final int GAIN_MGC = 1;
    public static final int GAIN_FASTATTACK_AGC = 2;
    public static final int GAIN_SLOWATTACK_AGC = 3;
    public static final int GAIN_HYBRID_AGC = 4;
}
