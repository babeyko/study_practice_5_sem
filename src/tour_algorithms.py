"""Тематические алгоритмы процедурного прототипа виртуального тура."""
from collections import deque
import re


def normalize_name(value):
    """Нормализует название комнаты или подсказку из имени файла."""
    text = str(value or "").rsplit("/", 1)[-1].rsplit("\\", 1)[-1]
    text = re.sub(r"\.[a-z0-9]{1,5}$", "", text.lower())
    text = text.replace("ё", "е")
    return " ".join(re.findall(r"[a-zа-я0-9]+", text))


def suggest_room_matches(panoramas, rooms):
    """Предлагает помещение: 3 — точное имя, 2 — синоним, 0 — вручную."""
    results = []
    for panorama in panoramas:
        name = normalize_name(panorama.get("room_hint") or panorama.get("file_name"))
        candidates = []
        for room in rooms:
            room_name = normalize_name(room.get("name"))
            synonyms = {normalize_name(item) for item in room.get("synonyms", [])}
            synonyms.discard("")
            score = 3 if name and name == room_name else (2 if name in synonyms else 0)
            if score:
                candidates.append((room["room_id"], score))
        best = max((score for _, score in candidates), default=0)
        winners = [room_id for room_id, score in candidates if score == best]
        if best == 0:
            results.append({"panorama_id": panorama["panorama_id"], "room_id": None,
                            "score": 0, "status": "manual_review"})
        elif len(winners) > 1:
            results.append({"panorama_id": panorama["panorama_id"], "room_id": None,
                            "score": best, "status": "ambiguous", "candidate_room_ids": winners})
        else:
            results.append({"panorama_id": panorama["panorama_id"], "room_id": winners[0],
                            "score": best, "status": "suggested"})
    return results


def check_tour_graph(scenes, transitions, start_scene_id, tour_id, panoramas):
    """Проверяет структуру тура и возвращает сцены, недостижимые из старта."""
    errors = []
    scene_by_id = {}
    for scene in scenes:
        scene_id = scene.get("scene_id")
        if scene_id in scene_by_id:
            errors.append(f"Повторяется scene_id={scene_id}")
        scene_by_id[scene_id] = scene
        if scene.get("tour_id") != tour_id:
            errors.append(f"Сцена {scene_id} относится к другому туру")
        if not any(p.get("panorama_id") == scene.get("panorama_id") for p in panoramas):
            errors.append(f"У сцены {scene_id} отсутствует панорама")
    starts = [s.get("scene_id") for s in scenes if s.get("is_start") is True]
    if start_scene_id not in scene_by_id:
        errors.append("Стартовая сцена не существует")
    if len(starts) != 1 or (starts and starts[0] != start_scene_id):
        errors.append("Для тура должна быть назначена ровно одна указанная стартовая сцена")

    adjacency = {scene_id: [] for scene_id in scene_by_id}
    for transition in transitions:
        source, target = transition.get("from_scene_id"), transition.get("to_scene_id")
        if transition.get("tour_id") != tour_id:
            errors.append(f"Переход {transition.get('transition_id')} относится к другому туру")
        if source not in scene_by_id or target not in scene_by_id:
            errors.append(f"У перехода {transition.get('transition_id')} неверный конец")
        elif scene_by_id[source].get("tour_id") == tour_id and scene_by_id[target].get("tour_id") == tour_id:
            adjacency[source].append(target)

    visited = set()
    if start_scene_id in scene_by_id:
        queue = deque([start_scene_id])
        visited.add(start_scene_id)
        while queue:
            current = queue.popleft()
            for target in adjacency[current]:
                if target not in visited:
                    visited.add(target)
                    queue.append(target)
    unreachable = [scene_id for scene_id in scene_by_id if scene_id not in visited]
    if unreachable:
        errors.append("Есть сцены, недостижимые из стартовой")
    return {"ok": not errors, "errors": errors, "unreachable_scene_ids": unreachable}
