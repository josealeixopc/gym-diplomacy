package cruz.agents;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class SocketClient
{
    private static Socket socket;
    private String host;
    private int port;


    public static void main(String[] args) {
        SocketClient c = new SocketClient("127.0.1.1", 5000);
        System.out.println(Arrays.toString(c.sendMessageAndReceiveResponse("Hello".getBytes())));
    }

    SocketClient(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public byte[] sendMessageAndReceiveResponse(byte[] messageToSend){
        try {
            InetAddress address = InetAddress.getByName(host);
            socket = new Socket(address, port);

            //Send the message to the server
            OutputStream os = socket.getOutputStream();
            os.write(messageToSend);
            os.flush();

            System.out.println("Sending message");

            System.out.println("Sent message. Waiting for response...");

            //Get the return message from the server
            InputStream is = socket.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 20];

            while(true)
            {
                int n = is.read(buffer);

                if(n < 0) {
                    break;
                }

                System.out.println("Received message with size: " + n);

                baos.write(buffer, 0, n);
            }

            System.out.println("Got response:");
            System.out.println(Arrays.toString(baos.toByteArray()));

            closeSocket();
            return baos.toByteArray();
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    private boolean closeSocket(){
        //Closing the socket
        try
        {
            socket.close();
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}