import java.io.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class Lab3 {
    private static final String DICT_PATH = "D:\\idea\\oop-lab-03\\Lab3\\dict.txt";

    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        try {
            Dictionary dict = Dictionary.load(DICT_PATH);
            System.out.println("Введите текст (пустая строка — выход):");
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty())
                System.out.println(dict.translate(line));
        } catch (InvalidFileFormatException e) {
            System.err.println("Неверный формат словаря: " + e.getMessage());
        } catch (FileReadException e) {
            System.err.println("Ошибка чтения словаря: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Ошибка ввода: " + e.getMessage());
        }
    }
}

class InvalidFileFormatException extends Exception {
    InvalidFileFormatException(String message) { super(message); }
}

class FileReadException extends Exception {
    FileReadException(String message, Throwable cause) { super(message, cause); }
}

class Dictionary {
    // проверка на корректный ввод регекспом
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{Nd}]+|\\s+|.");

    private final Map<String, String> map = new HashMap<>();
    private int maxWords = 1; // длина самой длинной фразы

    static Dictionary load(String file) throws InvalidFileFormatException, FileReadException {
        List<String> lines;
        try {
            lines = Files.readAllLines(Path.of(file), StandardCharsets.UTF_8);
        } catch (IOException | InvalidPathException e) {
            String reason = switch (e) {
                case NoSuchFileException x -> "файл не найден";
                case AccessDeniedException x -> "нет доступа к файлу";
                case CharacterCodingException x -> "надо поменять кодировку на UTF-8";
                case InvalidPathException x -> "некорректный путь";
                default -> "ошибка чтения";
            };
            throw new FileReadException(reason + ": " + file, e);
        }

        Dictionary d = new Dictionary();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|", -1);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank())
                throw new InvalidFileFormatException(
                        "строка " + (i + 1) + ": ожидается 'слово или выражение | перевод'");
            String key = normalize(parts[0]);
            d.map.put(key, parts[1].strip());
            d.maxWords = Math.max(d.maxWords, key.split(" ").length);
        }
        if (d.map.isEmpty()) throw new InvalidFileFormatException("словарь пуст");
        return d;
    }

    String translate(String text) {
        List<String> t = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        while (m.find()) t.add(m.group());

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < t.size(); ) {
            if (!isWord(t.get(i))) { out.append(t.get(i++)); continue; }

            // наращиваем фразу слово за словом
            String best = t.get(i);
            int bestEnd = i;
            StringBuilder key = new StringBuilder(t.get(i).toLowerCase(Locale.ROOT));
            for (int j = i, n = 1; ; j += 2, n++) {
                String found = map.get(key.toString());
                if (found != null) { best = found; bestEnd = j; }
                boolean canExtend = n < maxWords && j + 2 < t.size()
                        && t.get(j + 1).isBlank() && isWord(t.get(j + 2));
                if (!canExtend) break;
                key.append(' ').append(t.get(j + 2).toLowerCase(Locale.ROOT));
            }
            out.append(best);
            i = bestEnd + 1;
        }
        return out.toString();
    }

    private static boolean isWord(String s) {
        return Character.isLetterOrDigit(s.codePointAt(0));
    }

    private static String normalize(String phrase) {
        return String.join(" ", phrase.strip().toLowerCase(Locale.ROOT).split("\\s+"));
    }
}