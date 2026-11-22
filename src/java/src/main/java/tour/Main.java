package tour;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IOException | IllegalArgumentException | JsonParseException error) {
            System.err.println("Ошибка: " + error.getMessage());
            System.exit(2);
        }
    }

    private static void run(String[] args) throws IOException {
        String input = "example_input.json";
        String output = "result.json";

        for (int i = 0; i < args.length; i++) {
            String[] parts = args[i].split("=", 2);
            String option = parts[0];
            if (args[i].equals("-h") || args[i].equals("--help")) {
                System.out.println("Запуск: java -jar target/virtual-tour.jar [--input файл.json] [--output файл.json]");
                return;
            }
            if (!option.equals("--input") && !option.equals("--output")) {
                throw new IllegalArgumentException("Неизвестный параметр: " + option);
            }
            if (parts.length == 1 && (i + 1 == args.length || args[i + 1].startsWith("--"))) {
                throw new IllegalArgumentException("Укажите путь к файлу после " + option);
            }
            String value = parts.length == 2 ? parts[1] : args[++i];
            if (option.equals("--input")) {
                input = value;
            } else {
                output = value;
            }
        }

        TourJson json = new TourJson();
        TourWorkflow.Result result = new TourWorkflow().run(json.read(Path.of(input)));
        json.write(Path.of(output), result);

        System.out.println("Предложений помещений: " + result.roomSuggestions().size());
        System.out.println("Создано сцен: " + result.scenes().size() + "; проверка: успешно");
        TourVersion version = result.publishedVersion();
        System.out.println("Опубликована версия " + version.versionNumber() + ": " + version.url());
        System.out.println("Файл результата: " + output);
    }
}
