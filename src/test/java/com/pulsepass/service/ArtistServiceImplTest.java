package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.service.impl.ArtistServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository repository;

    @Mock
    private ArtistMapper mapper;

    @InjectMocks
    private ArtistServiceImpl service;

    @Test
    @DisplayName("Buscar artista por id existente")
    void shouldFindArtistById() {
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", true);

        when(repository.findById(1L)).thenReturn(Optional.of(artist));
        when(mapper.toResponse(artist)).thenReturn(response);

        ArtistResponse result = service.findById(1L);

        assertThat(result.stageName()).isEqualTo("Solar Beat");
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("Buscar artista inexistente lanza ResourceNotFoundException")
    void shouldThrowExceptionWhenArtistNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Artist not found");
    }

    @Test
    @DisplayName("Listar artistas activos")
    void shouldFindActiveArtists() {
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic", true);
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", true);

        when(repository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(artist));
        when(mapper.toResponse(artist)).thenReturn(response);

        List<ArtistResponse> result = service.findActiveArtists();

        assertThat(result).hasSize(1);
        verify(repository).findByActiveTrueOrderByStageNameAsc();
    }
}
