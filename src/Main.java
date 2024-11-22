import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

public class Main {
    // Блокирующие блоки для текстов
    private static final BlockingQueue<String> queueA = new ArrayBlockingQueue<>(100);
    private static final BlockingQueue<String> queueB = new ArrayBlockingQueue<>(100);
    private static final BlockingQueue<String> queueC = new ArrayBlockingQueue<>(100);

    // Метод для генерации текста
    public static String generateText(String letters, int length) {
        Random random = new Random();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append(letters.charAt(random.nextInt(letters.length())));
        }
        return text.toString();
    }

    public static void main(String[] args) throws InterruptedException {
        // Создание потокa
        ThreadFactory threadFactory = r -> new Thread(r, "TextGenerator");
        Thread generatorThread = threadFactory.newThread(new TextGenerator("abc", 100_000, 10_000));
        generatorThread.start();

        // Потоки для анализа текстов
        Thread analyzerA = new Thread(new SymbolAnalyzer(queueA, 'a'));
        Thread analyzerB = new Thread(new SymbolAnalyzer(queueB, 'b'));
        Thread analyzerC = new Thread(new SymbolAnalyzer(queueC, 'c'));

        analyzerA.start();
        analyzerB.start();
        analyzerC.start();

        // Ожидаем завершения генератора
        generatorThread.join();

        analyzerA.interrupt();
        analyzerB.interrupt();
        analyzerC.interrupt();

        analyzerA.join();
        analyzerB.join();
        analyzerC.join();

    }

    // Генерация текстов и добавление их в очередь
    private static class TextGenerator implements Runnable {
        private final String letters;
        private final int length;
        private final int numTexts;

        public TextGenerator(String letters, int length, int numTexts) {
            this.length = length;
            this.numTexts = numTexts;
            this.letters = letters;
        }

        @Override
        public void run() {
            for (int i = 0; i < numTexts; i++) {
                String text = generateText(letters, length);
                try {
                    queueA.put(text);
                    queueB.put(text);
                    queueC.put(text);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // Поток для анализа текстов
    private static class SymbolAnalyzer implements Runnable {
        private final BlockingQueue<String> queue;
        private final char symbol;
        private int maxCount = 0;
        private String maxText = "";

        private SymbolAnalyzer(BlockingQueue<String> queue, char symbol) {
            this.queue = queue;
            this.symbol = symbol;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String text = queue.take();
                    int count = countSymbol(text, symbol);
                    if (count > maxCount) {
                        maxCount = count;
                        maxText = text;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("Максимальное количество " + symbol + "': " + maxCount + " в тексте " + maxText.substring(0, 100)
                    + " ...");
        }

        private int countSymbol(String text, char symbol) {
            int count = 0;
            for (char c : text.toCharArray()) {
                if (c == symbol) {
                    count++;
                }
            }
            return count;
        }
    }

}
