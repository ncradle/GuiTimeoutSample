package ThreadPattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainFrame extends JFrame implements ActionListener {
    public static final int TIME_OUT_MILLIS = 5000;
    public static final int MIDDLE_TASK_TIME_MILLIS = 2000;
    public static final int DISPLAY_TIME_MILLIS = 2000;
    final JButton oButtonStartProcess;
    final JButton oButtonCancelProcess;
    final JLabel oLabel;
    final ExecutorService oService;
    final WatchDog oWatchDog;

    public static void main(String[] args) {
        new MainFrame();
    }


    public MainFrame() {
        getContentPane().setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        oButtonStartProcess = new JButton("Start");
        oButtonStartProcess.addActionListener(this);
        getContentPane().add(BorderLayout.NORTH, oButtonStartProcess);

        oLabel = new JLabel("Ok");
        getContentPane().add(BorderLayout.CENTER, oLabel);

        oButtonCancelProcess = new JButton("Cancel");
        oButtonCancelProcess.addActionListener(new CancelAction());
        getContentPane().add(BorderLayout.SOUTH, oButtonCancelProcess);

        setTitle("MainFrame");
        setSize(300, 300);
        setVisible(true);
        oService = Executors.newCachedThreadPool();
        oWatchDog = new WatchDog(TIME_OUT_MILLIS);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (oWatchDog.isProcess()) {
            System.out.println("Process is not finished!");
            return;
        }

        SwingUtilities.invokeLater(() -> oLabel.setText("Now Loading"));
        oWatchDog.start();
        oService.submit(new MiddleTask());
    }

    public class WatchDog {
        TimerWithMsg oTimer;
        private volatile boolean oIsProcess;
        private volatile boolean oIsWaitDone;
        final int oTimeOutMillis;

        public WatchDog(int aTimeOutMillis) {
            oTimeOutMillis = aTimeOutMillis;
        }

        /**
         * This is needed to start
         */
        public void start() {
            oIsProcess = true;
            oIsWaitDone = false;
            // Timer/TimerTask is not reusable. Need to create new one.
            oTimer = new TimerWithMsg("Timeout");
            oTimer.schedule(new Timeout(), oTimeOutMillis);
            System.out.println("Process start");
        }

        /**
         * Cancel process.
         * This can be called repeatedly
         */
        public void cancel() {
            oTimer.cancel();
            oIsWaitDone = true;
        }

        public void end() {
            oIsProcess = false;
            oIsWaitDone = true;
            oTimer.cancel();
            System.out.println("Process end");
        }

        public boolean isProcess() {
            return oIsProcess;
        }

        public boolean isWaitDone() {
            return oIsWaitDone;
        }
    }

    public static class TimerWithMsg extends Timer {
        public TimerWithMsg(String aName) {
            super(aName);
        }

        @Override
        public void cancel() {
            super.cancel();
            System.out.println("Timer is canceled");
        }
    }

    public class Timeout extends TimerTask {
        @Override
        public void run() {
            oWatchDog.cancel();
            SwingUtilities.invokeLater(() -> oLabel.setText("Process timeOut"));
        }
    }

    class CancelAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            oWatchDog.cancel();
            SwingUtilities.invokeLater(() -> oLabel.setText("Process is canceled"));
        }
    }

    public class MiddleTask implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            System.out.println("Middle task starts");
            Thread.sleep(MIDDLE_TASK_TIME_MILLIS);
            // Canceled by timer or cancel action
            if (oWatchDog.isWaitDone()) {
                System.out.println("Middle task is skipped");
                oWatchDog.end();
                return false;
            } else {
                System.out.println("Middle task finished");
                oService.submit(new FinalTask());
            }
            return true;
        }
    }

    public class FinalTask implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            System.out.println("Final task starts");
            Thread.sleep(DISPLAY_TIME_MILLIS);
            // Canceled by timer or cancel action
            if (oWatchDog.isWaitDone()) {
                System.out.println("Final task is skipped");
            } else {
                SwingUtilities.invokeLater(() -> oLabel.setText("Success"));
                System.out.println("Final task is finished");
            }
            oWatchDog.end();
            return true;
        }
    }
}
