package kvstore;

import java.util.LinkedList;
import java.util.concurrent.locks.*;


public class ThreadPool {

    /* Array of threads in the threadpool */
    public Thread threads[];
    public LinkedList<Runnable> threadQueue;
    public Lock queueLock;
    public Condition notEmpty;
    public int maxSize;

    /**
     * Constructs a Threadpool with a certain number of threads.
     *
     * @param size number of threads in the thread pool
     */
    public ThreadPool(int size) {
        threads = new Thread[size];
        this.maxSize = size;
        this.threadQueue = new LinkedList<Runnable>();
        this.queueLock = new ReentrantLock();
        this.notEmpty = queueLock.newCondition();
        threads[0] = new WorkerThread(this);
        threads[0].run();
        // implement me
    }

    /**
     * Add a job to the queue of jobs that have to be executed. As soon as a
     * thread is available, the thread will retrieve a job from this queue if
     * if one exists and start processing it.
     *
     * @param r job that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public void addJob(Runnable r) throws InterruptedException {
        this.queueLock.lock();
        this.threadQueue.add(r);
        this.notEmpty.signal();
        this.queueLock.unlock();
        
        
    }

    /**
     * Block until a job is present in the queue and retrieve the job
     * @return A runnable task that has to be executed
     * @throws InterruptedException if thread is interrupted while in blocked
     *         state. Your implementation may or may not actually throw this.
     */
    public Runnable getJob() throws InterruptedException {
        // implement me
        this.queueLock.lock();
        while (this.threadQueue.size() == 0) {
        	this.notEmpty.await();
        }
        Runnable task = this.threadQueue.pop();
        this.queueLock.unlock();
        return task;
        
    }

    /**
     * A thread in the thread pool.
     */
    public class WorkerThread extends Thread {

        public ThreadPool threadPool;

        /**
         * Constructs a thread for this particular ThreadPool.
         *
         * @param pool the ThreadPool containing this thread
         */
        public WorkerThread(ThreadPool pool) {
            threadPool = pool;
        }

        /**
         * Scan for and execute tasks.
         */
        @Override
        public void run() {
        	try {
        		Thread t;
        		boolean flag;
        		while (true) {
            		t = (Thread) threadPool.getJob();
            		flag = false;
            		for (int i = 0; i < threadPool.maxSize; i++) {
                    	if (threadPool.threads[i] == null || !threadPool.threads[i].isAlive()) {
                    		threadPool.threads[i] = t;
                    		flag = true;
                    	}
                    }
            		if (flag) {
            			t.run();
            		}
            		
            	}
            	
        	} catch (InterruptedException e) {
             	System.out.println(e.toString());
             }
        }
    }
}
