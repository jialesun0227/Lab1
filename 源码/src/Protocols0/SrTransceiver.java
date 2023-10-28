package Protocols0;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class SrTransceiver extends Protocols0.SlidingWindowTransceiver {
    private final byte SEND_STATE_UNSENT=0;
    private final byte SEND_STATE_WAIT_FOR_ACK=1;
    private final byte SEND_STATE_ACK_RECEIVED=2;

    private int getNextSendBasePacketIndex(byte[] sendStates,int sendBasePacketIndex){
        for(int i=sendBasePacketIndex;i<sendStates.length;i++){
            if(sendStates[i]==SEND_STATE_WAIT_FOR_ACK
            ||sendStates[i]==SEND_STATE_UNSENT){
                return i;
            }
        }

        return sendStates.length;
    }

    private int getNextReceiveBasePacketIndex(
            List<Boolean> isReceived,int receiveBasePacketIndex){
        for(int i=receiveBasePacketIndex;i<isReceived.size();i++){
            if(!isReceived.get(i)){
                return i;
            }
        }

        return isReceived.size();
    }

    @Override
    public void sendPackets(List<byte[]> packetData,
                            double lossProbability,
                            int atPort,
                            int toPort,
                            int windowSize,
                            int owtMs,
                            int maxDelayMs){
        final byte[] sendStates=new byte[packetData.size()];
        Arrays.fill(sendStates,SEND_STATE_UNSENT);

        int sendBasePacketIndex=0;

        Random random=new Random();

        try(DatagramSocket socket=new DatagramSocket(atPort)) {
            socket.setSoTimeout(100);

            byte[] windowSizeData=getByteArrayFromInt32(windowSize);
            // 发送窗口大小信息
            try {
                socket.send(new DatagramPacket(
                        windowSizeData,
                        windowSizeData.length,
                        InetAddress.getByName("localhost"),
                        toPort
                ));
            } catch (UnknownHostException e) {
                System.out.println("解析域名出错");
                e.printStackTrace();
            } catch (IOException e) {
                System.out.println("发送数据包出错");
                e.printStackTrace();
            }

            while(true){
                // System.out.printf("***当前sendBase为%d%n",sendBasePacketIndex);
                // 发送窗口中所有未发送数据包
                for(int i = sendBasePacketIndex;
                    i<sendBasePacketIndex+windowSize
                        &&i<packetData.size();
                    i++){
                    if(sendStates[i]==SEND_STATE_UNSENT){
                        byte[] sendData = makeSendData(
                                packetData.get(i),
                                i);

                        System.out.printf("***【%d】号数据包已发送%n",
                                i);

                        sendStates[i]=SEND_STATE_WAIT_FOR_ACK;

                        // 模拟丢包
                        if (random.nextDouble()<lossProbability) {
                            System.out.printf("***【%d】号丢包%n", i);
                        }
                        else {
                            try {
                                socket.send(new DatagramPacket(
                                        sendData,
                                        sendData.length,
                                        InetAddress.getByName("localhost"),
                                        toPort
                                ));
                            } catch (UnknownHostException e) {
                                System.out.println("解析域名出错");
                                e.printStackTrace();
                            } catch (IOException e) {
                                System.out.println("发送数据包出错");
                                e.printStackTrace();
                            }
                        }

                        // 为该包开启定时器
                        System.out.printf("***【%d】号包的计时器开启，超时%dms%n",
                                i,
                                maxDelayMs);
                        int currentPacketIndex = i;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                if(sendStates[currentPacketIndex]==SEND_STATE_WAIT_FOR_ACK){
                                    System.out.printf("***【%d】号包超时，将重新发送%n",
                                            currentPacketIndex);

                                    sendStates[currentPacketIndex]=SEND_STATE_UNSENT;
                                }
                            }
                        },maxDelayMs);
                    }
                }

                DatagramPacket receivePacket = new DatagramPacket(
                        new byte[Protocols0.Constants.ACK_BUFFER_SIZE],
                        Protocols0.Constants.ACK_BUFFER_SIZE
                );

                try {
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    System.out.println("接收数据包出错");
                    e.printStackTrace();
                }

                int receivedAckPacketIndex=getInt32FromByteArray(receivePacket.getData());

                System.out.printf("***收到【%d】号包的ACK，当前sendBase为%d%n",
                        receivedAckPacketIndex,
                        sendBasePacketIndex);

                sendStates[receivedAckPacketIndex]=SEND_STATE_ACK_RECEIVED;

                sendBasePacketIndex=getNextSendBasePacketIndex(
                        sendStates,sendBasePacketIndex
                );

                System.out.printf("***新的sendBase为%d%n%n",sendBasePacketIndex);

                if(sendBasePacketIndex==packetData.size()){
                    break;
                }
            }
//实现双向数据传输
            byte[] finalMessageData=getByteArrayFromInt32(-1);
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
        }
    }

    @Override
    public List<Byte> receivePackets(double lossProbability,
                                     int atPort,
                                     int owtMs){
        List<byte[]> receivedByteArrays=new ArrayList<>();
        List<Boolean> isReceived=new ArrayList<>();
        int receiveBasePacketIndex=0;

        int windowSize=0;

        Random random=new Random();

        try(DatagramSocket socket=new DatagramSocket(atPort)) {
            DatagramPacket receivePacket = new DatagramPacket(
                    new byte[Protocols0.Constants.DATA_BUFFER_SIZE],
                    Protocols0.Constants.DATA_BUFFER_SIZE
            );

            try {
                socket.receive(receivePacket);
                windowSize=getInt32FromByteArray(receivePacket.getData());
            } catch (IOException e) {
                System.out.println("接收窗口大小数据包出错");
                e.printStackTrace();
            }

            System.out.printf("协商窗口大小为：%d%n",windowSize);

            while(true) {
                try {
                    socket.receive(receivePacket);
                } catch (IOException e) {
                    System.out.println("接收数据包出错");
                    e.printStackTrace();
                }

                int receivedPacketIndex=getInt32FromByteArray(receivePacket.getData());

                // 终止信号
                if(receivedPacketIndex==-1){
                    System.out.println("***接收完成");
                    break;
                }

                System.out.printf("***收到【%d】号数据包，大小为%d%n",
                        receivedPacketIndex,
                        receivePacket.getLength());

                boolean shouldSendAck=false;
                byte[] ackData=getByteArrayFromInt32(receivedPacketIndex);

                if(receiveBasePacketIndex<=receivedPacketIndex
                &&receivedPacketIndex<receiveBasePacketIndex+windowSize){
                    if(receivedPacketIndex>=receivedByteArrays.size()){
                        for(int i=receivedByteArrays.size();i<=receivedPacketIndex;i++){
                            receivedByteArrays.add(new byte[0]);
                            isReceived.add(false);
                        }
                    }
                    receivedByteArrays.set(
                            receivedPacketIndex,
                            Arrays.copyOfRange(
                                    receivePacket.getData(),
                                    4,
                                    receivePacket.getLength()));

                    isReceived.set(receivedPacketIndex,true);

                    receiveBasePacketIndex=getNextReceiveBasePacketIndex(
                            isReceived,receiveBasePacketIndex
                    );

                    System.out.println("***接收到的数据包处于[rcvbase, rcvbase+N-1]，已缓存并将发送ACK");
                    shouldSendAck=true;
                }
                else if(receiveBasePacketIndex-windowSize<=receivedPacketIndex
                &&receivedPacketIndex<receiveBasePacketIndex){
                    System.out.println("***接收到的数据包处于[rcvbase-N,rcvbase-1]，将发送ACK");
                    shouldSendAck=true;
                }
                else{
                    System.out.println("***接收到的数据包处于其他区间，将忽略");
                }

                if(!shouldSendAck){
                    continue;
                }

                System.out.printf("***【%d】号ACK已发送%n",receivedPacketIndex);
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

        List<Byte> receivedBytes=new ArrayList<>();
        for(var i:receivedByteArrays){
            for(var j:i){
                receivedBytes.add(j);
            }
        }

        return receivedBytes;
    }
}
