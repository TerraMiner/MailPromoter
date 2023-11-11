package ua.terra;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Logger extends JTextArea {
    public void print(Object text) {
        append(text.toString() + "\n");
        setCaretPosition(getDocument().getLength());
    }

    public void printStackTrace(Exception e) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            String stackTrace = sw.toString();
            print(stackTrace);
        } catch (IOException ignored) {
        }
    }
}
