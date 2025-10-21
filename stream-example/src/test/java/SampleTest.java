import org.junit.jupiter.api.Test;

public class SampleTest {
    @Test
    void test() {
        int size = 10240;
        int[][] array = new int[size][size];

        long beforeTime = System.currentTimeMillis();

        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                array[i][j]++;
            }
        }

        long afterTime = System.currentTimeMillis();
        long diffTime = afterTime - beforeTime;

        System.out.println("수행시간(m) : " + diffTime); // 577ms
    }

    @Test
    void test2() {
        int size = 10240;
        int[][] array = new int[size][size];

        long beforeTime = System.currentTimeMillis();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                array[i][j]++;
            }
        }

        long afterTime = System.currentTimeMillis();
        long diffTime = afterTime - beforeTime;

        System.out.println("수행시간(m) : " + diffTime); // 28ms
    }
}
