package com.canteen;

import com.canteen.dao.UserDAO;
import com.canteen.db.DBConnection;
import com.canteen.model.User;
import com.canteen.service.TokenQueue;
import com.canteen.ui.AdminView;
import com.canteen.ui.LoginView;
import com.canteen.ui.StudentView;
import com.canteen.ui.Ui;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage stage;
    public static User currentUser;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            DBConnection.ensureSchemaCompatibility();
            UserDAO.ensureAdmin();
            TokenQueue.get().reload();
        } catch (Exception e) {
            Ui.error("Cannot reach the database.\n\n" + e.getMessage()
                    + "\n\nCheck MySQL is running, schema.sql was imported, and the DB user/password are correct.");
            return;
        }
        showLogin();
        stage.show();
    }

    public static void showLogin() {
        currentUser = null;
        show(new LoginView().build(), "Smart Canteen - Login", 420, 420);
    }

    public static void showHome() {
        if (currentUser.isAdmin()) {
            show(new AdminView().build(), "Smart Canteen - Admin Dashboard", 760, 540);
        } else {
            show(new StudentView().build(), "Smart Canteen - Order Food", 900, 580);
        }
    }

    private static void show(Parent root, String title, double w, double h) {
        stage.setScene(new Scene(root, w, h));
        stage.setTitle(title);
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
