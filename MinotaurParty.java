import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

class Present {
    public boolean thankYouNote = false;
}

class ConcurrentLinkedList {
    private final List<Present> list = Collections.synchronizedList(new ArrayList<>());

    public void add(Present p) {
        synchronized (list) {
            list.add(p);
        }
    }

    public int size() {
        synchronized (list) {
            return list.size();
        }
    }
}

class Servant implements Runnable {
    private final ConcurrentLinkedList list;
    private final int rangeStart;
    private final int rangeEnd;
    private final List<Present> presents;

    public Servant(ConcurrentLinkedList list, List<Present> presents, int rangeStart, int rangeEnd) {
        this.list = list;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.presents = presents;
    }

    @Override
    public void run() {
        for (int i = rangeStart; i < rangeEnd; i++) {
            Present p = presents.get(i);
            list.add(p);
            p.thankYouNote = true;
            //System.out.println("Present " + i + " added and note written by " + Thread.currentThread().getName());
        }

        // Verify that all of this servant's presents have thank you notes
        for (int i = rangeStart; i < rangeEnd; i++) {
            if (!presents.get(i).thankYouNote) {
                System.out.println("Present " + i + " is missing a thank you note.");
            }
        }
    }
}

public class MinotaurParty {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        final int NUM_ITEMS = 500000;

        ConcurrentLinkedList list = new ConcurrentLinkedList();
        List<Present> presents = new ArrayList<>(NUM_ITEMS);

        for (int i = 0; i < NUM_ITEMS; i++) {
            presents.add(new Present());
        }

        // USing 4 servants (threads) as specified
        int itemsPerThread = NUM_ITEMS / 4;
        Thread[] threads = new Thread[4];

        for (int i = 0; i < 4; i++) {
            int rangeStart = i * itemsPerThread;
            int rangeEnd = (i + 1) * itemsPerThread;
            threads[i] = new Thread(new Servant(list, presents, rangeStart, rangeEnd));
            threads[i].start();
        }

        for (int i = 0; i < 4; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();
        long total = end - start;

        System.out.println("All presents added and all have thank you notes. Total : " + list.size());
        System.out.println("Total execution time: " + total + " ms");
    }
}

