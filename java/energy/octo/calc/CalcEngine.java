package energy.octo.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Petite calculatrice recursive descent, sans dependance externe.
 * Gere : + - x /, parentheses, pourcentages, puissances, factorielles,
 * racines, log/ln et trigonometrie (degres ou radians).
 */
final class CalcEngine {

    static final class Error extends Exception {
        Error(String message) {
            super(message);
        }
    }

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private final String src;
    private final boolean degrees;
    private int pos;

    private CalcEngine(String src, boolean degrees) {
        this.src = src;
        this.degrees = degrees;
    }

    static double evaluate(String expression, boolean degrees) throws Error {
        if (expression == null || expression.trim().isEmpty()) {
            throw new Error("Erreur de format");
        }
        CalcEngine parser = new CalcEngine(expression, degrees);
        double value = parser.parseExpr().value;
        parser.skipSpaces();
        if (parser.pos < parser.src.length()) {
            throw new Error("Erreur de format");
        }
        if (!Double.isFinite(value)) {
            throw new Error("Non d\u00e9fini");
        }
        if (Math.abs(value) < 1e-13) {
            value = 0;
        }
        return value;
    }

    private static final class Val {
        double value;
        boolean hasPercent;
        double percentRaw;

        Val(double value) {
            this.value = value;
        }

        static Val percent(double raw) {
            Val v = new Val(raw / 100.0);
            v.hasPercent = true;
            v.percentRaw = raw;
            return v;
        }
    }

    private Val parseExpr() throws Error {
        Val left = parseTerm();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c != '+' && c != '-' && c != '\u2212') {
                break;
            }
            pos++;
            Val right = parseTerm();
            boolean minus = (c == '-' || c == '\u2212');
            double delta;
            if (right.hasPercent) {
                delta = left.value * right.percentRaw / 100.0;
            } else {
                delta = right.value;
            }
            left = new Val(minus ? left.value - delta : left.value + delta);
        }
        return left;
    }

    private Val parseTerm() throws Error {
        Val left = parseUnary();
        boolean combined = false;
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '\u00d7' || c == '*' || c == '\u00f7' || c == '/') {
                pos++;
                Val right = parseUnary();
                if (c == '\u00f7' || c == '/') {
                    if (right.value == 0) {
                        throw new Error("Div par 0 impossible");
                    }
                    left = new Val(left.value / right.value);
                } else {
                    left = new Val(left.value * right.value);
                }
                combined = true;
            } else if (startsPrimary()) {
                Val right = parseUnary();
                left = new Val(left.value * right.value);
                combined = true;
            } else {
                break;
            }
        }
        if (!combined) {
            return left;
        }
        return new Val(left.value);
    }

    private Val parseUnary() throws Error {
        skipSpaces();
        char c = peek();
        if (c == '+' || c == '-' || c == '\u2212') {
            pos++;
            Val inner = parseUnary();
            if (c == '+' ) {
                return inner;
            }
            Val v;
            if (inner.hasPercent) {
                v = Val.percent(-inner.percentRaw);
            } else {
                v = new Val(-inner.value);
            }
            return v;
        }
        return parsePower();
    }

    private Val parsePower() throws Error {
        Val base = parsePostfix();
        skipSpaces();
        if (peek() == '^') {
            pos++;
            Val exp = parseUnary();
            if (base.value < 0 && exp.value != Math.rint(exp.value)) {
                throw new Error("Erreur de domaine");
            }
            if (base.value == 0 && exp.value < 0) {
                throw new Error("Div par 0 impossible");
            }
            return new Val(Math.pow(base.value, exp.value));
        }
        return base;
    }

    private Val parsePostfix() throws Error {
        Val value = parsePrimary();
        while (true) {
            skipSpaces();
            char c = peek();
            if (c == '\u00b2') {
                pos++;
                value = new Val(value.value * value.value);
            } else if (c == '!') {
                pos++;
                value = new Val(factorial(value.value));
            } else if (c == '%') {
                pos++;
                double raw = value.hasPercent ? value.percentRaw / 100.0 : value.value;
                value = Val.percent(raw);
            } else {
                break;
            }
        }
        return value;
    }

    private Val parsePrimary() throws Error {
        skipSpaces();
        if (pos >= src.length()) {
            throw new Error("Erreur de format");
        }
        char c = src.charAt(pos);

        if (Character.isDigit(c) || c == ',' || c == '.') {
            return new Val(parseNumber());
        }
        if (c == '(') {
            pos++;
            double inner = parseExpr().value;
            consumeClose();
            return new Val(inner);
        }
        if (c == '\u03c0') {
            pos++;
            return new Val(Math.PI);
        }
        if (c == '\u221a') {
            pos++;
            double inner = parsePrimary().value;
            if (inner < 0) {
                throw new Error("Erreur de domaine");
            }
            return new Val(Math.sqrt(inner));
        }
        if (c == 'e' || c == 'E') {
            if (pos + 1 < src.length() && src.charAt(pos + 1) == '^') {
                pos++;
                return new Val(Math.E);
            }
            if (!startsFunction()) {
                pos++;
                return new Val(Math.E);
            }
        }
        if (startsFunction()) {
            return parseFunction();
        }
        throw new Error("Erreur de format");
    }

    private Val parseFunction() throws Error {
        if (match("sin\u207b\u00b9(")) {
            return new Val(toAngle(Math.asin(readArgument(true))));
        }
        if (match("cos\u207b\u00b9(")) {
            return new Val(toAngle(Math.acos(readArgument(true))));
        }
        if (match("tan\u207b\u00b9(")) {
            return new Val(toAngle(Math.atan(readArgument(true))));
        }
        if (match("asin(")) {
            return new Val(toAngle(Math.asin(readArgument(true))));
        }
        if (match("acos(")) {
            return new Val(toAngle(Math.acos(readArgument(true))));
        }
        if (match("atan(")) {
            return new Val(toAngle(Math.atan(readArgument(true))));
        }
        if (match("sin(")) {
            return new Val(Math.sin(toRadians(readArgument(false))));
        }
        if (match("cos(")) {
            return new Val(Math.cos(toRadians(readArgument(false))));
        }
        if (match("tan(")) {
            return new Val(Math.tan(toRadians(readArgument(false))));
        }
        if (match("ln(")) {
            double x = readArgument(false);
            if (x <= 0) {
                throw new Error("Erreur de domaine");
            }
            return new Val(Math.log(x));
        }
        if (match("log(")) {
            double x = readArgument(false);
            if (x <= 0) {
                throw new Error("Erreur de domaine");
            }
            return new Val(Math.log10(x));
        }
        throw new Error("Erreur de format");
    }

    private double readArgument(boolean inverseTrig) throws Error {
        double value = parseExpr().value;
        if (inverseTrig && (value < -1 || value > 1)) {
            throw new Error("Erreur de domaine");
        }
        consumeClose();
        return value;
    }

    private boolean startsFunction() {
        String[] names = {
                "sin\u207b\u00b9(", "cos\u207b\u00b9(", "tan\u207b\u00b9(",
                "asin(", "acos(", "atan(", "sin(", "cos(", "tan(", "ln(", "log("
        };
        for (String name : names) {
            if (src.startsWith(name, pos)) {
                return true;
            }
        }
        return false;
    }

    private boolean match(String token) {
        if (src.startsWith(token, pos)) {
            pos += token.length();
            return true;
        }
        return false;
    }

    private void consumeClose() throws Error {
        skipSpaces();
        if (pos < src.length() && src.charAt(pos) == ')') {
            pos++;
            return;
        }
        if (pos >= src.length()) {
            return;
        }
        throw new Error("Erreur de format");
    }

    private double parseNumber() throws Error {
        int start = pos;
        boolean dot = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if ((c == ',' || c == '.') && !dot) {
                dot = true;
                pos++;
            } else {
                break;
            }
        }
        if (pos == start) {
            throw new Error("Erreur de format");
        }
        if (pos < src.length() && (src.charAt(pos) == 'E' || src.charAt(pos) == 'e')) {
            int save = pos;
            pos++;
            if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-' || src.charAt(pos) == '\u2212')) {
                pos++;
            }
            int digits = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
            if (digits == pos) {
                pos = save;
            }
        }
        String raw = src.substring(start, pos)
                .replace(',', '.')
                .replace('\u2212', '-');
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new Error("Erreur de format");
        }
    }

    private boolean startsPrimary() {
        if (pos >= src.length()) {
            return false;
        }
        char c = src.charAt(pos);
        return Character.isDigit(c) || c == ',' || c == '.' || c == '('
                || c == '\u03c0' || c == '\u221a' || c == 'e' || c == 'E'
                || startsFunction();
    }

    private char peek() {
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private void skipSpaces() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }

    private double toRadians(double value) {
        return degrees ? Math.toRadians(value) : value;
    }

    private double toAngle(double radians) {
        return degrees ? Math.toDegrees(radians) : radians;
    }

    private double factorial(double value) throws Error {
        if (value < 0 || value != Math.rint(value) || value > 170) {
            throw new Error("Erreur de domaine");
        }
        double result = 1;
        for (int i = 2; i <= (int) value; i++) {
            result *= i;
        }
        return result;
    }

    static String format(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "Non d\u00e9fini";
        }
        if (value == 0) {
            return "0";
        }
        double abs = Math.abs(value);
        if (abs >= 1e12 || abs < 1e-9) {
            String s = String.format(java.util.Locale.US, "%.9E", value);
            int e = s.indexOf('E');
            String mantissa = s.substring(0, e);
            String exponent = s.substring(e);
            if (mantissa.contains(".")) {
                mantissa = mantissa.replaceAll("0+$", "").replaceAll("\\.$", "");
            }
            exponent = exponent.replace("E+", "E");
            return (mantissa + exponent).replace('.', ',');
        }
        BigDecimal bd = new BigDecimal(value).round(MC).stripTrailingZeros();
        String s = bd.toPlainString();
        return s.replace('.', ',');
    }
}
