"""Процедурные операции для подготовки, проверки и публикации тура."""
from datetime import datetime, timezone
from tour_algorithms import check_tour_graph


def create_tour_draft(tours, property_id, operator_id):
    """Создаёт черновую запись тура и возвращает её."""
    tour_id = max((item.get("tour_id", 0) for item in tours), default=0) + 1
    tour = {"tour_id": tour_id, "property_id": property_id,
            "operator_id": operator_id, "status": "draft"}
    tours.append(tour)
    return tour


def load_panorama_scenes(panoramas, scenes, tour_id, property_id, room_by_panorama=None):
    """Проверяет принадлежность панорам объекту и создаёт сцены для тура."""
    room_by_panorama = room_by_panorama or {}
    next_id = max((item.get("scene_id", 0) for item in scenes), default=0) + 1
    created = []
    for panorama in panoramas:
        if panorama.get("property_id") != property_id:
            raise ValueError(f"Панорама {panorama.get('panorama_id')} относится к другому объекту")
        scene = {"scene_id": next_id, "tour_id": tour_id,
                 "room_id": room_by_panorama.get(panorama.get("panorama_id")),
                 "panorama_id": panorama["panorama_id"],
                 "name": panorama.get("room_hint") or panorama.get("file_name"),
                 "order_index": len(scenes) + len(created) + 1,
                 "is_start": len(scenes) + len(created) == 0}
        created.append(scene)
        next_id += 1
    scenes.extend(created)
    return created


def publish_tour_version(tours, versions, scenes, transitions, panoramas, tour_id, start_scene_id):
    """Публикует версию только после успешной проверки структуры и достижимости."""
    tour = next((item for item in tours if item.get("tour_id") == tour_id), None)
    if tour is None:
        raise ValueError("Тур не найден")
    audit = check_tour_graph(scenes, transitions, start_scene_id, tour_id, panoramas)
    if not audit["ok"]:
        raise ValueError("Публикация запрещена: " + "; ".join(audit["errors"]))
    number = max((item.get("version_number", 0) for item in versions
                  if item.get("tour_id") == tour_id), default=0) + 1
    version = {"version_id": max((item.get("version_id", 0) for item in versions), default=0) + 1,
               "tour_id": tour_id, "version_number": number,
               "url": f"https://example.invalid/tours/{tour_id}/v/{number}",
               "published_at": datetime.now(timezone.utc).isoformat()}
    versions.append(version)
    tour["status"] = "published"
    return version


