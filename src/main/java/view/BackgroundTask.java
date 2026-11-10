package view;

import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs slow work (opening files, AI calls) off the event thread so the window never freezes,
 * then runs {@code whenDone} back on the event thread.
 */
final class BackgroundTask {

    private BackgroundTask() {
    }

    /**
     * @param work     runs on a background thread
     * @param whenDone runs on the event thread afterwards, even if {@code work} failed
     * @param onError  runs on the event thread with the message of an unexpected failure
     */
    static void run(Runnable work, Runnable whenDone, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                work.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    onError.accept("Something went wrong: " + cause.getMessage());
                } finally {
                    whenDone.run();
                }
            }
        }.execute();
    }
}
