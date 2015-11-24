package SocketProgramming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Dhian Satria, Dwi Kristianto, Fachrul Pralienka, Hendra Maulana
 */
public class ClientThread extends Thread {

    public BufferedReader is = null;
    public PrintStream os = null;
    public Socket clientSocket = null;

    public ClientThreadPool pool = null;

    public String usern;
    public String passw;
    public String mode;
    public String status;
    public Boolean visible;
    public Boolean running;

    public ClientThread(Socket clientSocket, ClientThreadPool apool) {
        this.clientSocket = clientSocket;
        this.pool = apool;
        this.mode = "chat";
        this.visible = true;
        this.status = "";
        this.running = true;
        System.out.println("thread created");
    }

    public ClientThread(MultiThreadChatServer mts) {
        this.clientSocket = mts.clientSocket;
        this.pool = mts.publicRoom;
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

            os.println("Hello " + name + " welcome to our chat room.\nTo leave enter #quit in a new line");

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
                            pool.sendPM(uname, messg, usern);
                        }
                    }//

                } else if(line.startsWith("#join")){
                    
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
            //e.printStackTrace(); Sengaja dikosongin
        }//try
    }
}
