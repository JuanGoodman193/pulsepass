package com.pulsepass.service;

import com.pulsepass.domain.Venue;
import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.impl.VenueServiceImpl;
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
class VenueServiceImplTest {

    @Mock
    private VenueRepository repository;

    @Mock
    private VenueMapper mapper;

    @InjectMocks
    private VenueServiceImpl service;

    @Test
    @DisplayName("Buscar venue existente por código")
    void shouldFindVenueByCode() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Center", "Santa Marta", "Address", 100, true);
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Center", "Santa Marta", 100, true);

        when(repository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(mapper.toResponse(venue)).thenReturn(response);

        VenueResponse result = service.findByCode("VEN-SMR-01");

        assertThat(result.code()).isEqualTo("VEN-SMR-01");
        verify(repository).findByCode("VEN-SMR-01");
    }

    @Test
    @DisplayName("Buscar venue inexistente lanza ResourceNotFoundException")
    void shouldThrowExceptionWhenVenueNotFound() {
        when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Venue not found");
    }

    @Test
    @DisplayName("Listar solo venues activos")
    void shouldFindActiveVenues() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Center", "Santa Marta", "Address", 100, true);
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Center", "Santa Marta", 100, true);

        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(mapper.toResponse(venue)).thenReturn(response);

        List<VenueResponse> result = service.findActiveVenues();

        assertThat(result).hasSize(1);
        verify(repository).findByActiveTrueOrderByNameAsc();
    }
}
