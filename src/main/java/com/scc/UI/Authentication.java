package com.scc.UI;

import com.scc.DAO.ActivityLogDao;
import com.scc.DAO.SecurityUtil;
import com.scc.DAO.UserDao;
import com.scc.MODEL.User;
import com.scc.MODEL.activityLog;

import java.time.LocalDateTime;
import java.util.Scanner;

public class Authentication {
    private final Scanner scanner = new Scanner(System.in);
    private final UserDao userDao = new UserDao();
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public void start() {
        while (true) {
            System.out.println("\n=======================================================");
            System.out.println("  ALPR VEHICLE ACCESS & PARKING MANAGEMENT SYSTEM");
            System.out.println("=======================================================");
            System.out.println("  [1] Login");
            System.out.println("  [2] Register New Vehicle Owner Account");
            System.out.println("  [0] Exit System");
            System.out.print("Select option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> login();
                case "2" -> registerOwner();
                case "0" -> {
                    System.out.println("Shutting down ALPR system. Goodbye!");
                    return;
                }
                default -> System.out.println("[!] Invalid option. Please enter 1, 2, or 0.");
            }
        }
    }

    private void login() {
        System.out.print("\nUsername: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        User user = userDao.getUserByUsername(username);
        if (user == null || !user.isActive() || !SecurityUtil.verifyPassword(password, user.getPasswordHash())) {
            System.out.println("[X] Login failed: Invalid credentials or inactive account.");
            return;
        }

        userDao.updateLastLogin(user.getUserId());

        activityLog log = new activityLog();
        log.setOperatorId(user.getUserId());
        log.setEventType("USER_LOGIN");
        log.setDetails("User '" + user.getUsername() + "' (" + user.getRole() + ") logged in via terminal.");
        log.setCreatedAt(LocalDateTime.now());
        activityLogDao.createLog(log);

        System.out.println("\n[OK] Welcome, " + user.getUsername() + " (" + user.getRole() + ")!");

        switch (user.getRole()) {
            case "SYSTEM_ADMIN" -> new AdminMenu(user, scanner).showMenu();
            case "SECURITY_OPERATOR" -> new SecurityPersonel(user, scanner).showMenu();
            case "VEHICLE_OWNER" -> new VehicleOwnerMenu(user, scanner).showMenu();
            default -> System.out.println("[!] Unknown role: " + user.getRole());
        }
    }

    private void registerOwner() {
        System.out.println("\n--- REGISTER NEW VEHICLE OWNER ---");
        System.out.print("Username          : ");
        String username = scanner.nextLine().trim();
        System.out.print("Password          : ");
        String password = scanner.nextLine().trim();
        System.out.print("Email             : ");
        String email = scanner.nextLine().trim();
        System.out.print("Contact Number    : ");
        String contact = scanner.nextLine().trim();
        System.out.print("Emergency Contact : ");
        String emergency = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("[X] Username and password are required.");
            return;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPasswordHash(SecurityUtil.hashPassword(password));
        newUser.setRole("VEHICLE_OWNER");
        newUser.setEmail(email);
        newUser.setContactNumber(contact);
        newUser.setEmergencyContact(emergency);
        newUser.setActive(true);

        if (userDao.createUser(newUser)) {
            System.out.println("[OK] Account created! You can now log in.");
        } else {
            System.out.println("[X] Could not create account (username may already exist).");
        }
    }
}