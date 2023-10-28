package Protocols0;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RdtTransceiver {
    private enum SenderState{
        WAIT_FOR_CALL,
        WAIT_FOR_ACK
    }

    private String getStateName(SenderState state,int serialNumber){
        return switch(state){
            case WAIT_FOR_CALL -> String.format("WAIT_FOR_CALL%d",serialNumber);
            case WAIT_FOR_ACK -> String.format("WAIT_FOR_ACK%d",serialNumber);
        };
    }
    
    private byte flipSerialNumber(byte currentSerialNumber){
        return switch(currentSerialNumber){
            case 0->1;
            default -> 0;
        };
    }

    public void sendPackets(List<byte[]> packetData,
                            double lossProbability,
                            int atPort,
                            int toPort,
                            int owtMs,
                            int maxDelayMs){
        SenderState currentState = SenderState.WAIT_FOR_CALL;
        int currentPacketIndex=0;
        byte currentSerialNumber=0;
        Random random=new Random();

        try(DatagramSocket socket=new DatagramSocket(atPort)) {
            socket.setSoTimeout(maxDelayMs);

            while(currentPacketIndex<packetData.size()) {
                switch (currentState) {
                    case WAIT_FOR_CALL -> {
                        System.out.printf("***[%s]【%d】号包已发送%n",
                                getStateName(currentState, currentSerialNumber),
                                currentPacketIndex);
                        // 通过不发送模拟丢包
                        if (random.nextDouble()<lossProbability) {
                            System.out.printf("***[%s]【%d】号丢包%n",
                                    getStateName(currentState, currentSerialNumber),
                                    currentPacketIndex);
                            currentState = SenderState.WAIT_FOR_ACK;
                            break;
                        }

                        // 模拟RTT
                        Thread.sleep(owtMs);

                        // 发送数据包
                        byte[] sendData=new byte[packetData.get(currentPacketIndex).length+1];
                        sendData[0]=currentSerialNumber;
                        System.arraycopy(
                                packetData.get(currentPacketIndex),
                                0, sendData, 1, sendData.length - 1);

                        try {
                            socket.send(new DatagramPacket(
                                    sendData,
                                    sendData.length,
                                    InetAddress.getByName("localhost"),
                                    toPort));
                        } catch(UnknownHostException e){
                            System.out.println("解析域名出错");
                            e.printStackTrace();
                        } catch (IOException e) {
                            System.out.println("发送数据包出错");
                            e.printStackTrace();
                        }

                        currentState = SenderState.WAIT_FOR_ACK;
                    }
                    case WAIT_FOR_ACK -> {
                        System.out.printf("***[%s]【%d】号包的计时器开启，超时%dms%n",
                                getStateName(currentState, currentSerialNumber),
                                currentPacketIndex,
                                maxDelayMs);
                        DatagramPacket receivePacket = new DatagramPacket(
                                new byte[Protocols0.Constants.ACK_BUFFER_SIZE],
                                Protocols0.Constants.ACK_BUFFER_SIZE
                        );
                        try {
                            socket.receive(receivePacket);
                        } catch (SocketTimeoutException e) {
                            currentState = SenderState.WAIT_FOR_CALL;
                            System.out.printf("***[%s]【%d】号超时未收到ACK，重新发送%n",
                                    getStateName(currentState, currentSerialNumber),
                                    currentPacketIndex);
                            break;
                        } catch (IOException e) {
                            System.out.println("接收数据包出错");
                            e.printStackTrace();
                        }

                        if (receivePacket.getData()[0] != currentSerialNumber) {
                            System.out.printf("***[%s]收到的ACK序列号【%d】不为0%n",
                                    getStateName(currentState, currentSerialNumber),
                                    currentPacketIndex);
                            break;
                        }
                        System.out.printf("***[%s]正确收到【%d】号包的ACK%n",
                                getStateName(currentState, currentSerialNumber),
                                currentPacketIndex);
                        currentPacketIndex++;
                        currentState = SenderState.WAIT_FOR_CALL;
                        currentSerialNumber = flipSerialNumber(currentSerialNumber);
                    }
                }
            }

            byte[] finalMessageData=new byte[] {-1};
            try {
                socket.send(new DatagramPacket(
                        finalMessageData,
                        finalMessageData.length,
                        InetAddress.getByName("localhost"),
                        toPort));
            } catch(UnknownHostException e){
                System.out.println("解析域名出错");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("发送数据包出错");
                e.printStackTrace();
            }

            System.out.println("发送完成");

        } catch (SocketException e) {
            System.out.println("创建UDP套接字出错");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public List<Byte> receivePackets(double lossProbability,
                               int atPort,
                               int owtMs){
        Random random=new Random();
        List<Byte> receivedBytes=new ArrayList<>();
        byte currentSerialNumber=0;

        try(DatagramSocket socket=new DatagramSocket(atPort)) {
            while(true) {
                DatagramPacket receivePacket = new DatagramPacket(
                        new byte[Protocols0.Constants.DATA_BUFFER_SIZE],
                        Protocols0.Constants.DATA_BUFFER_SIZE
                );

                try {
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    System.out.println("接收数据包出错");
                    e.printStackTrace();
                }

                System.out.printf("***[WAIT_FOR_%d]收到序列号为%d的数据包，大小为%d%n",
                        currentSerialNumber,
                        receivePacket.getData()[0],
                        receivePacket.getLength());

                // 终止信号
                if(receivePacket.getLength()==1
                        &&receivePacket.getData()[0]==-1){
                    System.out.println("***接收完成");
                    break;
                }

                byte[] ackData=new byte[Protocols0.Constants.ACK_BUFFER_SIZE];
                ackData[0]=receivePacket.getData()[0];

                if(receivePacket.getData()[0]==currentSerialNumber){
                    currentSerialNumber=flipSerialNumber(currentSerialNumber);
                    for(int i=1;i<receivePacket.getLength();i++){
                        receivedBytes.add(receivePacket.getData()[i]);
                    }
                    System.out.println("***接收正确");
                }

                System.out.println("***ACK已发送");
                // 模拟ACK丢包
                if(random.nextDouble()<lossProbability){
                    System.out.println("***ACK发送丢包");
                    continue;
                }

                // 模拟RTT
                Thread.sleep(owtMs);

                try {
                    socket.send(new DatagramPacket(
                            ackData,
                            ackData.length,
                            receivePacket.getSocketAddress()
                    ));
                } catch (IOException e) {
                    System.out.println("发送ACK数据包出错");
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            System.out.println("创建UDP套接字出错");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return receivedBytes;
    }
}
