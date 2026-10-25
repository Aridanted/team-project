import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;

import java.io.File;

/** Re-saves each given .circ file through Logisim's own loader and writer: Normalize in out [in out ...]. */
public class Normalize {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i += 2) {
            Loader loader = new Loader(null);
            LogisimFile file = loader.openLogisimFile(new File(args[i]));
            if (!loader.save(file, new File(args[i + 1]))) {
                throw new IllegalStateException("save failed for " + args[i]);
            }
            System.out.println("saved " + args[i + 1]);
        }
        System.exit(0);
    }
}
