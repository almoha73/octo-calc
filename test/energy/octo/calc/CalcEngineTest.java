package energy.octo.calc;

/**
 * Pure-Java unit tests for CalcEngine (no Android dependency).
 */
public final class CalcEngineTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        testBasicOperationsAndPrecedence();
        testTrailingOperators();
        testInverseTrig();
        testTrigSpecialAngles();
        testSmallNumbersAndFormatting();
        testCancellation();
        testPowersNegativesAndPostfixes();
        testPercentages();
        testLogsRootsAndDivZero();

        System.out.println("CalcEngineTest: " + passed + "/" + (passed + failed) + " passed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testBasicOperationsAndPrecedence() {
        assertNear("1+2", true, 3.0, 1e-12);
        assertNear("2+3\u00d74", true, 14.0, 1e-12);
        assertNear("(2+3)\u00d74", true, 20.0, 1e-12);
        assertNear("2(3+4)", true, 14.0, 1e-12);
        assertNear("(2)(3)", true, 6.0, 1e-12);
        assertNear("2\u03c0", true, 2.0 * Math.PI, 1e-12);
        assertNear("(2+3", true, 5.0, 1e-12);
        assertNear("10\u00f74", true, 2.5, 1e-12);
        assertNear("7\u221212", true, -5.0, 1e-12);
    }

    private static void testTrailingOperators() {
        assertNear("3+4+", true, 7.0, 1e-12);
        assertNear("3+4\u2212", true, 7.0, 1e-12);
        assertNear("3+4\u00d7", true, 7.0, 1e-12);
        assertNear("3+4\u00f7", true, 7.0, 1e-12);
        assertNear("2^", true, 2.0, 1e-12);
        assertNear("3+4\u00d7\u2212", true, 7.0, 1e-12);
        assertNear("sin(30+", true, 0.5, 1e-12);
        assertError("+", true, CalcEngine.ErrorKind.FORMAT);
        assertError("\u2212", true, CalcEngine.ErrorKind.FORMAT);
        assertError("", true, CalcEngine.ErrorKind.FORMAT);
        assertError("sin(+", true, CalcEngine.ErrorKind.FORMAT);
    }

    private static void testInverseTrig() {
        assertNear("tan\u207b\u00b9(2)", true, Math.toDegrees(Math.atan(2.0)), 1e-12);
        assertNear("tan\u207b\u00b9(-100)", true, Math.toDegrees(Math.atan(-100.0)), 1e-12);
        assertNear("atan(1)", true, 45.0, 1e-12);
        assertNear("atan(1)", false, Math.PI / 4.0, 1e-12);
        assertNear("sin\u207b\u00b9(0,5)", true, 30.0, 1e-12);
        assertNear("cos\u207b\u00b9(-1)", true, 180.0, 1e-12);
        assertError("sin\u207b\u00b9(2)", true, CalcEngine.ErrorKind.DOMAIN);
        assertError("cos\u207b\u00b9(-2)", true, CalcEngine.ErrorKind.DOMAIN);
    }

    private static void testTrigSpecialAngles() {
        assertExactZero("sin(180)", true);
        assertExactZero("sin(360)", true);
        assertExactZero("cos(90)", true);
        assertExactZero("cos(270)", true);
        assertExactZero("tan(180)", true);
        assertError("tan(90)", true, CalcEngine.ErrorKind.UNDEFINED);
        assertError("tan(270)", true, CalcEngine.ErrorKind.UNDEFINED);

        assertExactZero("sin(\u03c0)", false);
        assertExactZero("cos(\u03c0\u00f72)", false);
        assertExactZero("tan(\u03c0)", false);
        assertError("tan(\u03c0\u00f72)", false, CalcEngine.ErrorKind.UNDEFINED);

        assertNonZero("sin(1E-15)", false);
        assertNonZero("sin(1E-15)", true);
    }

    private static void testSmallNumbersAndFormatting() {
        assertNear("10^\u221214", true, 1e-14, 1e-26);
        assertEquals("format 10^-14", "1E-14", CalcEngine.format(eval("10^\u221214", true)));
        assertNear("10^\u221220", true, 1e-20, 1e-32);
        assertNonZero("e^\u221230", true);
        assertEquals("0,1+0,2", "0,3", CalcEngine.format(eval("0,1+0,2", true)));
        assertEquals("-0.0", "0", CalcEngine.format(-0.0));
        assertEquals("0-0", "0", CalcEngine.format(eval("0\u22120", true)));
    }

    private static void testCancellation() {
        assertExactZero("0,1+0,2\u22120,3", true);
        assertExactZero("1,1\u22121\u22120,1", true);
        assertExactZero("0,1+0,2+(\u22120,3)", true);
        assertExactZero("1000+0,1\u22121000\u22120,1", true);
        assertExactZero("\u221a(2)\u00b2\u22122", true);
        assertExactZero("sin(30)\u22120,5", true);
        assertExactZero("0,1\u00d710^\u221214+0,2\u00d710^\u221214\u22120,3\u00d710^\u221214", true);

        assertNear("0+10^\u221214", true, 1e-14, 1e-26);
        assertNear("3\u00d710^\u221214\u22122\u00d710^\u221214", true, 1e-14, 1e-26);
        assertNear("10^\u221214+10^\u221214", true, 2e-14, 1e-26);
        assertNear("2E-15", true, 2e-15, 1e-27);
        assertEquals("format 2E-15", "2E-15", CalcEngine.format(eval("2E-15", true)));
    }

    private static void testPowersNegativesAndPostfixes() {
        assertNear("-5\u00b2", true, -25.0, 1e-12);
        assertNear("(-5)\u00b2", true, 25.0, 1e-12);
        assertNear("2^3^2", true, 512.0, 1e-12);
        assertNear("2^-3", true, 0.125, 1e-12);
        assertNear("5\u00d7-3", true, -15.0, 1e-12);
        assertNear("10\u00f7\u22122", true, -5.0, 1e-12);
        assertNear("0!", true, 1.0, 1e-12);
        assertNear("5!", true, 120.0, 1e-12);
        assertError("(-1)!", true, CalcEngine.ErrorKind.DOMAIN);
        assertError("171!", true, CalcEngine.ErrorKind.DOMAIN);
        assertError("0^-1", true, CalcEngine.ErrorKind.DIV_ZERO);
        assertError("(-2)^0,5", true, CalcEngine.ErrorKind.DOMAIN);
    }

    private static void testPercentages() {
        assertNear("100+20%", true, 120.0, 1e-12);
        assertNear("100-20%", true, 80.0, 1e-12);
        assertNear("100\u00d720%", true, 20.0, 1e-12);
        assertNear("50%", true, 0.5, 1e-12);
        assertNear("100+2\u00d750%", true, 101.0, 1e-12);
        assertNear("20%+10%", true, 0.22, 1e-12);
        assertNear("50%%", true, 0.005, 1e-12);
    }

    private static void testLogsRootsAndDivZero() {
        assertNear("ln(e)", true, 1.0, 1e-12);
        assertNear("log(1000)", true, 3.0, 1e-12);
        assertError("ln(0)", true, CalcEngine.ErrorKind.DOMAIN);
        assertError("log(-1)", true, CalcEngine.ErrorKind.DOMAIN);
        assertNear("\u221a(9)", true, 3.0, 1e-12);
        assertError("\u221a(-1)", true, CalcEngine.ErrorKind.DOMAIN);
        assertError("1\u00f70", true, CalcEngine.ErrorKind.DIV_ZERO);
    }

    private static double eval(String expr, boolean degrees) {
        try {
            return CalcEngine.evaluate(expr, degrees);
        } catch (CalcEngine.Error e) {
            throw new AssertionError("Unexpected error for '" + expr + "': " + e.getMessage(), e);
        }
    }

    private static void assertNear(String expr, boolean degrees, double expected, double tol) {
        try {
            double actual = CalcEngine.evaluate(expr, degrees);
            if (Math.abs(actual - expected) <= tol) {
                passed++;
            } else {
                failed++;
                System.err.println("FAIL [" + expr + "] expected=" + expected + " actual=" + actual);
            }
        } catch (Throwable t) {
            failed++;
            System.err.println("FAIL [" + expr + "] threw " + t);
        }
    }

    private static void assertExactZero(String expr, boolean degrees) {
        try {
            double actual = CalcEngine.evaluate(expr, degrees);
            if (actual == 0.0 && Double.doubleToLongBits(actual) == 0L) {
                passed++;
            } else {
                failed++;
                System.err.println("FAIL [" + expr + "] expected=+0.0 actual=" + actual);
            }
        } catch (Throwable t) {
            failed++;
            System.err.println("FAIL [" + expr + "] threw " + t);
        }
    }

    private static void assertNonZero(String expr, boolean degrees) {
        try {
            double actual = CalcEngine.evaluate(expr, degrees);
            if (actual != 0.0 && Double.isFinite(actual)) {
                passed++;
            } else {
                failed++;
                System.err.println("FAIL [" + expr + "] expected non-zero finite, actual=" + actual);
            }
        } catch (Throwable t) {
            failed++;
            System.err.println("FAIL [" + expr + "] threw " + t);
        }
    }

    private static void assertError(String expr, boolean degrees, CalcEngine.ErrorKind expectedKind) {
        try {
            double actual = CalcEngine.evaluate(expr, degrees);
            failed++;
            System.err.println("FAIL [" + expr + "] expected error " + expectedKind + ", got " + actual);
        } catch (CalcEngine.Error e) {
            if (e.kind == expectedKind) {
                passed++;
            } else {
                failed++;
                System.err.println("FAIL [" + expr + "] expected=" + expectedKind + " actual=" + e.kind);
            }
        } catch (Throwable t) {
            failed++;
            System.err.println("FAIL [" + expr + "] threw unexpected " + t);
        }
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
        } else {
            failed++;
            System.err.println("FAIL [" + label + "] expected='" + expected + "' actual='" + actual + "'");
        }
    }
}
