package com.example.timesheet.config;

import com.example.timesheet.domain.AppUser;
import com.example.timesheet.domain.Project;
import com.example.timesheet.domain.Role;
import com.example.timesheet.domain.Task;
import com.example.timesheet.repository.AppUserRepository;
import com.example.timesheet.repository.ProjectRepository;
import com.example.timesheet.repository.TaskRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Demo data so the app is usable on first start. Replace the passwords before real use. */
@Component
public class SeedData implements CommandLineRunner {

    private final AppUserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final PasswordEncoder encoder;

    public SeedData(AppUserRepository users, ProjectRepository projects,
                    TaskRepository tasks, PasswordEncoder encoder) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        users.save(new AppUser("admin@example.com", "Ada Admin", encoder.encode("admin123"), Role.ADMIN));
        users.save(new AppUser("mitch@example.com", "Mitch Mullinax", encoder.encode("user123"), Role.USER));
        users.save(new AppUser("bob@example.com", "Bob Brown", encoder.encode("user123"), Role.USER));

        Project web = projects.save(new Project("ACME-WEB", "Website Redesign", "Acme Corp"));
        tasks.saveAll(List.of(
                new Task(web, "Design", true),
                new Task(web, "Development", true),
                new Task(web, "Review", true)));

        Project api = projects.save(new Project("GLOBEX-API", "API Migration", "Globex"));
        tasks.saveAll(List.of(
                new Task(api, "Discovery", true),
                new Task(api, "Implementation", true)));

        Project internal = projects.save(new Project("INTERNAL", "Internal", "Internal"));
        tasks.saveAll(List.of(
                new Task(internal, "Meetings", false),
                new Task(internal, "Training", false)));
    }
}