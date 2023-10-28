package main.java;
import java.io.FileWriter;
import java.io.IOException;
import controllers.ReceiverController;
import controllers.TransmitterController;

public class RdtDemo {
    public static void main(String[] args) {
        TransmitterController transmitterController=new TransmitterController(8897,8895);
         ReceiverController receiverController=new ReceiverController(5965);

    //transmitterController.sendWithRdt("Ernest", 0.3,32,2000);
    //transmitterController.sendWithGbn("Je Sun",0.2,1,30,2000);//停等协议
        //Gbn
       //transmitterController.sendWithGbn("GBN",0.2,5,30,2000);
      // receiverController.receiveFileWithGbn("D:/recvgbn.txt",23);
        //GBN双向数据传输
      transmitterController.sendWithGbn("双向",0.2,5,30,2000);
        transmitterController.sendFileWithGbn("D:/jane-eyre.txt",50,20,10000);
        receiverController.receiveFileWithGbn("D:/recv.txt",23);

        //SR
        transmitterController.sendFileWithSr("D:/jane-eyre.txt",50,20,10000);
       receiverController.receiveFileWithSr("D:/recvSRA.txt",23);




    }
}
