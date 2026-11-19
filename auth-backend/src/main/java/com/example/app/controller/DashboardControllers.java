package com.example.app.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Placeholder dashboard endpoints. URL rules in SecurityConfig and @PreAuthorize
 * both enforce roles (defense in depth). Replace the payloads with real data later.
 */
public class DashboardControllers {

    @RestController
    @RequestMapping("/api/student")
    @PreAuthorize("hasRole('STUDENT')")
    public static class StudentController {
        @GetMapping("/dashboard")
        public Map<String, Object> dashboard(Authentication a) {
            return Map.of("message", "Student dashboard", "user", a.getName(),
                    "menu", List.of("Browse Events", "My Registrations", "Profile"));
        }
    }

    @RestController
    @RequestMapping("/api/organizer")
    @PreAuthorize("hasRole('ORGANIZER')")
    public static class OrganizerController {
        @GetMapping("/dashboard")
        public Map<String, Object> dashboard(Authentication a) {
            return Map.of("message", "Organizer dashboard", "user", a.getName(),
                    "menu", List.of("My Events", "Create Event", "Participants", "Profile"));
        }
    }

    @RestController
    @RequestMapping("/api/admin")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public static class AdminController {
        @GetMapping("/dashboard")
        public Map<String, Object> dashboard(Authentication a) {
            return Map.of("message", "Administrator dashboard", "user", a.getName(),
                    "menu", List.of("Manage Users", "Approve Events", "Reports", "Profile"));
        }
    }
}
