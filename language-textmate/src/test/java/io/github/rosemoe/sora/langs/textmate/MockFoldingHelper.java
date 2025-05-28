package io.github.rosemoe.sora.langs.textmate;

import org.eclipse.tm4e.core.internal.oniguruma.OnigResult;
import io.github.rosemoe.sora.langs.textmate.folding.FoldingHelper;
import java.util.HashMap;
import java.util.Map;

public class MockFoldingHelper implements FoldingHelper {

    private Map<Integer, Integer> indents = new HashMap<>();
    private Map<Integer, OnigResult> results = new HashMap<>();
    public boolean offSide = false; // Add this field

    public void setIndentForLine(int line, int indent) {
        indents.put(line, indent);
    }

    public void setResultForLine(int line, OnigResult result) {
        results.put(line, result);
    }

    public void setOffSide(boolean offSide) {
        this.offSide = offSide;
    }

    @Override
    public OnigResult getResultFor(int line) {
        return results.get(line);
    }

    @Override
    public int getIndentFor(int line) {
        return indents.getOrDefault(line, 0);
    }

    public void clear() {
        indents.clear();
        results.clear();
        offSide = false;
    }
}
