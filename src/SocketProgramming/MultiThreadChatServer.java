package SocketProgramming;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Dhian Satria, Dwi Kristianto, Fachrul Pralienka, Hendra Maulana
 */
public class MultiThreadChatServer {

  // The server socket.
  private static ServerSocket serverSocket = null;
  // The client socket.
  private static Socket clientSocket = null;

  public static ClientThreadPool publicRoom = new ClientThreadPool(0);
  public static ClientThreadPoolRoom privateRooms = new ClientThreadPoolRoom();

  public static void main(String args[]) {

    // The default port number.
    int portNumber = 2222;
    if (args.length < 1) {
      System.out.println("Usage: java MultiThreadChatServer <portNumber>\n"
              + "Now using port number=" + portNumber);
    } else {
      portNumber = Integer.valueOf(args[0]).intValue();
    }

    /*
     * Open a server socket on the portNumber (default 2222). Note that we can
     * not choose a port less than 1023 if we are not privileged users (root).
     */
    try {
      serverSocket = new ServerSocket(portNumber);
      System.out.println("server socket created..");
    } catch (IOException e) {
      System.out.println(e);
    }

    /*
     * Create a client socket for each connection and pass it to a new client
     * thread.
     */
    while (true) {
      try {
        clientSocket = serverSocket.accept();

        ClientThread athread = new ClientThread(clientSocket, publicRoom, privateRooms);
        publicRoom.addClientThread(athread);
        athread.start();

      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}
