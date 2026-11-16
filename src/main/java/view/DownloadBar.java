package view;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * The strip under the workspace with one button per thing the student can save. Each button asks
 * for a file name and writes in the background. When there is nothing to save yet, the bar says
 * what to do first instead of greying the button out.
 */
final class DownloadBar extends JPanel {

    private static final long serialVersionUID = 1L;

    /** What a download writes. It is captured on the event thread and written on a background thread. */
    @FunctionalInterface
    interface Content {
        void writeTo(Path file) throws IOException;
    }

    /**
     * One button on the bar.
     *
     * @param label         the button text, for example "Results (.csv)"
     * @param suggestedName the file name the save dialog starts with; its extension is enforced
     * @param content       what to save, or empty when there is nothing yet
     * @param unavailable   what to tell the student when {@code content} is empty
     */
    record Item(String label, String suggestedName, Supplier<Optional<Content>> content, String unavailable) {
        Item {
            Objects.requireNonNull(label, "label");
            Objects.requireNonNull(suggestedName, "suggestedName");
            Objects.requireNonNull(content, "content");
            Objects.requireNonNull(unavailable, "unavailable");
        }

        String extension() {
            int dot = suggestedName.lastIndexOf('.');
            return dot < 0 ? "" : suggestedName.substring(dot);
        }
    }

    private final JLabel status = Styles.mutedLabel(" ");
    private final JFileChooser chooser = new JFileChooser();

    DownloadBar(List<Item> items) {
        super(new FlowLayout(FlowLayout.LEFT, Styles.GAP, 4));
        setBorder(Styles.padding());
        add(Styles.mutedLabel("Download:"));
        for (Item item : items) {
            JButton button = new JButton(item.label());
            button.addActionListener(e -> download(item, button));
            add(button);
        }
        add(status);
    }

    /** @return text content that is saved as UTF-8 */
    static Content text(String text) {
        return file -> Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    /** @return picture content that is saved as PNG */
    static Content png(BufferedImage image) {
        return file -> {
            if (!ImageIO.write(image, "png", file.toFile())) {
                throw new IOException("PNG is not supported on this computer");
            }
        };
    }

    /** @return {@code file} with {@code extension} added, unless it already ends with it (any case) */
    static File withExtension(File file, String extension) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (extension.isEmpty() || name.endsWith(extension.toLowerCase(Locale.ROOT))) {
            return file;
        }
        return new File(file.getParentFile(), file.getName() + extension);
    }

    private void download(Item item, JButton button) {
        Optional<Content> content = item.content().get();
        if (content.isEmpty()) {
            show(item.unavailable(), true);
            return;
        }
        String extension = item.extension();
        chooser.resetChoosableFileFilters();
        if (!extension.isEmpty()) {
            chooser.setFileFilter(new FileNameExtensionFilter(item.label(), extension.substring(1)));
        }
        chooser.setSelectedFile(new File(item.suggestedName()));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = withExtension(chooser.getSelectedFile(), extension);
        if (file.exists() && JOptionPane.showConfirmDialog(this, file.getName() + " already exists. Replace it?",
                "Replace file", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        save(content.get(), file, button);
    }

    private void save(Content content, File file, JButton button) {
        button.setEnabled(false);
        show("Saving " + file.getName() + "...", false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                content.writeTo(file.toPath());
                return null;
            }

            @Override
            protected void done() {
                button.setEnabled(true);
                try {
                    get();
                    show("Saved " + file.getName(), false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    show("Could not save " + file.getName() + ": " + cause.getMessage(), true);
                }
            }
        }.execute();
    }

    private void show(String message, boolean isError) {
        status.setForeground(isError ? Styles.ERROR : Styles.MUTED);
        Styles.setMessage(status, message);
    }
}
