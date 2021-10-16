package Application;

import Chat.Properties;
import Models.User;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author huert
 */
public class Server {

    private static MulticastSocket server;
    private static List<User> users;

    static private class ReceiveClients extends Thread {

        private final MulticastSocket socket;
        private DatagramPacket packet;
        private byte[] buffer;

        public ReceiveClients(MulticastSocket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                for (;;) {
                    buffer = new byte[65535];
                    packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    users.add(new User(new String(packet.getData(), 0,
                            packet.getLength()), socket.getRemoteSocketAddress()));
                    System.out.println("User " + users.get(users.size() - 1) + " online");
                }
            } catch (Exception ex) {
                System.out.println("Cannot receive client: " + ex.getMessage());
            }
        }
    }

    static private class AnnounceClients extends Thread {

        private final MulticastSocket socket;
        private DatagramPacket packet;
        private ByteArrayOutputStream baos;
        private ObjectOutputStream oos;
        private byte[] buffer;

        public AnnounceClients(MulticastSocket socket) {
            this.socket = socket;
        }

        public void run() {
            try{
                users.forEach(user -> {
                    try {
                        baos = new ByteArrayOutputStream();
                        oos = new ObjectOutputStream(baos);
                        oos.writeObject(user);
                        oos.flush();
                        buffer = baos.toByteArray();
                        packet = new DatagramPacket(buffer, buffer.length,
                                InetAddress.getByName(Properties.GROUP_IP),
                                Properties.CLIENTS_PORT);
                        socket.send(packet);
                    } catch (Exception ex) {
                        System.out.println("Cannot announce client: " + ex.getMessage());
                    }
                });
                Thread.sleep(7000);
            } catch (Exception ex) {
                System.out.println("Thread sleep exception at "+this.getClass().getName());
            }
        }
    }

    public static void main(String[] args) {
        users = new ArrayList<>();
        try {
            server = new MulticastSocket(Properties.SERVER_PORT);
            server.setReuseAddress(true);
            server.setTimeToLive(225);
            Properties.socketJoinGroup(server, InetAddress.getByName(
                    Properties.GROUP_IP), false);
            ReceiveClients receiveClients = new ReceiveClients(server);
            AnnounceClients announceClients = new AnnounceClients(server);
            receiveClients.setPriority(1);
            receiveClients.start();
            announceClients.start();
            receiveClients.join();
            announceClients.join();
        } catch (Exception ex) {
            System.out.println("Cannot run server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}