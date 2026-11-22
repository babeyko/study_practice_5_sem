package tour;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class Tour {
    private static final DateTimeFormatter PUBLISHED_AT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSxxx");

    private final long tourId;
    private final Property property;
    private final Operator operator;
    private final TourGraph graph = new TourGraph();
    private List<Scene> scenes = List.of();
    private String status = "draft";

    private Tour(long tourId, Property property, Operator operator) {
        this.tourId = tourId;
        this.property = property;
        this.operator = operator;
    }

    static Tour draft(List<Snapshot> existing, Property property, Operator operator) {
        long lastId = existing.stream().mapToLong(tour -> idOrZero(tour.tourId())).max().orElse(0);
        return new Tour(nextId(lastId), property, operator);
    }

    void loadScenes(List<Panorama> panoramas, List<Scene> existing, Map<Long, Long> roomByPanorama) {
        if (panoramas.isEmpty()) {
            throw new IllegalArgumentException("Для тура нужна хотя бы одна панорама");
        }

        long lastId = existing.stream().mapToLong(scene -> idOrZero(scene.sceneId())).max().orElse(0);
        List<Scene> created = new ArrayList<>();
        for (Panorama panorama : panoramas) {
            if (!Objects.equals(panorama.propertyId(), property.propertyId())) {
                throw new IllegalArgumentException("Панорама " + panorama.panoramaId()
                        + " относится к другому объекту");
            }
            lastId = nextId(lastId);
            created.add(new Scene(lastId, tourId, roomByPanorama.get(panorama.panoramaId()),
                    panorama.panoramaId(), panorama.sceneName(), existing.size() + created.size() + 1,
                    created.isEmpty()));
        }
        scenes = List.copyOf(created);
    }

    List<Scene> scenes() {
        return scenes;
    }

    private TourGraph.Validation validate(List<Scene> existing, List<Transition> transitions,
                                          List<Panorama> panoramas) {
        List<Scene> allScenes = new ArrayList<>(existing);
        allScenes.addAll(scenes);
        return graph.validate(allScenes, transitions, panoramas, tourId, scenes.get(0).sceneId());
    }

    Publication publish(List<Scene> existing, List<Transition> transitions, List<Panorama> panoramas,
                        List<TourVersion> versions) {
        TourGraph.Validation validation = validate(existing, transitions, panoramas);
        if (!validation.ok()) {
            throw new IllegalArgumentException("Структура тура не прошла проверку: "
                    + String.join("; ", validation.errors()));
        }
        long lastId = versions.stream().mapToLong(version -> idOrZero(version.versionId()))
                .max().orElse(0);
        long lastNumber = versions.stream().filter(version -> Objects.equals(version.tourId(), tourId))
                .mapToLong(version -> idOrZero(version.versionNumber())).max().orElse(0);
        long versionNumber = nextId(lastNumber);
        String url = "https://example.invalid/tours/" + tourId + "/v/" + versionNumber;
        String timestamp = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS).format(PUBLISHED_AT);

        TourVersion published = new TourVersion(nextId(lastId), tourId, versionNumber, url, timestamp);
        status = "published";
        return new Publication(validation, published);
    }

    Snapshot snapshot() {
        return new Snapshot(tourId, property.propertyId(), operator.operatorId(), status);
    }

    private static long idOrZero(Long id) {
        return id == null ? 0 : id;
    }

    private static long nextId(long previous) {
        if (previous == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Невозможно назначить следующий идентификатор или номер версии");
        }
        return previous + 1;
    }

    record Snapshot(Long tourId, Long propertyId, Long operatorId, String status) {
    }

    record Publication(TourGraph.Validation validation, TourVersion version) {
    }
}
