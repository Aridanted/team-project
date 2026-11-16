package entity.expression;

/** Converts Boolean expression text into an expression that can be evaluated. */
public class ExpressionParser {
    /**
     * Parses the input into a Boolean expression that can be evaluated.
     *
     * @param input the expression text
     * @return the parsed expression
     * @throws ExpressionParseException if the input contains invalid syntax
     */
    public BooleanExpression parse(String input) {
        return new Parser(input).parse();
    }

    private static final class Parser {
        private final String input;
        private int position;

        private Parser(String input) {
            this.input = input;
        }

        private BooleanExpression parse() {
            BooleanExpression expression = parseOr();
            skipWhitespace();
            if (position < input.length()) {
                throw new ExpressionParseException("Unexpected character", position);
            }
            return expression;
        }

        private BooleanExpression parseOr() {
            BooleanExpression left = parseXor();
            while (match('|')) {
                BooleanExpression right = parseXor();
                left = new BinaryExpression(left, BinaryExpression.Operator.OR, right);
            }
            return left;
        }

        private BooleanExpression parseXor() {
            BooleanExpression left = parseAnd();
            while (match('^')) {
                BooleanExpression right = parseAnd();
                left = new BinaryExpression(left, BinaryExpression.Operator.XOR, right);
            }
            return left;
        }

        private BooleanExpression parseAnd() {
            BooleanExpression left = parseNot();
            while (match('&')) {
                BooleanExpression right = parseNot();
                left = new BinaryExpression(left, BinaryExpression.Operator.AND, right);
            }
            return left;
        }

        private BooleanExpression parseNot() {
            if (match('!') || match('~')) {
                return new NotExpression(parseNot());
            }
            return parsePrimary();
        }

        private BooleanExpression parsePrimary() {
            if (match('(')) {
                BooleanExpression expression = parseOr();
                if (!match(')')) {
                    throw new ExpressionParseException("Expected ')'", position);
                }
                return expression;
            }

            if (position == input.length()) {
                throw new ExpressionParseException("Expected an expression", position);
            }

            char current = input.charAt(position);
            if (current == '0' || current == '1') {
                position++;
                return new ConstantExpression(current == '1');
            }
            if (Character.isLetter(current) || current == '_') {
                return parseVariable();
            }
            throw new ExpressionParseException("Expected a variable, constant, or '('", position);
        }

        private BooleanExpression parseVariable() {
            int start = position;
            while (position < input.length()) {
                char current = input.charAt(position);
                if (!Character.isLetterOrDigit(current) && current != '_') {
                    break;
                }
                position++;
            }
            return new Variable(input.substring(start, position));
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }
    }
}
