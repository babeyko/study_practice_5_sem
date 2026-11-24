package tour;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class TourJson {
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setStrictness(Strictness.STRICT)
            .disableHtmlEscaping()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    TourWorkflow.Input read(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null) {
                throw new IllegalArgumentException("Ожидается объект JSON");
            }
            for (String field : List.of("property_id", "operator_id", "rooms", "panoramas")) {
                if (!root.has(field)) {
                    throw new IllegalArgumentException("Отсутствует обязательное поле " + field);
                }
            }
            InputDocument data = gson.fromJson(root, InputDocument.class);
            Realtor realtor = data.realtor();
            if (realtor != null && realtor.realtorId() == null) {
                throw new IllegalArgumentException("Отсутствует обязательное поле realtor.realtor_id");
            }
            List<Panorama> panoramas = data.panoramas().stream().map(PanoramaEntry::toPanorama).toList();
            return new TourWorkflow.Input(new Property(data.propertyId(), realtor),
                    new Operator(data.operatorId()),
                    data.rooms(), panoramas, data.tours(), data.scenes(), data.transitions(), data.versions());
        }
    }

    void write(Path path, TourWorkflow.Result result) throws IOException {
        JsonObject root = gson.toJsonTree(result).getAsJsonObject();
        for (JsonElement entry : root.getAsJsonArray("room_suggestions")) {
            JsonObject suggestion = entry.getAsJsonObject();
            if (suggestion.get("candidate_room_ids").isJsonNull()) {
                suggestion.remove("candidate_room_ids");
            }
        }
        try (var writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(root, writer);
        }
    }

    private record PanoramaEntry(Long panoramaId, Long propertyId, String fileName, String roomHint) {
        Panorama toPanorama() {
            return Panorama.create(panoramaId, propertyId, fileName, roomHint);
        }
    }

    private record InputDocument(Long propertyId, Long operatorId, Realtor realtor,
                                 List<Room> rooms, List<PanoramaEntry> panoramas,
                                 List<Tour.Snapshot> tours, List<Scene> scenes, List<Transition> transitions,
                                 List<TourVersion> versions) {
        InputDocument {
            if (rooms == null || panoramas == null) {
                throw new IllegalArgumentException("Поля rooms и panoramas должны содержать массивы");
            }
            tours = tours == null ? List.of() : tours;
            scenes = scenes == null ? List.of() : scenes;
            transitions = transitions == null ? List.of() : transitions;
            versions = versions == null ? List.of() : versions;
        }
    }
}
