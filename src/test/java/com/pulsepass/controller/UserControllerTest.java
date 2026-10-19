package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("TEST-CTRL-USR-001: registrar válido -> 201")
    void shouldRegisterUserSuccessfully() throws Exception {
        String jsonRequest = """
                {
                  "username": "andrea",
                  "email": "andrea@example.com",
                  "password": "Password12345!",
                  "firstName": "Andrea",
                  "lastName": "Lopez",
                  "phone": "3001234567",
                  "city": "Bogota",
                  "birthDate": "1995-05-10"
                }
                """;

        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Lopez",
                "3001234567",
                "Bogota",
                LocalDate.of(1995, 5, 10)
        );

        when(userService.register(any(RegisterUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@example.com"))
                .andExpect(jsonPath("$.firstName").value("Andrea"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-USR-002: email inválido -> 400")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        String invalidJson = """
                {
                  "username": "andrea",
                  "email": "invalid-email",
                  "firstName": "",
                  "lastName": "Lopez",
                  "birthDate": null
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.firstName").exists())
                .andExpect(jsonPath("$.details.birthDate").exists());

        verify(userService, never()).register(any());
    }

    @Test
    @DisplayName("TEST-CTRL-USR-003: username duplicado -> 409")
    void shouldReturn409WhenUsernameIsDuplicate() throws Exception {
        String jsonRequest = """
                {
                  "username": "andrea",
                  "email": "andrea@example.com",
                  "password": "Password12345!",
                  "firstName": "Andrea",
                  "lastName": "Lopez",
                  "birthDate": "1995-05-10"
                }
                """;

        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("AC-CTRL-006: regla de negocio en registro (fecha futura) -> 409")
    void shouldReturn409WhenBusinessRuleFails() throws Exception {
        String jsonRequest = """
                {
                  "username": "futureuser",
                  "email": "future@example.com",
                  "password": "Password12345!",
                  "firstName": "Future",
                  "lastName": "User",
                  "birthDate": "2099-01-01"
                }
                """;

        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(new BusinessRuleException("Birth date cannot be in the future"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Birth date cannot be in the future"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("TEST-CTRL-USR-004: consultar por email -> 200")
    void shouldReturnUserByEmail() throws Exception {
        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Lopez",
                "3001234567",
                "Bogota",
                LocalDate.of(1995, 5, 10)
        );

        when(userService.findByEmail("andrea@example.com")).thenReturn(response);

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "andrea@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@example.com"))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByEmail("andrea@example.com");
    }

    @Test
    @DisplayName("TEST-CTRL-USR-004 variant: consultar por email inexistente -> 404")
    void shouldReturn404WhenEmailNotFound() throws Exception {
        when(userService.findByEmail("unknown@example.com"))
                .thenThrow(new ResourceNotFoundException("User not found with email: unknown@example.com"));

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "unknown@example.com")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with email: unknown@example.com"));

        verify(userService).findByEmail("unknown@example.com");
    }

    @Test
    @DisplayName("TEST-CTRL-USR-005: consultar por username -> 200")
    void shouldReturnUserByUsername() throws Exception {
        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Lopez",
                "3001234567",
                "Bogota",
                LocalDate.of(1995, 5, 10)
        );

        when(userService.findByUsername("andrea")).thenReturn(response);

        mockMvc.perform(get("/api/users/by-username")
                        .param("username", "andrea")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@example.com"));

        verify(userService).findByUsername("andrea");
    }

    @Test
    @DisplayName("TEST-CTRL-USR-005 variant: consultar por username inexistente -> 404")
    void shouldReturn404WhenUsernameNotFound() throws Exception {
        when(userService.findByUsername("unknown"))
                .thenThrow(new ResourceNotFoundException("User not found with username: unknown"));

        mockMvc.perform(get("/api/users/by-username")
                        .param("username", "unknown")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with username: unknown"));

        verify(userService).findByUsername("unknown");
    }
}
