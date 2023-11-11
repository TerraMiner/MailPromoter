package ua.terra;

import lombok.SneakyThrows;




public class Main {
    public static String sessionUser;
    public static String sessionPassword;

    public static void main(String[] args) {
        registerClass();

        sessionUser = System.getProperty("login");
        sessionPassword = System.getProperty("password");

        Window.open();
    }

    @SneakyThrows
    private static void registerClass() {
        Class.forName("com.mysql.cj.jdbc.Driver");
    }
}

