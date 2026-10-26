package com.lino.skills.domain;

import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.FormulaParser;

public record AttributeDefinition(
        String id,
        String title,
        String formula,
        String summary,
        int slot,
        String material
) {
    public String formatSummary(double level) {
        double computed = FormulaParser.compute(formula, level);
        return summary.replace("{value}", ColorFormatter.formatDecimal(computed));
    }
}
