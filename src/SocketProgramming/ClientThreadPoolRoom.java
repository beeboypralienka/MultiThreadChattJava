/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SocketProgramming;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author Dhian Satria, Dwi Kristianto, Fachrul Pralienka, Hendra Maulana
 */
public class ClientThreadPoolRoom {

    private static ArrayList<ClientThreadPool> rooms = new ArrayList<>();

    public ClientThreadPool addNewRoom(String roomname) {
        if (this.isRoomNameExists(roomname)) {
            return null;
        }

        ClientThreadPool ret = new ClientThreadPool(1);
        ret.name = roomname;
        return ret;
    }

    public void removeRoom(String roomname) {
        this.removeClientThreadPool(this.searchClientThreadPool(roomname));
    }

    protected void removeClientThreadPool(ClientThreadPool apool) {
        if (apool != null) {
            String aroom = apool.name;
            rooms.remove(apool);
            System.out.println("remove room: " + aroom);
        }
    }

    protected ClientThreadPool searchClientThreadPool(String aname) {
        ClientThreadPool ret = null;
        ClientThreadPool aret = null;

        aname = aname.trim();
        String bname = "";

        Iterator<ClientThreadPool> foreach = rooms.iterator();
        if (rooms.size() > 0) {
            while (foreach.hasNext()) {
                aret = foreach.next();
                if (aret == null) {
                    continue;
                }

                bname = aret.name;
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

    public Boolean isRoomNameExists(String aname) {
        return this.searchClientThreadPool(aname) != null;
    }

    public void sendMessage(String roomName, String msg, ClientThread meThread) {
        ClientThreadPool apool;
        apool = this.searchClientThreadPool(roomName);
        if (apool != null) {
            apool.sendMessageToAll_exceptMe(msg, meThread);
        }
    }

}
