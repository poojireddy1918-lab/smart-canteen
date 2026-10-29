package com.canteen.ui;

import com.canteen.MainApp;
import com.canteen.dao.UserDAO;
import com.canteen.model.User;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class LoginView {

    public Parent build() {
        TabPane tabs = new TabPane(buildLoginTab(), buildRegisterTab());
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Label title = new Label("Smart Canteen Token System");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        VBox root = new VBox(14, title, tabs);
        root.setPadding(new Insets(20));
        return root;
    }

    private Tab buildLoginTab() {
        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password");
        Button login = new Button("Login");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setDefaultButton(true);

        login.setOnAction(e -> {
            if (email.getText().isBlank() || pass.getText().isEmpty()) {
                Ui.error("Enter email and password.");
                return;
            }
            try {
                User u = UserDAO.login(email.getText().trim(), pass.getText());
                if (u == null) {
                    Ui.error("Invalid email or password.");
                    return;
                }
                MainApp.currentUser = u;
                MainApp.showHome();
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
        });

        VBox box = new VBox(10, new Label("Email"), email, new Label("Password"), pass, login);
        box.setPadding(new Insets(16));
        return new Tab("Login", box);
    }

    private Tab buildRegisterTab() {
        TextField name = new TextField();
        name.setPromptText("Full name");
        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField pass = new PasswordField();
        pass.setPromptText("Password (min 6 characters)");
        Button register = new Button("Create student account");
        register.setMaxWidth(Double.MAX_VALUE);

        register.setOnAction(e -> {
            String n = name.getText().trim();
            String m = email.getText().trim();
            String p = pass.getText();
            if (n.isEmpty() || m.isEmpty() || p.isEmpty()) {
                Ui.error("All fields are required.");
                return;
            }
            if (!m.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                Ui.error("Enter a valid email address.");
                return;
            }
            if (p.length() < 6) {
                Ui.error("Password must be at least 6 characters.");
                return;
            }
            try {
                if (UserDAO.register(n, m, p, "STUDENT")) {
                    Ui.info("Account created. You can log in now.");
                    name.clear();
                    email.clear();
                    pass.clear();
                } else {
                    Ui.error("That email is already registered.");
                }
            } catch (SQLException ex) {
                Ui.error("Database error: " + ex.getMessage());
            }
        });

        VBox box = new VBox(10, new Label("Name"), name, new Label("Email"), email,
                new Label("Password"), pass, register);
        box.setPadding(new Insets(16));
        return new Tab("Register", box);
    }
}
