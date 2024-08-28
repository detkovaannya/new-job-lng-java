package org.example;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Основной метод программы
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar -Xmx1G target\\GroupingTool-1.0.jar <output-file>.txt");
            return;
        }

        long startTime = System.currentTimeMillis();

        String fileName = "lng-4.txt.gz";

        Set<String> uniqueLines = new HashSet<>();

        System.out.println("Loading lines from file to save unique and valid lines only...");
        try (InputStream fileStream = Main.class.getClassLoader().getResourceAsStream(fileName)) {
            assert fileStream != null;
            try (GZIPInputStream gzipStream = new GZIPInputStream(fileStream);
                 InputStreamReader decoder = new InputStreamReader(gzipStream);
                 BufferedReader buffered = new BufferedReader(decoder)) {
                String line;
                while ((line = buffered.readLine()) != null) {
                    if (isValid(line)) {
                        uniqueLines.add(line);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.printf("Number of prepared lines = %d.%n", uniqueLines.size());

        System.out.println("Lines processing: grouping...");
        // формирование групп из полученного файла
        List<Set<String>> groups = groupLines(uniqueLines);
        // сортировка списка групп по размеру
        groups.sort((a, b) -> b.size() - a.size());

        int groupCount = 0;
        String outputFile = args[0];
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            for (int i = 0; i < groups.size(); i++) {
                Set<String> group = groups.get(i);
                if (group.size() > 1) {
                    groupCount++;
                    writer.write("Group " + (i + 1) + "\n");
                    for (String line : group) {
                        writer.write(line + "\n");
                    }
                    writer.write("\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Groups with more than one element: " + groupCount + ".");
        System.out.println("Execution time: " + (endTime - startTime) / 1000 + "s.");
    }


    /**
     * Валидация считываемых строк из файла
     * @param line считываемая строка
     * @return true - строка корректна, false - строка некорректна
     */
    private static boolean isValid(String line) {
        return line.matches("(\"\\d*\";)*(\"\\d*\")");
    }


    /**
     * Метод, группирующий строки
     * @param lines набор строк, считанных с файла
     * @return список групп
     */
    private static List<Set<String>> groupLines(Set<String> lines) {
        Map<Integer, Set<String>> groups = new HashMap<>();
        final Integer[] totalGroupAmount = {1};
        Map<Integer, Map<String, Integer>> columnMapWordGroup = new HashMap<>();

        lines.forEach(line -> {
            // значения в каждой колонке текущей строки
            String[] columnValues = line.split(";");
            Map<Integer, String> columnValueMap = new HashMap<>();
            for (int j = 0; j < columnValues.length; j ++) {
                // пустые строки не добавляем
                if (!Objects.equals(columnValues[j], "\"\"")) {
                    columnValueMap.put(j, columnValues[j]);
                }
            }

            Integer groupNumber = null;

            // проходим по всем номерам колонок текущей строки
            for (Integer column: columnValueMap.keySet()) {
                // если такой колонки еще нет в общем списке колонок, то добавим
                columnMapWordGroup.computeIfAbsent(column, k -> new HashMap<>());

                // находим значение в текущей колонке в общей мапе и проверяем,
                // какой группе соответствует это значение (одна строка формата "***********")
                if (columnMapWordGroup.get(column).containsKey(columnValueMap.get(column))) {
                    groupNumber = columnMapWordGroup.get(column).get(columnValueMap.get(column));
                    if (groupNumber != null) {
                        groups.get(groupNumber).add(line);
                        break;
                    }
                } else {
                    // если такой строки еще нет, то добавляем без номера группы, так как еще не знаем
                    columnMapWordGroup.get(column).put(columnValueMap.get(column), null);
                }
            }

            // если группа определена, то обновляем мапу со всеми колонками
            if (groupNumber != null) {
                for (Integer column: columnValueMap.keySet()) {
                    columnMapWordGroup.get(column).put(columnValueMap.get(column), groupNumber);
                }
            } else { // иначе создаем новую группу и обновляем мапу с колонками
                groups.put(totalGroupAmount[0], new HashSet<>());
                groups.get(totalGroupAmount[0]).add(line);
                for (Integer column: columnValueMap.keySet()) {
                    columnMapWordGroup.get(column).put(columnValueMap.get(column), totalGroupAmount[0]);
                }
                totalGroupAmount[0]++;
            }
        });

        return new ArrayList<>(groups.values());
    }
}