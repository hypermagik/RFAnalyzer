package com.mantz_it.rfanalyzer;

public class Signed12BitIQConverter extends IQConverter {
    public Signed12BitIQConverter() {
        super();
    }

    @Override
    protected void generateLookupTable() {
        lookupTable = new float[4096];

        for (int i = 0; i < 4096; i++) {
            lookupTable[i] = (i - 2048) / 2048.0f;
        }
    }

    @Override
    protected void generateMixerLookupTable(int mixFrequency) {
        if (mixFrequency == 0 || (sampleRate / Math.abs(mixFrequency) > MAX_COSINE_LENGTH)) {
            mixFrequency += sampleRate;
        }

        if (cosineRealLookupTable == null || mixFrequency != cosineFrequency) {
            cosineFrequency = mixFrequency;

            final int bestLength = calcOptimalCosineLength();

            cosineRealLookupTable = new float[bestLength][4096];
            cosineImagLookupTable = new float[bestLength][4096];

            for (int t = 0; t < bestLength; t++) {
                final float cosineAtT = (float) Math.cos(2 * Math.PI * cosineFrequency * t / (float) sampleRate);
                final float sineAtT = (float) Math.sin(2 * Math.PI * cosineFrequency * t / (float) sampleRate);

                for (int i = 0; i < 4096; i++) {
                    cosineRealLookupTable[t][i] = (i - 2048) / 2048.0f * cosineAtT;
                    cosineImagLookupTable[t][i] = (i - 2048) / 2048.0f * sineAtT;
                }
            }

            cosineIndex = 0;
        }
    }

    @Override
    public int fillPacketIntoSamplePacket(byte[] packet, SamplePacket samplePacket) {
        final int count = Math.min(packet.length / 4, samplePacket.capacity());
        final int startIndex = samplePacket.size();

        float[] re = samplePacket.re();
        float[] im = samplePacket.im();

        for (int i = 0; i < count; i++) {
            final short sre = (short) (((short) packet[i * 4 + 1] << 8) | (short) packet[i * 4 + 0]);
            final short sim = (short) (((short) packet[i * 4 + 3] << 8) | (short) packet[i * 4 + 2]);
            re[startIndex + i] = lookupTable[sre + 2048];
            im[startIndex + i] = lookupTable[sim + 2048];
        }

        samplePacket.setSize(samplePacket.size() + count);
        samplePacket.setSampleRate(sampleRate);
        samplePacket.setFrequency(frequency);

        return count;
    }

    @Override
    public int mixPacketIntoSamplePacket(byte[] packet, SamplePacket samplePacket, long channelFrequency) {
        final int mixFrequency = (int) (frequency - channelFrequency);
        generateMixerLookupTable(mixFrequency);

        final int count = Math.min(packet.length / 4, samplePacket.capacity());
        final int startIndex = samplePacket.size();

        float[] re = samplePacket.re();
        float[] im = samplePacket.im();

        for (int i = 0; i < count; i++) {
            final short sre = (short) (((short) packet[i * 4 + 1] << 8) | (short) packet[i * 4 + 0]);
            final short sim = (short) (((short) packet[i * 4 + 3] << 8) | (short) packet[i * 4 + 2]);
            re[startIndex + i] = cosineRealLookupTable[cosineIndex][sre + 2048] - cosineImagLookupTable[cosineIndex][sim + 2048];
            im[startIndex + i] = cosineRealLookupTable[cosineIndex][sim + 2048] + cosineImagLookupTable[cosineIndex][sre + 2048];
            cosineIndex = (cosineIndex + 1) % cosineRealLookupTable.length;
        }

        samplePacket.setSize(samplePacket.size() + count);
        samplePacket.setSampleRate(sampleRate);
        samplePacket.setFrequency(channelFrequency);

        return count;
    }
}
