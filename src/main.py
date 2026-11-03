"""Запуск демонстрационного сценария процедурного прототипа."""
import argparse
import json
from pathlib import Path

from tour_algorithms import check_tour_graph, suggest_room_matches
from tour_workflow import create_tour_draft, load_panorama_scenes, publish_tour_version


def run(input_path, output_path):
    data = json.loads(Path(input_path).read_text(encoding="utf-8"))
    tours = data.setdefault("tours", [])
    scenes = data.setdefault("scenes", [])
    transitions = data.setdefault("transitions", [])
    versions = data.setdefault("versions", [])
    suggestions = suggest_room_matches(data["panoramas"], data["rooms"])
    room_by_panorama = {item["panorama_id"]: item["room_id"] for item in suggestions
                        if item["status"] == "suggested"}
    tour = create_tour_draft(tours, data["property_id"], data["operator_id"])
    created = load_panorama_scenes(data["panoramas"], scenes, tour["tour_id"],
                                   data["property_id"], room_by_panorama)
    if not created:
        raise ValueError("Для тура нужна хотя бы одна панорама")
    start_scene_id = created[0]["scene_id"]
    created[0]["is_start"] = True
    validation = check_tour_graph(scenes, transitions, start_scene_id, tour["tour_id"], data["panoramas"])
    if not validation["ok"]:
        raise ValueError("Структура тура не прошла проверку: " + "; ".join(validation["errors"]))
    version = publish_tour_version(tours, versions, scenes, transitions, data["panoramas"],
                                   tour["tour_id"], start_scene_id)
    result = {"room_suggestions": suggestions, "tour": tour, "scenes": created,
              "validation": validation, "published_version": version}
    Path(output_path).write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", default="example_input.json", help="Входной JSON")
    parser.add_argument("--output", default="result.json", help="Файл результата")
    args = parser.parse_args()
    try:
        result = run(args.input, args.output)
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        parser.error(str(error))
    print(f"Предложений помещений: {len(result['room_suggestions'])}")
    print(f"Создано сцен: {len(result['scenes'])}; проверка: успешно")
    print(f"Опубликована версия {result['published_version']['version_number']}: {result['published_version']['url']}")
    print(f"Файл результата: {args.output}")


if __name__ == "__main__":
    main()
