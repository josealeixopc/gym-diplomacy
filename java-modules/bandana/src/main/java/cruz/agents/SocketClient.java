package cruz.agents;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

class SocketClient
{
    private static Socket socket;
    private int port;
    private DataOutputStream out;
    private DataInputStream in;

    SocketClient(int port)
    {
        this.port = port;
        this.createSocket();
    }

    private void createSocket() {
        InetAddress address;
        try {
            address = InetAddress.getByName("localhost");
            socket = new Socket(address, port);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    byte[] sendMessageAndReceiveResponse(byte[] messageToSend){
        try {
            //Send the message to the server
            this.out.writeInt(messageToSend.length);
            out.flush();

            this.out.write(messageToSend);
            out.flush();

            //Get the response message from the server
            int length = in.readInt();                    // read length of incoming message

            byte[] buffer = null;
            if(length>=0) {
                buffer = new byte[length];
                in.readFully(buffer, 0, buffer.length); // read the message
            }

            return buffer;
        }
        catch (ConnectException exception) {
            System.out.println("ATTENTION! Could not connect to socket. No information was retrieved from the Python module.");
            exception.printStackTrace();
            return null;
        }
        catch (Exception exception)
        {
            System.out.println("ATTENTION! Something went wrong while communicating with the Python module.");
            exception.printStackTrace();
            return null;
        }
    }

    void close(){
        //Closing the socket
        try
        {
            in.close();
            out.close();
            socket.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        // Testing
        SocketClient socketClient = new SocketClient(5000);

        byte[] response;

        response = socketClient.sendMessageAndReceiveResponse("a2345678".getBytes());
        System.out.println(new String(response));

        response = socketClient.sendMessageAndReceiveResponse("a234567812345".getBytes());
        System.out.println(new String(response));

        response = socketClient.sendMessageAndReceiveResponse("a12345678123456781".getBytes());
        System.out.println(new String(response));


        socketClient.close();
    }
}

