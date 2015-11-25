package SocketProgramming;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Dhian Satria, Dwi Kristianto, Fachrul Pralienka, Hendra Maulana
 */
public class ClientThreadPool {

    private final ArrayList<ClientThread> threads;
    private final int poolKind;

    public ClientThreadPool(int akind) {
        this.threads = new ArrayList<>();
        this.poolKind = akind;
    }

    public void removeClientThread(ClientThread athread) {
        if (threads.indexOf(athread) >= 0) {
            threads.remove(athread);
        }
        System.out.println("remove thread");
    }

    public void addClientThread(ClientThread athread) {
        if (threads.indexOf(athread) < 0) {
            threads.add(athread);
        }
        System.out.println("add thread");
    }

    public void addToOtherPool(ClientThreadPool apool, String aname) {
        ClientThread ret = searchThreadByName(aname);
        if (ret == null) {
            return;
        }
        apool.addClientThread(ret);
    }

    public ClientThread searchThreadByName(String aname) {
        ClientThread ret = null;
        ClientThread aret = null;

        aname = aname.trim();
        String bname = "";

        Iterator<ClientThread> foreach = threads.iterator();
        if (threads.size() > 0) {
            while (foreach.hasNext()) {
                aret = foreach.next();
                if (aret == null) {
                    continue;
                }

                bname = aret.usern;
                if (bname == null) {
                    continue;
                }

                bname = bname.trim();
                if (bname.equals(aname)) {
                    ret = aret;
                    break;
                }
            }//while
        }
        return ret;
    }

    public Boolean isUserNameExists(String aname) {
        return this.searchThreadByName(aname) != null;
    }

    public void sendMessageToAll(String msg) {
        ClientThread athread;
        Iterator<ClientThread> foreach = threads.iterator();
        while (foreach.hasNext()) {
            athread = foreach.next();
            athread.os.println(msg);
        }//while
    }

    public void sendMessageToAll_exceptMe(String msg, ClientThread bthread) {
        ClientThread athread;
        Iterator<ClientThread> foreach = threads.iterator();
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
        ClientThread athread;
        Iterator<ClientThread> foreach = threads.iterator();
        while (foreach.hasNext()) {
            athread = foreach.next();
            if (athread == null) {
                continue;
            }

            buser = athread.usern;
            if (buser == null) {
                continue;
            }

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
        ClientThread athread;
        Iterator<ClientThread> foreach = threads.iterator();
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
        ClientThread athread = this.searchThreadByName(ausern);
        if (athread == null) {
            System.out.println("delUser (notfound): " + ausern);
            return;
        }

        System.out.println("delUser: " + ausern);
        this.removeClientThread(athread);
        athread.os.println("#die");
        athread.doDie();
    }

    public void sendPM(String uname, String msg, String sender) {
        if (!this.isUserNameExists(uname)) {
            return;
        }

        ClientThread athread = this.searchThreadByName(uname);
        if (athread != null) {
            athread.os.println("PM from " + sender + ": " + msg);
        }
    }

    public void enterRoom(String nama, ClientThread bthread) {
        
    }

}
