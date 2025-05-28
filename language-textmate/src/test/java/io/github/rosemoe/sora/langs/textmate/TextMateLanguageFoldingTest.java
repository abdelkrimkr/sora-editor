package io.github.rosemoe.sora.langs.textmate;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.core.internal.oniguruma.OnigRegExp;
import org.eclipse.tm4e.core.internal.oniguruma.OnigResult;
import org.eclipse.tm4e.core.internal.oniguruma.OnigString;
import org.eclipse.tm4e.languageconfiguration.internal.model.Folding;
import org.eclipse.tm4e.languageconfiguration.internal.model.FoldingMarkers;
import org.eclipse.tm4e.languageconfiguration.internal.model.LanguageConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import io.github.rosemoe.sora.lang.folding.FoldingRegion;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry; // Required for MockedTextMateAnalyzer
import io.github.rosemoe.sora.text.CharsTextContent;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentReference;

import static org.junit.Assert.*;

public class TextMateLanguageFoldingTest {

    private TestTextMateLanguage textMateLanguage;
    private MockedTextMateAnalyzer mockAnalyzer;
    private LanguageConfiguration mockLanguageConfig;

    // Test-specific subclass to allow setting protected/internal fields
    private static class TestTextMateLanguage extends TextMateLanguage {
        public TestTextMateLanguage() {
            // Bypassing grammar loading by preparing to directly set analyzer and lang config.
            // Super constructor needs IGrammar, LanguageConfiguration, GrammarRegistry, ThemeRegistry, boolean
            // These can be null for this specific testing path if we ensure they are not used,
            // or if the methods we call are self-contained.
            // However, TextMateLanguage constructor itself calls createAnalyzerAndNewlineHandler.
            // This is problematic.
            // A better way would be to have a constructor for testing or use a mocking framework.
            // For now, we'll rely on setting the fields post-construction.
            super(null, null, null, null, false); 
        }

        public void setTestTextMateAnalyzer(TextMateAnalyzer analyzer) {
            this.textMateAnalyzer = analyzer;
        }

        public void setTestLanguageConfiguration(LanguageConfiguration config) {
            this.languageConfiguration = config;
        }
    }
    
    // Concrete implementation of ContentReference for tests
    private static class TestContentReference implements ContentReference {
        private final Content content;

        public TestContentReference(String text) {
            this.content = new CharsTextContent();
            this.content.insert(0, 0, text);
        }

        @Override
        public Content get() {
            return content;
        }

        @Override
        public void requireLatest() { /* No-op */ }

        @Override
        public void release() { /* No-op */ }
    }

    // Mocked TextMateAnalyzer that extends TextMateAnalyzer
    // This allows it to be placed in TextMateLanguage.textMateAnalyzer
    // And allows us to control its behavior for FoldingHelper methods.
    public static class MockedTextMateAnalyzer extends TextMateAnalyzer {
        private Map<Integer, Integer> indents = new HashMap<>();
        private Map<Integer, OnigResult> results = new HashMap<>();
        // foldingOffside is a field in TextMateAnalyzer, we can set it if needed for toIndentRanges,
        // but getFoldingRegions gets offSide from LanguageConfiguration.

        public MockedTextMateAnalyzer(TextMateLanguage language, IGrammar grammar, LanguageConfiguration languageConfiguration, ThemeRegistry themeRegistry) {
            super(language, grammar, languageConfiguration, themeRegistry);
        }

        public void setIndentForLine(int line, int indent) {
            indents.put(line, indent);
        }

        public void setResultForLine(int line, OnigResult result) {
            results.put(line, result);
        }

        @Override
        public int getIndentFor(int line) {
            // This is crucial: MyState might not exist if analyze() hasn't run.
            // So, we directly return our mock values.
            return indents.getOrDefault(line, 0);
        }

        @Override
        public OnigResult getResultFor(int line) {
            // Similar to getIndentFor, bypass actual state.
            return results.get(line);
        }
        
        public void clear() {
            indents.clear();
            results.clear();
        }
    }


    @Before
    public void setUp() {
        // We need a TextMateLanguage instance.
        // The constructor of TextMateLanguage calls createAnalyzerAndNewlineHandler, which creates a real TextMateAnalyzer.
        // We need to replace that with our mock.
        textMateLanguage = new TestTextMateLanguage(); // This will init a real analyzer.

        mockLanguageConfig = new LanguageConfiguration();
        // IGrammar and ThemeRegistry can be null if MockedTextMateAnalyzer's super constructor handles it,
        // or if the methods we rely on don't use them.
        // TextMateAnalyzer's constructor uses languageConfig.getFolding(), language.getTabSize().
        mockAnalyzer = new MockedTextMateAnalyzer(textMateLanguage, null, mockLanguageConfig, null);
        
        textMateLanguage.setTestTextMateAnalyzer(mockAnalyzer); // Replace analyzer
        textMateLanguage.setTestLanguageConfiguration(mockLanguageConfig); // Set language config
    }


    @Test
    public void testEmptyInput() {
        ContentReference contentRef = new TestContentReference("");
        mockAnalyzer.clear();
        // Ensure folding rules are minimal or non-existent if not needed
        mockLanguageConfig.setFolding(null);


        List<FoldingRegion> regions = textMateLanguage.getFoldingRegions(contentRef);
        assertNotNull("Regions list should not be null", regions);
        assertTrue("Regions list should be empty for empty input", regions.isEmpty());
    }

    @Test
    public void testNoFoldingRegions() {
        String code = "line1\nline2\nline3";
        ContentReference contentRef = new TestContentReference(code);
        
        mockAnalyzer.clear();
        mockAnalyzer.setIndentForLine(0, 0);
        mockAnalyzer.setIndentForLine(1, 0);
        mockAnalyzer.setIndentForLine(2, 0);
        
        // No folding markers, offside false (default for new Folding())
        Folding foldingRules = new Folding();
        foldingRules.setOffSide(false); // Explicitly not indentation-based for this test
        mockLanguageConfig.setFolding(foldingRules);

        List<FoldingRegion> regions = textMateLanguage.getFoldingRegions(contentRef);
        assertNotNull("Regions list should not be null", regions);
        assertTrue("Regions list should be empty for no folding regions", regions.isEmpty());
    }

    @Test
    public void testIndentationFolding() {
        String code = 
            "level0\n" +      // line 0
            "  level1\n" +    // line 1
            "    level2\n" +  // line 2
            "  level1_end\n" +// line 3
            "level0_end";     // line 4
        ContentReference contentRef = new TestContentReference(code);

        mockAnalyzer.clear();
        // Tab size is used by IndentRange.computeIndentLevel, which our mock bypasses by using setIndentForLine.
        // However, TextMateLanguage.getTabSize() is used by TextMateAnalyzer.
        // Our MockedTextMateAnalyzer inherits tabSize logic if not overridden.
        // The call to `rangesCollector.toIndentRanges` does not directly use tabSize from FoldingHelper.
        // It's used by FoldingHelper.getIndentFor, which we are mocking.

        mockAnalyzer.setIndentForLine(0, 0); 
        mockAnalyzer.setIndentForLine(1, 2); 
        mockAnalyzer.setIndentForLine(2, 4);
        mockAnalyzer.setIndentForLine(3, 2);
        mockAnalyzer.setIndentForLine(4, 0);
        
        Folding foldingRules = new Folding();
        foldingRules.setOffSide(true); // Key for indentation-based folding
        mockLanguageConfig.setFolding(foldingRules);
        // textMateLanguage.setTabSize(2); // Ensure tabSize is configured if indents are based on it.

        List<FoldingRegion> regions = textMateLanguage.getFoldingRegions(contentRef);
        assertNotNull(regions);
        
        // Expected for offSide=true:
        // Line 0 (indent 0) folds lines 1-3 (indents 2,4,2) -> region(0,3)
        // Line 1 (indent 2) folds line 2 (indent 4) -> region(1,2)
        assertEquals("Expected 2 folding regions for indentation", 2, regions.size());

        boolean foundRegion0_3 = false;
        boolean foundRegion1_2 = false;

        for (FoldingRegion region : regions) {
            if (region.getStartLine() == 0 && region.getEndLine() == 3) {
                foundRegion0_3 = true;
            } else if (region.getStartLine() == 1 && region.getEndLine() == 2) {
                foundRegion1_2 = true;
            }
        }
        assertTrue("Expected folding region from line 0 to line 3", foundRegion0_3);
        assertTrue("Expected folding region from line 1 to line 2", foundRegion1_2);
    }

    @Test
    public void testMarkerFolding() {
        String code = 
            "line0 // start-marker\n" + // line 0
            "  line1\n" +              // line 1
            "line2 // end-marker\n" +  // line 2
            "line3";                   // line 3
        ContentReference contentRef = new TestContentReference(code);

        FoldingMarkers markers = new FoldingMarkers();
        String startRegexStr = "^.*//\\s*start-marker\\s*$";
        String endRegexStr = "^.*//\\s*end-marker\\s*$";
        markers.setStart(startRegexStr);
        markers.setEnd(endRegexStr);
        
        Folding foldingRules = new Folding();
        foldingRules.setMarkers(markers);
        foldingRules.setOffSide(false); 
        mockLanguageConfig.setFolding(foldingRules);

        // The TextMateAnalyzer (our mockAnalyzer) would use these regexes to produce OnigResults.
        // We need to provide these OnigResults.
        // The getResultFor() in TextMateLanguage uses the main folding regex from TextMateAnalyzer: new OnigRegExp("(" + markers.markersStart + ")|(?:" + markers.markersEnd + ")");
        // So, the OnigResult should reflect this structure.
        // Group 1 for start marker, Group 2 for end marker.
        OnigRegExp combined = new OnigRegExp("(" + startRegexStr + ")|(" + endRegexStr + ")");

        mockAnalyzer.clear();
        mockAnalyzer.setIndentForLine(0, 0); // Indent shouldn't matter for pure marker folding
        mockAnalyzer.setIndentForLine(1, 2);
        mockAnalyzer.setIndentForLine(2, 0);
        mockAnalyzer.setIndentForLine(3, 0);

        // Line 0: Matches start marker
        OnigResult resultLine0 = combined.search(new OnigString("line0 // start-marker"));
        assertNotNull(resultLine0); // Sanity check regex
        //assertTrue(resultLine0.isBeginning(1)); // Group 1 is start
        mockAnalyzer.setResultForLine(0, resultLine0);

        // Line 1: No marker
        mockAnalyzer.setResultForLine(1, null); // Or OnigResult from no match

        // Line 2: Matches end marker
        OnigResult resultLine2 = combined.search(new OnigString("line2 // end-marker"));
        assertNotNull(resultLine2); // Sanity check regex
        //assertTrue(resultLine2.isBeginning(2)); // Group 2 is end
        mockAnalyzer.setResultForLine(2, resultLine2);
        
        mockAnalyzer.setResultForLine(3, null);


        List<FoldingRegion> regions = textMateLanguage.getFoldingRegions(contentRef);
        assertNotNull(regions);
        assertEquals("Expected 1 folding region for markers", 1, regions.size());
        
        FoldingRegion region = regions.get(0);
        assertEquals("Region should start at line 0", 0, region.getStartLine());
        assertEquals("Region should end at line 2", 2, region.getEndLine());
    }
    
    @Test
    public void testNestedMarkerFolding() {
        String code =
            "item1 // start-outer\n" +    // 0
            "  item2 // start-inner\n" +  // 1
            "  item3\n" +                // 2
            "  item4 // end-inner\n" +    // 3
            "item5 // end-outer\n";      // 4
        ContentReference contentRef = new TestContentReference(code);

        FoldingMarkers markers = new FoldingMarkers();
        String startOuterStr = "^.*//\\s*start-outer\\s*$";
        String endOuterStr = "^.*//\\s*end-outer\\s*$";
        String startInnerStr = "^.*//\\s*start-inner\\s*$";
        String endInnerStr = "^.*//\\s*end-inner\\s*$";
        // For simplicity, let's assume a single pair of markers for now,
        // as RangesCollector handles one set of start/end markers from the FoldingHelper's OnigResult.
        // To test nesting with different markers, the OnigResult from FoldingHelper would need to differentiate them,
        // or the grammar would have hierarchical rules that TextMate itself resolves before this folding stage.
        // The current `getFoldingRegions` uses one `cachedRegExp` in `TextMateAnalyzer`.
        // So, let's use one marker type that can be nested.
        markers.setStart("^.*//\\s*start\\s*$");
        markers.setEnd("^.*//\\s*end\\s*$");

        Folding foldingRules = new Folding();
        foldingRules.setMarkers(markers);
        foldingRules.setOffSide(false);
        mockLanguageConfig.setFolding(foldingRules);

        OnigRegExp combined = new OnigRegExp("(" + markers.getStart() + ")|(" + markers.getEnd() + ")");

        mockAnalyzer.clear();
        // Indents (should not affect marker logic if offSide=false)
        mockAnalyzer.setIndentForLine(0, 0);
        mockAnalyzer.setIndentForLine(1, 2);
        mockAnalyzer.setIndentForLine(2, 2);
        mockAnalyzer.setIndentForLine(3, 2);
        mockAnalyzer.setIndentForLine(4, 0);

        // Set OnigResults
        mockAnalyzer.setResultForLine(0, combined.search(new OnigString("item1 // start")));      // Outer start
        mockAnalyzer.setResultForLine(1, combined.search(new OnigString("  item2 // start")));  // Inner start
        mockAnalyzer.setResultForLine(2, null);
        mockAnalyzer.setResultForLine(3, combined.search(new OnigString("  item4 // end")));    // Inner end
        mockAnalyzer.setResultForLine(4, combined.search(new OnigString("item5 // end")));        // Outer end

        List<FoldingRegion> regions = textMateLanguage.getFoldingRegions(contentRef);
        assertNotNull(regions);
        assertEquals("Expected 2 folding regions for nested markers", 2, regions.size());

        boolean foundOuter = false; // 0-4
        boolean foundInner = false; // 1-3

        for (FoldingRegion region : regions) {
            if (region.getStartLine() == 0 && region.getEndLine() == 4) {
                foundOuter = true;
            } else if (region.getStartLine() == 1 && region.getEndLine() == 3) {
                foundInner = true;
            }
        }
        assertTrue("Expected outer folding region [0-4]", foundOuter);
        assertTrue("Expected inner folding region [1-3]", foundInner);
    }
}
