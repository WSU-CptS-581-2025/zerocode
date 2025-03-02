package org.jsmart.zerocode.core.engine.sorter;

import org.jsmart.zerocode.core.domain.Step;

public interface ZeroCodeSorter {

    String sortArrayAndReplaceInResponse(Step thisStep, String results, String resolvedScenarioState);
}
