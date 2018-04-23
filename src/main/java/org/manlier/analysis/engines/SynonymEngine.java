package org.manlier.analysis.engines;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public interface SynonymEngine {

	void scanThesaurus(Consumer<String> consumer) throws IOException;
}
