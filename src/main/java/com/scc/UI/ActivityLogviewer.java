package com.scc.UI;

import com.scc.DAO.ActivityLogDao;
import com.scc.MODEL.User;
import com.scc.MODEL.activityLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class ActivityLogviewer {
    private final User currentUser;
    private final Scanner scanner;
    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    public ActivityLogviewer(User currentUser, Scanner scanner) {
        this.currentUser = currentUser;
        this.scanner = scanner;
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n--- ACTIVITY & AUDIT LOGS (CRUD) ---");
            System.out.println("  [1] CREATE - Add Manual Audit Entry");
            System.out.println("  [2] READ   - View / Search Audit Logs");
            System.out.println("  [3] UPDATE - Edit Audit Log Details");
            System.out.println("  [4] DELETE - Soft-Delete Audit Log");
            System.out.println("  [0] Back");
            System.out.print("Select: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> {
                    System.out.print("Audit Note Details: ");
                    String details = scanner.nextLine().trim();
                    activityLog log = new activityLog();
                    log.setOperatorId(currentUser.getUserId());
                    log.setEventType("MANUAL_AUDIT_NOTE");
                    log.setDetails(details);
                    log.setCreatedAt(LocalDateTime.now());
                    if (activityLogDao.createLog(log)) {
                        System.out.println("[OK] Audit entry created.");
                    }
                }
                case "2" -> {
                    System.out.print("Search keyword (or press Enter for all): ");
                    String kw = scanner.nextLine().trim();
                    List<activityLog> logs = activityLogDao.getAllLogs(kw, false);
                    System.out.printf("\n%-5s %-22s %-12s %-35s %-20s%n", "ID", "EVENT", "PLATE", "DETAILS", "TIME");
                    System.out.println("------------------------------------------------------------------------------------------------");
                    for (activityLog l : logs) {
                        System.out.printf("%-5d %-22s %-12s %-35s %-20s%n",
                                l.getLogId(), l.getEventType(),
                                l.getPlateNumber() != null ? l.getPlateNumber() : "-",
                                l.getDetails(), l.getCreatedAt());
                    }
                }
                case "3" -> {
                    System.out.print("Enter Log ID to update: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    System.out.print("New Details: ");
                    String details = scanner.nextLine().trim();
                    activityLog log = new activityLog();
                    log.setLogId(id);
                    log.setEventType("UPDATED_AUDIT_LOG");
                    log.setDetails(details);
                    if (activityLogDao.updateLog(log)) {
                        System.out.println("[OK] Log updated.");
                    }
                }
                case "4" -> {
                    System.out.print("Enter Log ID to soft-delete: ");
                    int id = Integer.parseInt(scanner.nextLine().trim());
                    if (activityLogDao.softDeleteLog(id)) {
                        System.out.println("[OK] Log soft-deleted.");
                    }
                }
                case "0" -> { return; }
                default -> System.out.println("[!] Invalid option.");
            }
        }
    }
}