/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lanchat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mear
 */
public class PeriodicCheckItemsAlive<E extends HasLife> implements Runnable {

    final static private Object STATIC_LOCK = new Object();
    private static Long nextSerial = 0L;
    private final long serial;
    List<E> list;
    final private List<E> removeList = new ArrayList<>();
    final private listObeserver<E> listObs;
    final static long DEFAULT_PERIOD = 1000L; // 1sec
    private long checkingPeriod = DEFAULT_PERIOD;
    private long maxNumOfChecks = 0L;
    private long numOfChecks = 0L;
    private boolean terminated = false;
    private boolean infiniteLoop = false;
    private boolean running = false;

    public PeriodicCheckItemsAlive(List<E> list, listObeserver<E> listObs, long checkingPeriod, long maxNumOfChecks) {
        if (list == null) {
            throw new NullPointerException("List is null!");
        }
        if (listObs == null) {
            throw new NullPointerException("ListObserver is null!");
        }
        synchronized (STATIC_LOCK) {
            serial = nextSerial++;
        }
        this.list = list;
        this.listObs = listObs;
        setCheckingPeriod(checkingPeriod);
        setMaxNumOfChecks(maxNumOfChecks);
    }

    public PeriodicCheckItemsAlive(List<E> list, listObeserver<E> listObs, long checkingPeriod) throws IllegalArgumentException {
        this(list, listObs, checkingPeriod, 0);
    }

    public PeriodicCheckItemsAlive(List<E> list, listObeserver<E> listObs) throws IllegalArgumentException {
        this(list, listObs, DEFAULT_PERIOD);
    }

    public final synchronized long setMaxNumOfChecks(long max) {
        infiniteLoop = (max < 1);
        maxNumOfChecks = infiniteLoop ? 0 : max;
        return maxNumOfChecks;
    }

    public final synchronized long setCheckingPeriod(long period) {
        this.checkingPeriod = (period > 0) ? period : DEFAULT_PERIOD;
        return this.checkingPeriod;
    }

    public long getCheckingPeriod() {
        return checkingPeriod;
    }

    public long getMaxNumOfChecks() {
        return maxNumOfChecks;
    }

    public long getNumOfChecks() {
        return numOfChecks;
    }

    public synchronized void terminate() {
        terminated = true;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public boolean isInfinite() {
        return infiniteLoop;
    }

    public List<E> getList() {
        return list;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public String toString() {
        return "PeriodicCheckItemsAlive Server #" + serial;
    }

    public String getStatus() {
        String statusStr = "";
        if (terminated) {
            statusStr = "Terminated";
        } else if (running) {
            statusStr = "Running";
        } else {
            statusStr = "Finished";
        }
        statusStr += ", Num of checks: " + numOfChecks;
        return statusStr;
    }

    @Override
    public void run() {

        numOfChecks = 0L;
        while (!terminated && (infiniteLoop || numOfChecks < maxNumOfChecks)) {
            running = true;
            synchronized (removeList) {
                removeList.clear();
            }
            Iterator<E> it = list.iterator();
            while (it.hasNext()) {
                E objE = it.next();
                if (!objE.guessIsAlive()) {
                    //it.removeAll();
                    removeList.add(objE);
                    //System.out.println(this.toString()+ ", Added to removeList: " + objE.toString());
                }
            }
            //System.out.println("removeList: " + Arrays.toString(removeList.toArray()));
            listObs.removeAll(list, removeList);
            numOfChecks++;

            try {
                Thread.sleep(checkingPeriod);
            } catch (InterruptedException ex) {
                Logger.getLogger(PeriodicCheckItemsAlive.class.getName()).log(Level.WARNING, null, ex);
            }
        }
        running = false;
    }

}
