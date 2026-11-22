package tour;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class TourGraph {
    Validation validate(List<Scene> scenes, List<Transition> transitions, List<Panorama> panoramas,
                        long tourId, long startSceneId) {
        List<String> errors = new ArrayList<>();
        Map<Long, Scene> sceneById = new LinkedHashMap<>();
        Set<Long> panoramaIds = new HashSet<>();
        List<Long> starts = new ArrayList<>();
        for (Panorama panorama : panoramas) {
            panoramaIds.add(panorama.panoramaId());
        }

        for (Scene scene : scenes) {
            Long id = scene.sceneId();
            if (sceneById.put(id, scene) != null) {
                errors.add("Повторяется scene_id=" + displayId(id));
            }
            if (!Objects.equals(scene.tourId(), tourId)) {
                errors.add("Сцена " + displayId(id) + " относится к другому туру");
            }
            if (!panoramaIds.contains(scene.panoramaId())) {
                errors.add("У сцены " + displayId(id) + " отсутствует панорама");
            }
            if (Boolean.TRUE.equals(scene.isStart())) {
                starts.add(id);
            }
        }
        if (!sceneById.containsKey(startSceneId)) {
            errors.add("Стартовая сцена не существует");
        }
        if (starts.size() != 1 || !Objects.equals(starts.get(0), startSceneId)) {
            errors.add("Для тура должна быть назначена ровно одна указанная стартовая сцена");
        }

        Map<Long, List<Long>> adjacent = new LinkedHashMap<>();
        for (Long id : sceneById.keySet()) {
            adjacent.put(id, new ArrayList<>());
        }
        for (Transition transition : transitions) {
            String id = displayId(transition.transitionId());
            if (!Objects.equals(transition.tourId(), tourId)) {
                errors.add("Переход " + id + " относится к другому туру");
            }
            Scene source = sceneById.get(transition.fromSceneId());
            Scene target = sceneById.get(transition.toSceneId());
            if (source == null || target == null) {
                errors.add("У перехода " + id + " неверный конец");
            } else if (Objects.equals(source.tourId(), tourId) && Objects.equals(target.tourId(), tourId)) {
                adjacent.get(source.sceneId()).add(target.sceneId());
            }
        }

        Set<Long> visited = reachableFrom(adjacent, startSceneId);
        List<Long> unreachable = sceneById.keySet().stream().filter(id -> !visited.contains(id)).toList();
        if (!unreachable.isEmpty()) {
            errors.add("Есть сцены, недостижимые из стартовой");
        }
        return new Validation(errors.isEmpty(), List.copyOf(errors), unreachable);
    }

    private Set<Long> reachableFrom(Map<Long, List<Long>> adjacent, long startSceneId) {
        Set<Long> visited = new HashSet<>();
        if (!adjacent.containsKey(startSceneId)) {
            return visited;
        }

        // A missing ID in a historical input scene can appear as a null key.
        Deque<Long> queue = new LinkedList<>();
        queue.addLast(startSceneId);
        visited.add(startSceneId);
        while (!queue.isEmpty()) {
            for (Long target : adjacent.get(queue.removeFirst())) {
                if (visited.add(target)) {
                    queue.addLast(target);
                }
            }
        }
        return visited;
    }

    private static String displayId(Long id) {
        return id == null ? "None" : id.toString();
    }

    record Validation(boolean ok, List<String> errors, List<Long> unreachableSceneIds) {
    }
}
