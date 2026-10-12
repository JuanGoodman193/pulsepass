package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @Test
    @DisplayName("TEST-CTRL-ART-001: buscar por ID -> 200")
    void shouldReturnArtistWhenIdExists() throws Exception {
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", true);
        when(artistService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/artists/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findById(1L);
    }

    @Test
    @DisplayName("TEST-CTRL-ART-002: ID inexistente -> 404")
    void shouldReturn404WhenArtistIdNotFound() throws Exception {
        when(artistService.findById(999L))
                .thenThrow(new ResourceNotFoundException("Artist not found with id: 999"));

        mockMvc.perform(get("/api/artists/999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found with id: 999"));

        verify(artistService).findById(999L);
    }

    @Test
    @DisplayName("TEST-CTRL-ART-003: buscar por stageName -> 200")
    void shouldReturnArtistWhenStageNameExists() throws Exception {
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", true);
        when(artistService.findByStageName("Solar Beat")).thenReturn(response);

        mockMvc.perform(get("/api/artists/by-stage-name")
                        .param("stageName", "Solar Beat")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    @DisplayName("TEST-CTRL-ART-003 variant: buscar por stageName inexistente -> 404")
    void shouldReturn404WhenStageNameNotFound() throws Exception {
        when(artistService.findByStageName("Unknown Artist"))
                .thenThrow(new ResourceNotFoundException("Artist not found with stage name: Unknown Artist"));

        mockMvc.perform(get("/api/artists/by-stage-name")
                        .param("stageName", "Unknown Artist")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found with stage name: Unknown Artist"));

        verify(artistService).findByStageName("Unknown Artist");
    }

    @Test
    @DisplayName("TEST-CTRL-ART-004: listar activos -> 200")
    void shouldReturnActiveArtists() throws Exception {
        ArtistResponse response1 = new ArtistResponse(1L, "Solar Beat", true);
        ArtistResponse response2 = new ArtistResponse(2L, "Neon Waves", true);
        when(artistService.findActiveArtists()).thenReturn(List.of(response1, response2));

        mockMvc.perform(get("/api/artists/active")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"));

        verify(artistService).findActiveArtists();
    }
}
