
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiThreadChatClient implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static BufferedReader is = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void main(String[] args) {

        // The default port.
        int portNumber = 2222;
        // The default host.
        String host = "localhost";

        if (args.length < 2) {
            System.out
                    .println("Usage: java MultiThreadChatClient <host> <portNumber>\n"
                            + "Now using host=" + host + ", portNumber=" + portNumber);
        } else {
            host = args[0];
            portNumber = Integer.valueOf(args[1]).intValue();
        }

        try {
            clientSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host " + host);
        }

        /*
         * If everything has been initialized then we want to write some data to the
         * socket we have opened a connection to on the port portNumber.
         */
        if (clientSocket != null && os != null && is != null) {
            try {
                /* Create a thread to read from the server. */
                new Thread(new MultiThreadChatClient()).start();

                //
                String msg;
                while (!closed) {
                    msg = inputLine.readLine().trim();
                    if (msg.length() > 0) {
                        os.println(msg);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(MultiThreadChatClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void run() {
        String responseLine;
        try {
            while (!closed && (responseLine = is.readLine()) != null) {
                if (responseLine.startsWith("#die") || responseLine.startsWith("#bye") || responseLine.startsWith("*** Bye")) {
                    System.out.println("#die command received..");
                    System.exit(0);
                    break;
                }

                System.out.println(responseLine);
                
            }
            closed = true;
            inputLine.close();            
        } catch (IOException e) {
            //System.err.println("IOException:  " + e);
            //e.printStackTrace();
        }
    }
}
