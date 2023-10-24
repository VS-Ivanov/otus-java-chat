package org.example;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    public static void main(String[] args) {
        // переключаемся на авторизацию через sqlite
        //Server server = new Server(8888, new InMemoryAuthenticationProvider());
        Server server = new Server(8888, new InSqlAuthenticationProvider());
        server.start();
    }
}
