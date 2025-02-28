package TME2_Hejun;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class KMPPatternSearch {

    public static void computeLTS(String pattern, int[] LTS) {
        int M = pattern.length();
        LTS[0] = -1;
        LTS[1] = 0;
        int j = 0;

        for (int i = 2; i <= M; i++) {
            while (j > 0 && pattern.charAt(i - 1) != pattern.charAt(j)) {
                j = LTS[j];
            }
            if (pattern.charAt(i - 1) == pattern.charAt(j)) {
                j++;
            }
            LTS[i] = j;
        }
    }

    public static void computeCarryOver(String pattern, int[] LTS, int[] CarryOver) {
        int M = pattern.length();
        System.arraycopy(LTS, 0, CarryOver, 0, LTS.length);

        for (int i = 1; i <= M; i++) {
            int carryOverI = CarryOver[i];
            if (carryOverI == -1) {
                continue;
            }
            if (carryOverI < 0 || carryOverI >= M) {
                continue;
            }
            if (pattern.charAt(i % M) == pattern.charAt(carryOverI)) {
                if (CarryOver[carryOverI] == -1) {
                    CarryOver[i] = -1;
                } else {
                    CarryOver[i] = LTS[LTS[i]];
                }
            }
        }
    }

    public static void KMPSearchInLine(String text, String pattern, int lineNumber, AtomicInteger failCount) {
        int N = text.length();
        int M = pattern.length();
    
        int[] LTS = new int[M + 1];
        int[] CarryOver = new int[M + 1];
    
        computeLTS(pattern, LTS);
        computeCarryOver(pattern, LTS, CarryOver);
    
        int i = 0;
        int j = 0;
        boolean found = false;
    
        while (i < N) {
            if (j == -1 || pattern.charAt(j) == text.charAt(i)) {
                i++;
                j++;
            } else {
                j = CarryOver[j];
            }
    
            if (j == M) {
                // 打印找到的模式位置及所在行的文本
                System.out.printf("Line %d: \"%s\" \n",lineNumber, text);
                found = true;
                j = CarryOver[j];
            }
        }
    
        if (!found) {
            failCount.incrementAndGet();
        }
    }
    

    public static List<String> readTextFileByLines(String filename) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader file = new BufferedReader(new FileReader(filename))) {
            String buffer;
            while ((buffer = file.readLine()) != null) {
                lines.add(buffer);
            }
        } catch (IOException e) {
            System.out.println("Can't open file " + filename);
            return null;
        }
        return lines;
    }

    public static class ThreadData {
        List<String> lines;
        String pattern;
        AtomicInteger failCount;
        Semaphore previousSem;
        Semaphore currentSem;

        public ThreadData(List<String> lines, String pattern, AtomicInteger failCount, Semaphore previousSem, Semaphore currentSem) {
            this.lines = lines;
            this.pattern = pattern;
            this.failCount = failCount;
            this.previousSem = previousSem;
            this.currentSem = currentSem;
        }
    }

    public static class SearchInThread implements Runnable {
        ThreadData data;

        public SearchInThread(ThreadData data) {
            this.data = data;
        }

        @Override
        public void run() {
            try {
                data.previousSem.acquire();  // 等待前一个线程完成

                // 在每个线程开始时打印对应的 pattern
                System.out.println("Now searching for pattern: " + data.pattern);

                for (int i = 0; i < data.lines.size(); i++) {
                    KMPSearchInLine(data.lines.get(i), data.pattern, i + 1, data.failCount);
                }
                if (data.failCount.get() == data.lines.size()) {
                    System.out.println("404 Text Not Found for pattern: " + data.pattern);
                }

                System.out.println("\n\n");
                data.currentSem.release();  // 通知下一个线程
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        AtomicInteger failCount1 = new AtomicInteger(0);
        AtomicInteger failCount2 = new AtomicInteger(0);
        AtomicInteger failCount3 = new AtomicInteger(0);
        AtomicInteger failCount4 = new AtomicInteger(0);
        AtomicInteger failCount5 = new AtomicInteger(0);

        String pattern1 = "Sargon";
        String pattern2 = "Pizzi";
        String pattern3 = "Pepperoni";
        String pattern4 = "Pizza";
        String pattern5 = "Poppers";

        List<String> lines = readTextFileByLines("TME2_Hejun/56667-0.txt");
        if (lines == null) {
            return;
        }

        Thread[] threads = new Thread[5];
        Semaphore[] semaphores = new Semaphore[6];

        for (int i = 0; i < 6; i++) {
            semaphores[i] = new Semaphore(0);  // 初始化信号量
        }

        // 第一个信号量设置为1，启动第一个线程
        semaphores[0].release();

        ThreadData[] threadData = new ThreadData[]{
                new ThreadData(lines, pattern1, failCount1, semaphores[0], semaphores[1]),
                new ThreadData(lines, pattern2, failCount2, semaphores[1], semaphores[2]),
                new ThreadData(lines, pattern3, failCount3, semaphores[2], semaphores[3]),
                new ThreadData(lines, pattern4, failCount4, semaphores[3], semaphores[4]),
                new ThreadData(lines, pattern5, failCount5, semaphores[4], semaphores[5]),
        };

        // 创建线程
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(new SearchInThread(threadData[i]));
            threads[i].start();
        }

        // 等待所有线程完成
        for (int i = 0; i < 5; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 清理资源
        // 在Java中，Semaphore不需要显式销毁
    }
}
