package Protocols0;

import java.util.List;

public abstract class SlidingWindowTransceiver {
    protected final int PACKET_INDEX_FINAL_CODE = -1;

    protected byte[] getByteArrayFromInt32(int value) {
        byte[] byteArray = new byte[4];
        byteArray[0] = (byte) (value & 0xFF);
        byteArray[1] = (byte) ((value & 0xFF00) >>> 8);
        byteArray[2] = (byte) ((value & 0xFF0000) >>> 16);
        byteArray[3] = (byte) ((value & 0xFF000000) >>> 24);
        return byteArray;
    }

    protected int getInt32FromByteArray(byte[] data) {
        // 确保字节转换为int32后高24位清零
        int value = ((int) data[0]) & 0xFF;
        value |= (((int) data[1]) & 0xFF) << 8;
        value |= (((int) data[2]) & 0xFF) << 16;
        value |= (((int) data[3]) & 0xFF) << 24;
        return value;
    }

    // 使用前4字节标注下标（序列号）
    protected final byte[] makeSendData(byte[] data, int index) {
        byte[] sendData = new byte[data.length + 4];
        byte[] indexData = getByteArrayFromInt32(index);
        System.arraycopy(indexData, 0, sendData, 0, indexData.length);
        System.arraycopy(data, 0, sendData, 4, data.length);
        return sendData;
    }

    public abstract void sendPackets(List<byte[]> packetData,
                                     double lossProbability,
                                     int atPort,
                                     int toPort,
                                     int windowSize,
                                     int owtMs,
                                     int maxDelayMs);

    public abstract List<Byte> receivePackets(double lossProbability,
                                              int atPort,
                                              int owtMs);
}
