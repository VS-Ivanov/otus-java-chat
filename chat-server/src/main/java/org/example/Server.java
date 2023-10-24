package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private final AuthenticationProvider authenticationProvider;

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        clients = new ArrayList<>();
        this.authenticationProvider = authenticationProvider;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                System.out.println("Сервер запущен на порту " + port);
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastMessage(null,"Клиент: " + clientHandler.getUsername() + " вошел в чат");
    }

    public synchronized void broadcastMessage(ClientHandler sender, String message) {
        for (ClientHandler client : clients) {
            //исключаем возможность отправки самому себе
            if(!Objects.equals(client,sender)) {
                client.sendMessage(message);
            }
        }
    }

    public synchronized void privateMessage(ClientHandler sender, String username, String message) {
        //фильтруем пользователей
        List<ClientHandler> filteredClients = clients.stream()
                .filter(client -> Objects.equals(client.getUsername(),username))
                .collect(Collectors.toList());

        if(filteredClients.isEmpty()){
            sender.sendMessage("Пользователь "+username+" не найден в чате");
            return;
        }

        //отправляем сообщение
        for(ClientHandler client: filteredClients) {
            client.sendMessage("from "+sender.getUsername()+": "+message);
        }
    }

    public synchronized void kickUser(ClientHandler kicker, String username) {
        //сразу проверяем достаточно ли прав
        if(kicker.getUserRole() != UserRole.ADMIN) {
            kicker.sendMessage("У вас должна быть роль ADMIN чтобы удалить пользователя из чата");
            return;
        }

        List<ClientHandler> filteredClients = clients.stream()
                .filter(client -> Objects.equals(client.getUsername(),username))
                .collect(Collectors.toList());

        if(filteredClients.isEmpty()) {
            kicker.sendMessage("Пользователь "+username+" не найден в чате.");
            return;
        }

        for(ClientHandler client: filteredClients) {
            client.sendMessage("Вы удалены из чата пользователем "+kicker.getUsername());
            client.disconnect();
        }

    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage(null,"Клиент: " + clientHandler.getUsername() + " вышел из чата");
    }

    public synchronized List<String> getUserList() {
//        var listUsers = new ArrayList<String>();
//        for (ClientHandler client : clients) {
//            listUsers.add(client.getUsername());
//        }
//        return listUsers;
        return clients.stream()
                .map(ClientHandler::getUsername)
//                .map(client -> client.getUsername())
                .collect(Collectors.toList());
    }
}
