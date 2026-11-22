package tour;

import java.util.List;

record Room(Long roomId, Long propertyId, String name, List<String> synonyms) {
    Room {
        synonyms = synonyms == null ? List.of() : synonyms;
    }
}
