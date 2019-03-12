package cruz.agents;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

public class SocketClient
{
    private static Socket socket;
    private String host;
    private int port;


    public static void main(String[] args) {
        SocketClient c = new SocketClient("127.0.1.1", 5000);
        System.out.println(c.sendMessageAndReceiveResponse("Hello"));
    }

    SocketClient(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    public String sendMessageAndReceiveResponse(String messageToSend){
        try {
            InetAddress address = InetAddress.getByName(host);
            socket = new Socket(address, port);

            //Send the message to the server
            OutputStream os = socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter bw = new BufferedWriter(osw);

            String completeMessage = messageToSend + "\n";
            bw.write(completeMessage);
            bw.flush();
            System.out.println("Message sent to the server : '" + messageToSend + "'.");

            //Get the return message from the server
            InputStream is = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String message = br.readLine();
            closeSocket();
            return message;
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