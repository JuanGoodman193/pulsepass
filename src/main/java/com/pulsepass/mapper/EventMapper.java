package com.pulsepass.mapper;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "artists", expression = "java(mapArtists(event.getArtists()))")
    EventResponse toResponse(Event event);

    @Mapping(target = "venueName", source = "venue.name")
    EventSummaryResponse toSummary(Event event);

    default Set<String> mapArtists(Set<Artist> artists) {
        if (artists == null) {
            return Collections.emptySet();
        }
        return artists.stream()
                .map(Artist::getStageName)
                .collect(Collectors.toSet());
    }
}
