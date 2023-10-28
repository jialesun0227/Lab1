package Protocols0;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class GbnTransceiver extends Protocols0.SlidingWindowTransceiver {
    @Override
    public void sendPackets(List<byte[]> packetData,
                            double lossProbability,
                            int atPort,
                            int toPort,
                            int windowSize,
                            int owtMs,
                            int maxDelayMs){
        int sendBasePacketIndex=0;
        int nextPacketIndex=0;
        Random random=new Random();

        try(DatagramSocket socket=new DatagramSocket(atPort)) {
            socket.setSoTimeout(maxDelayMs);

            while(true){
                for(;nextPacketIndex<sendBasePacketIndex+windowSize
                        &&nextPacketIndex<packetData.size();
                    nextPacketIndex++){
                    byte[] sendData=makeSendData(
                            packetData.get(nextPacketIndex),
                            nextPacketIndex);

                    System.out.printf("***【%d】号数据包已发送%n",
                            nextPacketIndex);

                    // 模拟丢包
                    if(random.nextDouble()<lossProbability){
                        System.out.printf("***【%d】号丢包%n",nextPacketIndex);
                        continue;
                    }

                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                socket.send(new DatagramPacket(
                                        sendData,
                                        sendData.length,
                                        InetAddress.getByName("localhost"),
                                        toPort
                                ));
                            } catch(UnknownHostException e){
                                System.out.println("解析域名出错");
                                e.printStackTrace();
                            } catch (IOException e) {
                                System.out.println("发送数据包出错");
                                e.printStackTrace();
                            }
                        }
                    },owtMs);

                    Thread.sleep(50);
                }

                System.out.printf("***计时器开启，超时%dms%n",
                        maxDelayMs);
                DatagramPacket receivePacket = new DatagramPacket(
                        new byte[Constants.ACK_BUFFER_SIZE],
                        Constants.ACK_BUFFER_SIZE
                );

                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    nextPacketIndex=sendBasePacketIndex;
                    System.out.printf("***超时，从【%d】号包开始重新发送%n",
                            sendBasePacketIndex);
                    continue;
                } catch (IOException e) {
                    System.out.println("接收数据包出错");
                    e.printStackTrace();
                }

                int receivedAckPacketIndex=getInt32FromByteArray(receivePacket.getData());

                System.out.printf("***收到【%d】号包的ACK%n", receivedAckPacketIndex);

                sendBasePacketIndex=receivedAckPacketIndex+1;

                if(sendBasePacketIndex==packetData.size()){
                    break;
                }
            }

            byte[] finalMessageData=getByteArrayFromInt32(PACKET_INDEX_FINAL_CODE);
            try {
                socket.send(new DatagramPacket(
                        finalMessageData,
                        finalMessageData.length,
                        InetAddress.getByName("localhost"),
                        toPort
                ));
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

    @Override
    public List<Byte> receivePackets(double lossProbability,
                               int atPort,
                               int owtMs){
        List<Byte> receivedBytes=new ArrayList<>();
        int expectedPacketIndex=0;

        Random random=new Random();

        try(DatagramSocket socket=new DatagramSocket(atPort)) {
            DatagramPacket receivePacket = new DatagramPacket(
                    new byte[Constants.DATA_BUFFER_SIZE],
                    Constants.DATA_BUFFER_SIZE
            );

            while(true) {
                try {
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    System.out.println("接收数据包出错");
                    e.printStackTrace();
                }

                int receivedPacketIndex=getInt32FromByteArray(receivePacket.getData());

                // 终止信号
                if(receivedPacketIndex==PACKET_INDEX_FINAL_CODE){
                    System.out.println("***接收完成");
                    break;
                }

                System.out.printf("***收到【%d】号数据包，期望【%d】号，大小为%d%n",
                        receivedPacketIndex,
                        expectedPacketIndex,
                        receivePacket.getLength());

                if(receivedPacketIndex==expectedPacketIndex){
                    for(int i=4;i<receivePacket.getLength();i++){
                        receivedBytes.add(receivePacket.getData()[i]);
                    }
                    System.out.println("***接收正确");
                    expectedPacketIndex++;
                }

                System.out.printf("***【%d】号ACK已发送%n",expectedPacketIndex-1);
                // 模拟ACK丢包
                if(random.nextDouble()<lossProbability){
                    System.out.println("***ACK发送丢包");
                    continue;
                }

                // 模拟RTT
                Thread.sleep(owtMs);

                byte[] ackData=getByteArrayFromInt32(expectedPacketIndex-1);

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
