package com.example.app.model;

public enum Role {
    STUDENT("/student/dashboard"),
    ORGANIZER("/organizer/dashboard"),
    ADMINISTRATOR("/admin/dashboard");

    private final String dashboardPath;

    Role(String dashboardPath) { this.dashboardPath = dashboardPath; }

    /** Frontend route the user is redirected to after login. */
    public String getDashboardPath() { return dashboardPath; }
}
