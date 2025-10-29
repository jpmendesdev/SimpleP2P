import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SimplePeer {
    private Set<String> knownPeers = new CopyOnWriteArraySet<>();
    private int myPort;

    public SimplePeer(int myPort) {
        this.myPort = myPort;
    }

    public void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(myPort)) {
                System.out.println("Peer ouvindo na porta " + myPort);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new PeerHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("Erro no servidor: " + e.getMessage());
            }
        }).start();
    }

    private class PeerHandler implements Runnable {
        private Socket socket;

        public PeerHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("\n[Mensagem Recebida de " + socket.getRemoteSocketAddress() + "]: " + message);
                    System.out.print("> ");
                }
            } catch (IOException e) {
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void startUserInput() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bem-vindo ao Chat P2P!");
        System.out.println("Comandos:");
        System.out.println("  connect [host] [porta] - Conecta a um novo peer");
        System.out.println("  broadcast [mensagem]   - Envia mensagem para todos os peers conhecidos");
        System.out.println("  exit                     - Sai do chat");

        System.out.print("> ");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split(" ", 3);
            String command = parts[0];

            switch (command) {
                case "connect":
                    if (parts.length == 3) {
                        String host = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        String peerAddress = host + ":" + port;
                        knownPeers.add(peerAddress);
                        System.out.println("Peer " + peerAddress + " adicionado.");
                    } else {
                        System.out.println("Uso: connect [host] [porta]");
                    }
                    break;
                case "broadcast":
                    if (parts.length == 2) {
                        broadcastMessage(parts[1]);
                    } else {
                        System.out.println("Uso: broadcast [mensagem]");
                    }
                    break;
                case "exit":
                    System.out.println("Saindo...");
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Comando desconhecido.");
            }
            System.out.print("> ");
        }
    }

    public void broadcastMessage(String message) {
        if (knownPeers.isEmpty()) {
            System.out.println("Nenhum peer conhecido para enviar a mensagem.");
            return;
        }
        System.out.println("Enviando '" + message + "' para " + knownPeers.size() + " peer(s)...");

        for (String peerAddress : knownPeers) {
            String[] parts = peerAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            new Thread(() -> {
                try (Socket socket = new Socket(host, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                    out.println(message);

                } catch (IOException e) {
                    System.err.println("Não foi possível conectar ao peer " + peerAddress);
                }
            }).start();
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java SimplePeer [minhaPorta]");
            return;
        }

        int myPort = Integer.parseInt(args[0]);
        SimplePeer peer = new SimplePeer(myPort);
        peer.startServer();
        peer.startUserInput();
    }
}