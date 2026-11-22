package tour;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TourWorkflow {
    private final RoomMatcher matcher = new RoomMatcher();

    Result run(Input input) {
        List<RoomMatcher.Suggestion> suggestions = matcher.suggest(input.panoramas(), input.rooms());
        Map<Long, Long> roomByPanorama = new HashMap<>();
        for (RoomMatcher.Suggestion suggestion : suggestions) {
            if (suggestion.status().equals("suggested")) {
                roomByPanorama.put(suggestion.panoramaId(), suggestion.roomId());
            }
        }

        Tour tour = Tour.draft(input.tours(), input.property(), input.operator());
        tour.loadScenes(input.panoramas(), input.scenes(), roomByPanorama);

        Tour.Publication publication = tour.publish(input.scenes(), input.transitions(), input.panoramas(),
                input.versions());
        return new Result(suggestions, tour.snapshot(), tour.scenes(), publication.validation(), publication.version());
    }

    record Input(Property property, Operator operator, List<Room> rooms, List<Panorama> panoramas,
                 List<Tour.Snapshot> tours, List<Scene> scenes, List<Transition> transitions,
                 List<TourVersion> versions) {
    }

    record Result(List<RoomMatcher.Suggestion> roomSuggestions, Tour.Snapshot tour, List<Scene> scenes,
                  TourGraph.Validation validation, TourVersion publishedVersion) {
    }
}
