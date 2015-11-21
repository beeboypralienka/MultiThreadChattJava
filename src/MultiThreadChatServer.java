import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author ayya
 */
public class MultiThreadChatServer {

  // The server socket.
  private static ServerSocket serverSocket = null;
  // The client socket.
  private static Socket clientSocket = null;

  // This chat server can accept up to maxClientsCount clients' connections.
  private static final int maxClientsCount = 50;
  public static final clientThread[] threads = new clientThread[maxClientsCount]; ///ada di kelas utama

  public static clientThreadPool publicRoom = new clientThreadPool(0);

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

        clientThread athread = new clientThread(clientSocket, publicRoom);
        publicRoom.addClientThread(athread);
        athread.start();

      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}

/*
 Datas
*/
class chatData {
}

/*
 Client Thread Pool == Room
 http://stackoverflow.com/questions/1647260/java-dynamic-array-sizes
 */
class clientThreadPool {

  private final ArrayList<clientThread> threads;
  private final int poolKind;

  public clientThreadPool(int akind) {
    this.threads = new ArrayList<>();
    this.poolKind = akind;
  }

  public void removeClientThread(clientThread athread) {
    if (threads.indexOf(athread) < 0) {
      threads.remove(athread);
    }
    System.out.println("remove thread");
  }

  public void addClientThread(clientThread athread) {
    if (threads.indexOf(athread) < 0) {
      threads.add(athread);
    }
    System.out.println("add thread");
  }

  public void addToOtherPool(clientThreadPool apool, String aname) {
    clientThread ret = searchThreadByName(aname);
    if (ret == null) {
      return;
    }
    apool.addClientThread(ret);
  }

  public clientThread searchThreadByName(String aname) {
    clientThread ret = null;
    clientThread aret;

    Iterator<clientThread> foreach = threads.iterator();
    while (foreach.hasNext()) {
      aret = foreach.next();
      if (aret.usern == aname) {
        ret = aret;
        break;
      }
    }//while

    return ret;
  }

  public void sendMessageToAll(String msg) {
    clientThread athread;
    Iterator<clientThread> foreach = threads.iterator();
    while (foreach.hasNext()) {
      athread = foreach.next();
      athread.os.println(msg);
    }//while
  }

  public void sendMessageToAll_exceptMe(String msg, clientThread bthread) {
    clientThread athread;
    Iterator<clientThread> foreach = threads.iterator();
    while (foreach.hasNext()) {
      athread = foreach.next();
      if (athread != bthread) {
        athread.os.println(msg);
      }
    }//while
  }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. When a client leaves the chat room this thread informs also
 * all the clients about that and terminates.
 */
class clientThread extends Thread {

  public BufferedReader is = null;
  //private DataInputStream is = null;
  public PrintStream os = null;
  public Socket clientSocket = null;

  private clientThread[] threads;
  private MultiThreadChatServer schat = null;

  public clientThreadPool pool = null;

  public String usern;
  public String passw;
  public String mode;

  public clientThread(Socket clientSocket, clientThread[] threads) {
    this.clientSocket = clientSocket;
    this.threads = threads;
    this.mode = "chat";
  }

  public clientThread(Socket clientSocket, clientThreadPool apool) {
    this.clientSocket = clientSocket;
    this.pool = apool;
    this.mode = "chat";
    System.out.println("thread created");
  }

  public void run() {

    try {
      /*
       * Create input and output streams for this client.
       */
      //is = new DataInputStream(clientSocket.getInputStream()); //bi-directional link
      is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      os = new PrintStream(clientSocket.getOutputStream());
      System.out.println("request user+passw");

      os.println("Enter your name.");
      String name = is.readLine().trim();
      os.println("Enter your password.");
      String passw = is.readLine().trim();
      this.usern = name;
      this.passw = passw;
      
      os.println("Hello " + name + " to our chat room.\nTo leave enter #quit in a new line");

      String amsg;
      amsg = "*** A new user " + name + " entered the chat room !!! ***";
      pool.sendMessageToAll_exceptMe(amsg, this);

      while (true) {
        String line = is.readLine();
        if (line.startsWith("#quit")) {
          break;
        }

        // send to all users in room, repeat message from a user to all user
        amsg = "<" + usern + "> " + line;
        pool.sendMessageToAll(amsg);
      }

      // notification to all user
      amsg = "*** The user " + name + " is leaving the chat room !!! ***";
      pool.sendMessageToAll_exceptMe(amsg, this);

      os.println("*** Bye " + name + " ***");

      /*
       * Clean up. Set the current thread variable to null so that a new client
       * could be accepted by the server.
       */
      pool.removeClientThread(this);

      /*
       * Close the output stream, close the input stream, close the socket.
       */
      is.close();
      os.close();
      clientSocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
