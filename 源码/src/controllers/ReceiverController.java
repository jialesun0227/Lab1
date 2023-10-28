package controllers;

import Protocols0.*;
import Protocols0.GbnTransceiver;
import Protocols0.SlidingWindowTransceiver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ReceiverController {
    private final RdtTransceiver rdtTransceiver;
    private final SlidingWindowTransceiver gbnTransceiver;
    private final SlidingWindowTransceiver srTransceiver;

    private final int atPort;

    private void printReceivedByteList(List<Byte> bytes) {
        System.out.println("接收到的数据为：");
        for (var i : bytes) {
            System.out.printf("%c", i);
        }
    }

    public ReceiverController(int atPort) {
        this.atPort = atPort;
        this.rdtTransceiver = new RdtTransceiver();
        this.gbnTransceiver = new GbnTransceiver();
        this.srTransceiver = new SrTransceiver();
    }

    public void receiveWithRdt(double lossProbability, int owtMs) {
        var receivedData =
                rdtTransceiver.receivePackets(lossProbability, atPort, owtMs);
        printReceivedByteList(receivedData);
    }

    public void receiveWithGbn(double lossProbability,
                               int owtMs) {
        var receivedData =
                gbnTransceiver.receivePackets(
                        lossProbability, atPort, owtMs);
        printReceivedByteList(receivedData);
    }

    public void receiveWithSr(double lossProbability,
                              int owtMs) {
        var receivedData =
                srTransceiver.receivePackets(
                        lossProbability, atPort, owtMs);
        printReceivedByteList(receivedData);
    }

    public void receiveFileWithSr(String fileName,
                                  int owtMs) {
        var receivedData =
                srTransceiver.receivePackets(
                        0, atPort, owtMs);

        byte[] bytes=new byte[receivedData.size()];

        for(int i=0;i<receivedData.size();i++){
            bytes[i]=receivedData.get(i);
        }

        try {
            Files.write(Path.of(fileName),bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveFileWithGbn(String fileName,
                                  int owtMs) {
        var receivedData =
                gbnTransceiver.receivePackets(
                        0, atPort, owtMs);

        byte[] bytes=new byte[receivedData.size()];

        for(int i=0;i<receivedData.size();i++){
            bytes[i]=receivedData.get(i);
        }

        try {
            Files.write(Path.of(fileName),bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
