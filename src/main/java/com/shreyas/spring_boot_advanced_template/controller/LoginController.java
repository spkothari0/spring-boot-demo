package com.shreyas.spring_boot_advanced_template.controller;

import com.shreyas.spring_boot_advanced_template.business.bean.LoginRequest;
import com.shreyas.spring_boot_advanced_template.business.bean.LoginResponse;
import com.shreyas.spring_boot_advanced_template.business.bean.UserBean;
import com.shreyas.spring_boot_advanced_template.entity.Constants.RoleType;
import com.shreyas.spring_boot_advanced_template.jwt.JwtUtils;
import com.shreyas.spring_boot_advanced_template.service.interfaces.IUserServices;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*")
public class LoginController extends BaseController {
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final IUserServices userServices;

    @Autowired
    public LoginController(JwtUtils jwtUtils, AuthenticationManager authenticationManager, IUserServices userServices) {
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.userServices = userServices;
    }

    @PostMapping(value = "/login")
    @Operation(summary = "Login a user with the specified username and password. Accessible to all users.",
        description = "Used to get the token for the specified user."
    )
    public ResponseEntity<APIResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtils.generateTokenFromUsername(userDetails);
        List<String> roles = userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        LoginResponse loginResponse = new LoginResponse(token, userDetails.getUsername(), roles);
        return SuccessResponse(loginResponse);
    }

    @PostMapping(value = "/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a user with the specified username and password and other information. Accessible to Admin only.",
        description = "The Role of a user can be one of the following: ROLE_ADMIN, ROLE_STUDENT, ROLE_TEACHER, ROLE_USER."
    )
    public ResponseEntity<APIResponse<UserBean>> register(@Valid @RequestBody UserBean user) {
        List<RoleType> roleList = new ArrayList<>(Arrays.asList(RoleType.values()));
        String role;
        try {
            role = RoleType.valueOf(user.getRole()).toString();
            user.setRole(role);
        } catch (IllegalArgumentException e) {
            return ErrorResponse("Invalid role. Role must be one of: " + roleList);
        }

        UserBean createdUser = userServices.createUser(user);
        return SuccessResponse("User registered successfully", createdUser);
    }

    @GetMapping(value ="/verification/{token}", produces = MediaType.ALL_VALUE)
    @Operation(summary = "Verify that user is registered and unlocked.",
    description = "Should be accessed only via link from the email address.")
    public ResponseEntity<String> verifyUser(@PathVariable String token) {
        String response = userServices.verifyUser(token);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}