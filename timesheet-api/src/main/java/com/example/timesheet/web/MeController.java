package com.example.timesheet.web;

import com.example.timesheet.security.AppUserPrincipal;
import com.example.timesheet.web.Dtos.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal AppUserPrincipal me) {
        return new UserDto(me.id(), me.name(), me.email(), me.role());
    }
}