package ecommerce.utils;

import java.util.ResourceBundle;

public class Msg {
    private static final ResourceBundle B = ResourceBundle.getBundle("messages");

    private Msg() {
    }

    public static String get(String key) {
        return B.getString(key);
    }
}
