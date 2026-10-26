package com.lino.skills.platform;

public final class FormulaParser {

    private FormulaParser() {
    }

    public static double compute(String expression, double levelValue) {
        if (expression == null || expression.isBlank()) {
            return 0.0;
        }
        String prepared = expression
                .replace("{level}", String.valueOf(levelValue))
                .replace("%level%", String.valueOf(levelValue));
        try {
            return new Cursor(prepared).evaluate();
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private static final class Cursor {
        private final String src;
        private int pos = -1;
        private int ch;

        Cursor(String src) {
            this.src = src;
        }

        void advance() {
            ch = (++pos < src.length()) ? src.charAt(pos) : -1;
        }

        boolean consume(int target) {
            while (ch == ' ') {
                advance();
            }
            if (ch == target) {
                advance();
                return true;
            }
            return false;
        }

        double evaluate() {
            advance();
            return parseAddition();
        }

        double parseAddition() {
            double val = parseMultiplication();
            while (true) {
                if (consume('+')) {
                    val += parseMultiplication();
                } else if (consume('-')) {
                    val -= parseMultiplication();
                } else {
                    return val;
                }
            }
        }

        double parseMultiplication() {
            double val = parseAtom();
            while (true) {
                if (consume('*')) {
                    val *= parseAtom();
                } else if (consume('/')) {
                    double div = parseAtom();
                    val = div != 0.0 ? val / div : 0.0;
                } else if (consume('%')) {
                    double mod = parseAtom();
                    val = mod != 0.0 ? val % mod : 0.0;
                } else {
                    return val;
                }
            }
        }

        double parseAtom() {
            if (consume('+')) {
                return parseAtom();
            }
            if (consume('-')) {
                return -parseAtom();
            }

            double result;
            int startIndex = this.pos;

            if (consume('(')) {
                result = parseAddition();
                consume(')');
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') {
                    advance();
                }
                result = Double.parseDouble(src.substring(startIndex, this.pos));
            } else if (ch >= 'a' && ch <= 'z') {
                while (ch >= 'a' && ch <= 'z') {
                    advance();
                }
                String fn = src.substring(startIndex, this.pos);
                if (consume('(')) {
                    double first = parseAddition();
                    double second = 0.0;
                    if (consume(',')) {
                        second = parseAddition();
                    }
                    consume(')');
                    result = switch (fn) {
                        case "floor" -> Math.floor(first);
                        case "ceil" -> Math.ceil(first);
                        case "round" -> Math.round(first);
                        case "min" -> Math.min(first, second);
                        case "max" -> Math.max(first, second);
                        default -> first;
                    };
                } else {
                    result = 0.0;
                }
            } else {
                result = 0.0;
            }
            return result;
        }
    }
}
