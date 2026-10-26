package com.lino.skills.domain;

import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.FormulaParser;

import java.util.Map;

public record TalentDefinition(
        String id,
        String title,
        String summary,
        Map<String, String> formulas
) {
    public String formatSummary(int level) {
        String output = summary;
        for (Map.Entry<String, String> entry : formulas.entrySet()) {
            double val = FormulaParser.compute(entry.getValue(), level);
            output = output.replace("{" + entry.getKey() + "}", ColorFormatter.formatDecimal(val));
        }
        return output;
    }
}
