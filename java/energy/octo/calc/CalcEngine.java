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

    enum ErrorKind {
        FORMAT,
        DIV_ZERO,
        DOMAIN,
        UNDEFINED
    }

    static final class Error extends Exception {
        final ErrorKind kind;

        Error(ErrorKind kind, String message) {
            super(message);
            this.kind = kind;
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

    static String trimTrailingOperators(String expression) {
        if (expression == null) {
            return "";
        }
        int end = expression.length();
        while (end > 0) {
            char c = expression.charAt(end - 1);
            if (Character.isWhitespace(c) || c == '+' || c == '-' || c == '\u2212'
                    || c == '\u00d7' || c == '*' || c == '\u00f7' || c == '/' || c == '^') {
                end--;
            } else {
                break;
            }
        }
        return expression.substring(0, end).trim();
    }

    static double evaluate(String expression, boolean degrees) throws Error {
        String cleaned = trimTrailingOperators(expression);
        if (cleaned.isEmpty()) {
            throw new Error(ErrorKind.FORMAT, "Erreur de format");
        }
        CalcEngine parser = new CalcEngine(cleaned, degrees);
        double value = parser.parseExpr().value;
        parser.skipSpaces();
        if (parser.pos < parser.src.length()) {
            throw new Error(ErrorKind.FORMAT, "Erreur de format");
        }
        if (!Double.isFinite(value)) {
            throw new Error(ErrorKind.UNDEFINED, "Non d\u00e9fini");
        }
        // Normalise -0.0 en +0.0 (en IEEE 754, -0.0 == 0.0 est vrai).
        if (value == 0.0) {
            value = 0.0;
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
            double res = minus ? left.value - delta : left.value + delta;
            double scale = Math.max(Math.abs(left.value), Math.abs(delta));
            if (Double.isFinite(scale) && scale > 0 && Math.abs(res) < 1e-12 * scale) {
                res = 0.0;
            }
            left = new Val(res);
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
                        throw new Error(ErrorKind.DIV_ZERO, "Div par 0 impossible");
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
                throw new Error(ErrorKind.DOMAIN, "Erreur de domaine");
            }
            if (base.value == 0 && exp.value < 0) {
                throw new Error(ErrorKind.DIV_ZERO, "Div par 0 impossible");
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
            throw new Error(ErrorKind.FORMAT, "Erreur de format");
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
                throw new Error(ErrorKind.DOMAIN, "Erreur de domaine");
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
        throw new Error(ErrorKind.FORMAT, "Erreur de format");
    }

    private Val parseFunction() throws Error {
        if (match("sin\u207b\u00b9(")) {
            return new Val(toAngle(Math.asin(readArgument(true))));
        }
        if (match("cos\u207b\u00b9(")) {
            return new Val(toAngle(Math.acos(readArgument(true))));
        }
        if (match("tan\u207b\u00b9(")) {
            return new Val(toAngle(Math.atan(readArgument(false))));
        }
        if (match("asin(")) {
            return new Val(toAngle(Math.asin(readArgument(true))));
        }
        if (match("acos(")) {
            return new Val(toAngle(Math.acos(readArgument(true))));
        }
        if (match("atan(")) {
            return new Val(toAngle(Math.atan(readArgument(false))));
        }
        if (match("sin(")) {
            return new Val(evalSin(readArgument(false)));
        }
        if (match("cos(")) {
            return new Val(evalCos(readArgument(false)));
        }
        if (match("tan(")) {
            return new Val(evalTan(readArgument(false)));
        }
        if (match("ln(")) {
            double x = readArgument(false);
            if (x <= 0) {
                throw new Error(ErrorKind.DOMAIN, "Erreur de domaine");
            }
            return new Val(Math.log(x));
        }
        if (match("log(")) {
            double x = readArgument(false);
            if (x <= 0) {
                throw new Error(ErrorKind.DOMAIN, "Erreur de domaine");
            }
            return new Val(Math.log10(x));
        }
        throw new Error(ErrorKind.FORMAT, "Erreur de format");
    }

    private double readArgument(boolean inverseTrig) throws Error {
        double value = parseExpr().value;
        if (inverseTrig && (value < -1 || value > 1)) {
            throw new Error(ErrorKind.DOMAIN, "Erreur de domaine");
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
        throw new Error(ErrorKind.FORMAT, "Erreur de format");
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
            throw new Error(ErrorKind.FORMAT, "Erreur de format");
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
            throw new Error(ErrorKind.FORMAT, "Erreur de format");
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

    private static double degreeMod180(double arg) {
        return ((arg % 180.0) + 180.0) % 180.0;
    }

    private static boolean isDegreePiMultiple(double arg, double mod) {
        return Math.abs(arg) >= 1.0 && (mod < 1e-12 || Math.abs(mod - 180.0) < 1e-12);
    }

    private static Long nearHalfPiMultiple(double arg) {
        double halfPi = Math.PI / 2.0;
        double k = Math.rint(arg / halfPi);
        if (k != 0.0 && Math.abs(k) < 1e15
                && Math.abs(arg - k * halfPi) <= Math.max(1e-14, Math.abs(arg) * 1e-14)) {
            return (long) k;
        }
        return null;
    }

    private double evalSin(double arg) {
        if (degrees) {
            double mod = degreeMod180(arg);
            if (isDegreePiMultiple(arg, mod)) {
                return 0.0;
            }
            return Math.sin(Math.toRadians(arg));
        }
        Long k = nearHalfPiMultiple(arg);
        if (k != null && (k % 2 == 0)) {
            return 0.0;
        }
        return Math.sin(arg);
    }

    private double evalCos(double arg) {
        if (degrees) {
            if (Math.abs(degreeMod180(arg) - 90.0) < 1e-12) {
                return 0.0;
            }
            return Math.cos(Math.toRadians(arg));
        }
        Long k = nearHalfPiMultiple(arg);
        if (k != null && (k % 2 != 0)) {
            return 0.0;
        }
        return Math.cos(arg);
    }

    private double evalTan(double arg) throws Error {
        if (degrees) {
            double mod = degreeMod180(arg);
            if (Math.abs(mod - 90.0) < 1e-12) {
                throw new Error(ErrorKind.UNDEFINED, "Non d\u00e9fini");
            }
            if (isDegreePiMultiple(arg, mod)) {
                return 0.0;
            }
            return Math.tan(Math.toRadians(arg));
        }
        Long k = nearHalfPiMultiple(arg);
        if (k != null) {
            if (k % 2 != 0) {
                throw new Error(ErrorKind.UNDEFINED, "Non d\u00e9fini");
            }
            return 0.0;
        }
        return Math.tan(arg);
    }

    private double toAngle(double radians) {
        return degrees ? Math.toDegrees(radians) : radians;
    }

    private double factorial(double value) throws Error {
        if (value < 0 || value != Math.rint(value) || value > 170) {
            throw new Error(ErrorKind.DOMAIN, "Erreur de domaine");
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
        BigDecimal bd = BigDecimal.valueOf(value).round(MC).stripTrailingZeros();
        String s = bd.toPlainString();
        return s.replace('.', ',');
    }
}
