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
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    clientThread aret = null;

    aname = aname.trim();
    String bname = "";

    Iterator<clientThread> foreach = threads.iterator();
    if (threads.size() > 0)
    while (foreach.hasNext()) {
      aret = foreach.next();
      if (aret == null) continue;

      bname = aret.usern;
      if (bname == null) continue;

      bname = bname.trim();
      if (bname.equals(aname)) {
        ret = aret;
        break;
      }
    }//while

    return ret;
  }

  public Boolean isUserNameExists(String aname) {
    return this.searchThreadByName(aname) != null;
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

  public String getUserStatus(String auser) {
    String aret = "--none--";
    String buser = "";
    clientThread athread;
    Iterator<clientThread> foreach = threads.iterator();
    while (foreach.hasNext()) {
      athread = foreach.next();
      if (athread == null) continue;

      buser = athread.usern;
      if (buser == null) continue;

      buser = buser.trim();
      if (buser.equals(auser)) {
        aret = auser + " status: " + athread.status;
        break;
      }
    }//while
    return aret;
  }

  public String getUserList() {
    ArrayList<String> list = new ArrayList<String>();
    clientThread athread;
    Iterator<clientThread> foreach = threads.iterator();
    while (foreach.hasNext()) {
      athread = foreach.next();
      if (athread.visible) {
        list.add(athread.usern);
      }
    }//while

    String joined = list.toString().replaceAll("\\[|\\]", "").trim();
    if (joined.length() < 1) {
      joined = "---empty---";
    }
    return joined;
  }

  public void deleteUser(String ausern) {
    clientThread athread = this.searchThreadByName(ausern);
    if (athread == null) {
      System.out.println("delUser (notfound): " + ausern);
      return;
    }

    System.out.println("delUser: " + ausern);
    this.removeClientThread(athread);
    athread.os.println("#die");
    athread.doDie();
  }

  public void sendPM(String uname, String msg) {
    if (!this.isUserNameExists(uname)) {
      return;
    }

    clientThread athread = this.searchThreadByName(uname);
    if (athread != null) {
      athread.os.println("PM from " + uname + ": " + msg);
    }
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
  public PrintStream os = null;
  public Socket clientSocket = null;

  public clientThreadPool pool = null;

  public String usern;
  public String passw;
  public String mode;
  public String status;
  public Boolean visible;
  public Boolean running;

  public clientThread(Socket clientSocket, clientThreadPool apool) {
    this.clientSocket = clientSocket;
    this.pool = apool;
    this.mode = "chat";
    this.visible = true;
    this.status = "";
    this.running = true;
    System.out.println("thread created");
  }

  public void doDie() {
    os.println("#die");
    System.out.println("send #die to: " + this.usern);
    this.running = false;
    this.interrupt();
    pool.removeClientThread(this);
  }

  @Override
  public void run() {

    try {
      /*
       * Create input and output streams for this client.
       */
      is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      os = new PrintStream(clientSocket.getOutputStream());

      System.out.println("request user+passw");
      String name;
      while (true) {
        os.println("Enter your name.");
        name = is.readLine().trim();
        if (!pool.isUserNameExists(name)) {
          os.println("USER OK");
          break;
        } else {
          os.println("User " + name + " already exists..");
        }
      }
      os.println("Enter your password.");
      String passw = is.readLine().trim();
      this.usern = name;
      this.passw = passw;

      os.println("Hello " + name + " to our chat room.\nTo leave enter #quit in a new line");

      String amsg;
      amsg = "*** A new user " + name + " entered the chat room !!! ***";
      pool.sendMessageToAll_exceptMe(amsg, this);

      while (this.running && !Thread.currentThread().isInterrupted()) {

        if (is == null || os == null || clientSocket == null || pool == null) {
          break;//
        }

        String line = is.readLine().trim();
        if (line.startsWith("#quit")) {
          break;
        } else if (line.startsWith("#date")) {
          DateFormat dateFormat = new SimpleDateFormat("dd-MMMM-yyyy HH:mm:ss");
          Date date = new Date();
          os.println(dateFormat.format(date));
        } else if (line.startsWith("#list")) {
          os.println(this.pool.getUserList());
        } else if (line.startsWith("#hideme")) {
          this.visible = false;
          os.println("Visible changed to: " + this.visible);
          pool.sendMessageToAll("A user hide from room");
        } else if (line.startsWith("#showme")) {
          this.visible = true;
          os.println("Visible changed to: " + this.visible);
          pool.sendMessageToAll("A user visible at room");
        } else if (line.startsWith("#show")) {
          String auser = line.replaceAll("#show@", ""); //get username
          os.println(this.pool.getUserStatus(auser));
        } else if (line.startsWith("#status")) {
          this.status = line.replaceAll("#status:", "").replaceAll("#status", "");
          os.println("Status changed to: " + this.status);
          pool.sendMessageToAll_exceptMe(this.usern + " status changed to: " + this.status, this);
        } else if (line.startsWith("#del")) {
          String auser = line.replaceAll("#del@", ""); //get username
          pool.deleteUser(auser);
        } else if (line.startsWith("#pm")) {
          line = line.replaceAll("#pm", "");
          String uname = "";
          String messg = "";

          if (line.indexOf(":") > 0) { // valid command, separated by ":"
            String[] parts = line.split(":");
            uname = parts[0].replaceAll("@", "");
            messg = line.replaceAll("@" + uname + ":", "").trim();
          } else { // not valid command, no ":"
            String[] parts = line.split(" ");
            uname = parts[0].replaceAll("@", "");
            messg = line.replaceAll("@" + uname, "").trim();
          }

          if (uname.length() < 1) {
            os.println("ERROR: invalid command, empty user name");
          } else {
            if (!pool.isUserNameExists(uname)) {
              os.println("ERROR: " + uname + " not exists");
            } else {
              pool.sendPM(uname, messg);
            }
          }//

        } else {
          // send to all users in room, repeat message from a user to all user
          pool.sendMessageToAll("<" + usern + "> " + line);
        }
      }//while

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
    }//try
  }
}
