import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FrontEnd {
    JFrame mainFrame = new JFrame();
    AtomicBoolean loggedIn = new AtomicBoolean(false);
    Container loginContainer;
    public Container loginContainer(){
        Login login = new Login();

        Container container = new Container();
        container.setLayout(new GridLayout(3, 2));
        JLabel usernameLabel = new JLabel("Username");
        JTextField usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
                    String username = usernameField.getText();
                    String password = new String(passwordField.getPassword());
            loggedIn.set(login.authenticate(username, password));
            showFrame();
                });
        container.add(usernameLabel);
        container.add(usernameField);
        container.add(passwordLabel);
        container.add(passwordField);
        container.add(loginButton);
        return container;
    }

    public void showFrame() {
        String frameTitle = loggedIn.get() ? "Welcome" : "Hello, World!";
        mainFrame.setTitle(frameTitle);

        if (!loggedIn.get()) {
            loginContainer = loginContainer();
            mainFrame.add(loginContainer);
        }
        else
            mainFrame.remove(loginContainer);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(300, 300);
        mainFrame.setResizable(false);
        mainFrame.setVisible(true);
    }
}
