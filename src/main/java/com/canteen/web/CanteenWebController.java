package com.canteen.web;

import com.canteen.dao.FoodDAO;
import com.canteen.dao.OrderDAO;
import com.canteen.dao.UserDAO;
import com.canteen.model.CartItem;
import com.canteen.model.FoodItem;
import com.canteen.model.Token;
import com.canteen.model.User;
import com.canteen.service.TokenQueue;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CanteenWebController {

    private static final String USER_SESSION_KEY = "canteenUser";

    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session) {
        if (session.getAttribute(USER_SESSION_KEY) != null) {
            User user = (User) session.getAttribute(USER_SESSION_KEY);
            return user.isAdmin() ? "redirect:/admin" : "redirect:/student";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) throws SQLException {
        User user = UserDAO.login(email.trim(), password);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password.");
            return "redirect:/login";
        }
        session.setAttribute(USER_SESSION_KEY, user);
        return user.isAdmin() ? "redirect:/admin" : "redirect:/student";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) throws SQLException {
        if (name == null || name.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "All fields are required.");
            return "redirect:/login";
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/login";
        }

        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Enter a valid email address.");
            return "redirect:/login";
        }

        boolean created = UserDAO.register(name.trim(), email.trim(), password, "STUDENT");
        if (!created) {
            redirectAttributes.addFlashAttribute("error", "That email is already registered.");
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("success", "Account created successfully. Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "You have been logged out.");
        return "redirect:/login";
    }

    @GetMapping("/student")
    public String studentPage(HttpSession session, Model model) throws SQLException {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (user.isAdmin()) {
            return "redirect:/admin";
        }

        List<FoodItem> menuItems = FoodDAO.findAll();
        List<com.canteen.model.OrderSummary> orderHistory = OrderDAO.findByUser(user.getUserId());
        model.addAttribute("user", user);
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("orderHistory", orderHistory);
        return "student";
    }

    @PostMapping("/student/order")
    public String placeOrder(@RequestParam(value = "foodId", required = false) List<Integer> foodIds,
                            @RequestParam(value = "quantity", required = false) List<Integer> quantities,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<CartItem> cart = buildCart(foodIds, quantities);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Select at least one item with quantity greater than zero.");
            return "redirect:/student";
        }

        Token token = OrderDAO.placeOrder(user, cart);
        TokenQueue.get().enqueue(token);
        redirectAttributes.addFlashAttribute("success",
                "Order placed successfully. Your token number is " + token.getTokenNumber() + ".");
        return "redirect:/student";
    }

    @GetMapping("/admin")
    public String adminPage(HttpSession session, Model model) throws SQLException {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (!user.isAdmin()) {
            return "redirect:/student";
        }

        TokenQueue.get().reload();
        model.addAttribute("user", user);
        model.addAttribute("queue", TokenQueue.get().snapshot());
        model.addAttribute("menuItems", FoodDAO.findAll());
        model.addAttribute("orders", OrderDAO.findAll());
        return "admin";
    }

    @PostMapping("/admin/serve-next")
    public String serveNext(HttpSession session, RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }

        Token t = TokenQueue.get().serveNext();
        if (t == null) {
            redirectAttributes.addFlashAttribute("error", "The queue is empty.");
        } else {
            redirectAttributes.addFlashAttribute("success",
                    "Now serving Token #" + t.getTokenNumber() + " for " + t.getCustomerName() + ".");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/menu/add")
    public String addMenuItem(@RequestParam String itemName,
                              @RequestParam Double itemPrice,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }
        if (itemName == null || itemName.isBlank() || itemPrice == null || itemPrice <= 0) {
            redirectAttributes.addFlashAttribute("error", "Enter a valid item name and price.");
            return "redirect:/admin";
        }

        FoodDAO.add(itemName.trim(), itemPrice);
        redirectAttributes.addFlashAttribute("success", "Item added successfully.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/menu/update")
    public String updateMenuItem(@RequestParam Integer foodId,
                                @RequestParam Double itemPrice,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }
        if (foodId == null || itemPrice == null || itemPrice <= 0) {
            redirectAttributes.addFlashAttribute("error", "Select an item and enter a valid price.");
            return "redirect:/admin";
        }

        FoodDAO.updatePrice(foodId, itemPrice);
        redirectAttributes.addFlashAttribute("success", "Price updated successfully.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/menu/remove")
    public String removeMenuItem(@RequestParam Integer foodId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }
        if (foodId == null) {
            redirectAttributes.addFlashAttribute("error", "Select an item to remove.");
            return "redirect:/admin";
        }

        FoodDAO.remove(foodId);
        redirectAttributes.addFlashAttribute("success", "Item removed successfully.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/orders/{orderId}/complete")
    public String markCompleted(@PathVariable int orderId, HttpSession session, RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }
        OrderDAO.markOrderCompleted(orderId);
        redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " marked as completed.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/orders/{orderId}/deliver")
    public String markDelivered(@PathVariable int orderId, HttpSession session, RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }
        OrderDAO.markOrderDelivered(orderId);
        redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " marked as delivered.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/orders/{orderId}/cancel")
    public String cancelOrder(@PathVariable int orderId, HttpSession session, RedirectAttributes redirectAttributes) throws SQLException {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            return "redirect:/login";
        }
        OrderDAO.cancelOrder(orderId);
        redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " cancelled.");
        return "redirect:/admin";
    }

    private User currentUser(HttpSession session) {
        Object user = session.getAttribute(USER_SESSION_KEY);
        return user instanceof User ? (User) user : null;
    }

    private List<CartItem> buildCart(List<Integer> foodIds, List<Integer> quantities) {
        if (foodIds == null || quantities == null) {
            return new ArrayList<>();
        }

        try {
            Map<Integer, FoodItem> menuMap = new HashMap<>();
            for (FoodItem item : FoodDAO.findAll()) {
                menuMap.put(item.getFoodId(), item);
            }

            List<CartItem> cart = new ArrayList<>();
            for (int i = 0; i < foodIds.size(); i++) {
                Integer foodId = foodIds.get(i);
                Integer quantity = (i < quantities.size()) ? quantities.get(i) : 0;
                if (foodId == null || quantity == null || quantity <= 0) {
                    continue;
                }
                FoodItem item = menuMap.get(foodId);
                if (item != null) {
                    cart.add(new CartItem(item, quantity));
                }
            }
            return cart;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to read menu items", e);
        }
    }
}
