package tour;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RoomMatcher {
    private static final Pattern EXTENSION = Pattern.compile("\\.[a-z0-9]{1,5}$");
    private static final Pattern WORD = Pattern.compile("[a-zа-я0-9]+");

    private final List<MatchingRule> rules = List.of(new ExactNameRule(), new SynonymRule());

    List<Suggestion> suggest(List<Panorama> panoramas, List<Room> rooms) {
        List<PreparedRoom> preparedRooms = rooms.stream().map(PreparedRoom::from).toList();
        return panoramas.stream().map(panorama -> suggestOne(panorama, preparedRooms)).toList();
    }

    private Suggestion suggestOne(Panorama panorama, List<PreparedRoom> rooms) {
        String name = normalize(panorama.sceneName());
        int bestScore = 0;
        List<Long> winners = new ArrayList<>();

        for (PreparedRoom room : rooms) {
            int score = score(name, room);
            if (score > bestScore) {
                bestScore = score;
                winners.clear();
            }
            if (score > 0 && score == bestScore) {
                winners.add(room.id());
            }
        }

        if (winners.isEmpty()) {
            return new Suggestion(panorama.panoramaId(), null, 0, "manual_review", null);
        }
        if (winners.size() > 1) {
            return new Suggestion(panorama.panoramaId(), null, bestScore, "ambiguous", winners);
        }
        return new Suggestion(panorama.panoramaId(), winners.get(0), bestScore, "suggested", null);
    }

    private int score(String name, PreparedRoom room) {
        int best = 0;
        for (MatchingRule rule : rules) {
            best = Math.max(best, rule.score(name, room));
        }
        return best;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String basename = value.substring(Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\')) + 1);
        String lowered = basename.toLowerCase(Locale.ROOT).replace('ё', 'е');
        String withoutExtension = EXTENSION.matcher(lowered).replaceFirst("");
        Matcher matcher = WORD.matcher(withoutExtension);
        List<String> words = new ArrayList<>();
        while (matcher.find()) {
            words.add(matcher.group());
        }
        return String.join(" ", words);
    }

    private record PreparedRoom(Long id, String name, Set<String> synonyms) {
        static PreparedRoom from(Room room) {
            Set<String> synonyms = new HashSet<>();
            for (String synonym : room.synonyms()) {
                synonyms.add(normalize(synonym));
            }
            synonyms.remove("");
            return new PreparedRoom(room.roomId(), normalize(room.name()), synonyms);
        }
    }

    private interface MatchingRule {
        int score(String panoramaName, PreparedRoom room);
    }

    private static final class ExactNameRule implements MatchingRule {
        @Override
        public int score(String panoramaName, PreparedRoom room) {
            return !panoramaName.isEmpty() && panoramaName.equals(room.name()) ? 3 : 0;
        }
    }

    private static final class SynonymRule implements MatchingRule {
        @Override
        public int score(String panoramaName, PreparedRoom room) {
            return room.synonyms().contains(panoramaName) ? 2 : 0;
        }
    }

    record Suggestion(Long panoramaId, Long roomId, int score, String status, List<Long> candidateRoomIds) {
    }
}
