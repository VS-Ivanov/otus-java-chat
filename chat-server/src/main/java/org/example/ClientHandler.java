package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler {
    private Socket socket;

    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;

    private static int userCount = 0;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        username = "User" + userCount++;
        new Thread(() -> {
            try {
                authenticateUser(server);
                communicateWithUser(server);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    private void authenticateUser(Server server) throws IOException {
        boolean isAuthenticated = false;
        while (!isAuthenticated) {
            String message = in.readUTF();
//            /auth login password
//            /register login nick password
            String[] args = message.split(" ");
            String command = args[0];
            switch (command) {
                case "/auth": {
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (username == null || username.isBlank()) {
                        sendMessage("Указан неверный логин/пароль");
                    } else {
                        this.username = username;
                        sendMessage(username + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                case "/register": {
                    String login = args[1];
                    String nick = args[2];
                    String password = args[3];
                    boolean isRegistred = server.getAuthenticationProvider().register(login, password, nick);
                    if (!isRegistred) {
                        sendMessage("Указанный логин/никнейм уже заняты");
                    } else {
                        this.username = nick;
                        sendMessage(nick + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                default: {
                    sendMessage("Авторизуйтесь сперва");
                }
            }
        }
    }

    private void communicateWithUser(Server server) throws IOException {
        while (true) {
            // /exit -> disconnect()
            // /w user message -> user

            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                } else if (message.equals("/list")) {
                    List<String> userList = server.getUserList();
                    String joinedUsers =
                            String.join(", ", userList);
//                            userList.stream().collect(Collectors.joining(","));
                    sendMessage(joinedUsers);
                } else if (message.startsWith("/w ")) {
                    //находим пользователя в чате и отправляем ему сообщение
                    String[] parsedMsg = message.split(" ",3);
                    if(server.getUserList().contains(parsedMsg[1])) {
                        server.privateMessage(parsedMsg[1], "from " + username + ": " + parsedMsg[2]);
                    } else {
                        server.privateMessage(username, "from Server: User " + parsedMsg[1] +  " not found in chat!");
                    }
                }
            } else {
                server.broadcastMessage(this,username+": " + message);
            }
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }
}
