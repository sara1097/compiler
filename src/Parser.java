import java.util.*;

public class Parser {
    private List<Token> tokens;
    private int currentTokenIndex;
    private Token currentToken;
    private List<String> errors;
    private List<String> matchedRules;
    private int errorCount;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.currentTokenIndex = 0;
        this.errors = new ArrayList<>();
        this.matchedRules = new ArrayList<>();
        this.errorCount = 0;

        if (!tokens.isEmpty()) {
            this.currentToken = tokens.get(currentTokenIndex);
        }
//        else {
//            // Create a special EOF token to avoid null pointer exceptions
//            this.currentToken = new Token("EOF", "END_OF_FILE", 0);
//        }
    }

    public void parseProgram() {
        try {
            program();
            System.out.println("Total NO of errors: " + errorCount);
        } catch (Exception e) {
            System.out.println("Parsing error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void error(String message) {
        if (currentToken != null) {
            String errorMsg = "Line #: " + currentToken.getLine() + " Not Matched: " + message;
            errors.add(errorMsg);
            System.out.println(errorMsg);
            errorCount++;
        } else {
            String errorMsg = "Not Matched: " + message + " (end of file)";
            errors.add(errorMsg);
            System.out.println(errorMsg);
            errorCount++;
        }
    }

    private void matchRule(String rule) {
        if (currentToken != null) {
            String matchedRule = "Line #: " + currentToken.getLine() + " Matched Rule Used: " + rule;
            matchedRules.add(matchedRule);
            System.out.println(matchedRule);
        } else {
            String matchedRule = "Matched Rule Used: " + rule + " (end of file)";
            matchedRules.add(matchedRule);
            System.out.println(matchedRule);
        }
    }

    private void consume() {
        currentTokenIndex++;
        if (currentTokenIndex < tokens.size()) {
            currentToken = tokens.get(currentTokenIndex);
        } else {
            // We're at the end of the token stream
            // Create a special EOF token to avoid null pointer exceptions
            currentToken = new Token("EOF", "END_OF_FILE",
                    tokens.isEmpty() ? 1 : tokens.get(tokens.size() - 1).getLine());
        }
    }

    private boolean match(String type) {
        if (currentToken.getType().equals(type)) {
            consume();
            return true;
        }
        return false;
    }

    private boolean matchText(String text) {
        if (currentToken != null && currentToken.getText().equals(text)) {
            consume();
            return true;
        }
        return false;
    }
    private boolean checkEOF() {
        return currentToken == null || currentToken.getType().equals("END_OF_FILE");
    }
    // Error recovery - skip tokens until finding a synchronization point
    private void synchronize(String... syncTokens) {
        Set<String> syncSet = new HashSet<>(Arrays.asList(syncTokens));

        while (currentToken != null && !syncSet.contains(currentToken.getText()) &&
                !checkEOF()) {
            consume();
        }

        if (currentToken != null && syncSet.contains(currentToken.getText())) {
            consume(); // Consume the synchronization token
        }
    }

    // 1. Program -> Start_Symbols ClassDeclaration End_Symbols .
    private void program() {
        matchRule("Program");
        startSymbols();
        classDeclaration();
        endSymbols();
    }

    // 2. Start_Symbols -> @ | ^
    private void startSymbols() {
        if (currentToken != null && currentToken.getType().equals("Start Symbol")) {
            matchRule("Start_Symbols");
            consume();
        } else {
            error("Expected start symbol (@ or ^)");
            // No need for synchronization here as we're at the beginning
        }
    }

    // 3. End_Symbols -> $ | #
    private void endSymbols() {
        if (currentToken != null && currentToken.getType().equals("End Symbol")) {
            matchRule("End_Symbols");
            consume();
        } else {
            error("Expected end symbol ($ or #)");
        }
    }

    // 4. ClassDeclaration -> Type ID ClassBody | Type ID DerivedFrom ClassBody
    private void classDeclaration() {
        matchRule("ClassDeclaration");

        if (matchText("Type")) {
            if (currentToken != null && currentToken.getType().equals("Identifier")) {
                consume();

                if (currentToken != null && matchText("DerivedFrom")) {
                    if (currentToken != null && currentToken.getType().equals("Identifier")) {
                        consume();
                    } else {
                        error("Expected identifier after DerivedFrom");
                        synchronize("{");
                    }
                }

                classBody();
            } else {
                error("Expected identifier after Type");
                synchronize("{");
            }
        } else {
            error("Expected Type in class declaration");
            synchronize("{", "$", "#");
        }
    }

   //  5. ClassBody -> { ClassMembers }
    private void classBody() {
        matchRule("ClassBody");

        if (matchText("{")) {
            classMembers();

            if (matchText("}")) {
                // Successfully parsed class body
            } else {
                error("Expected } at end of class body");
            }
        } else {
            error("Expected { at beginning of class body");
            synchronize("}", "$", "#");
        }
    }


    // 6. ClassMembers -> ClassMember ClassMembers | ε
//    private void classMembers() {
//        matchRule("ClassMembers");
//
//        // Check if we have a class member
//        if (isClassMemberStart()) {
//            classMember();
//            classMembers();
//        }
//        // If not, it's epsilon (do nothing)
//    }
    private void classMembers() {
        matchRule("ClassMembers");

        // Check if current token could start a class member and we're not at closing brace
        while (currentToken != null &&
                !currentToken.getText().equals("}") &&
                !checkEOF() &&
        isClassMemberStart()) {
            classMember();
        }
        // Implicit ε case when we hit } or end of input
    }

    private boolean isClassMemberStart() {
        if (currentToken == null) return false;
        String type = currentToken.getType();
        String text = currentToken.getText();

        return type.equals("Class") ||
                type.equals("Integer") ||
                type.equals("SInteger") ||
                type.equals("Character") ||
                type.equals("String") ||
                type.equals("Float") ||
                type.equals("SFloat") ||
                type.equals("Void") ||
                type.equals("Boolean") ||
                type.equals("Comment") ||
                text.equals("Require") ||
                (type.equals("Identifier") && !text.equals("}"));
    }

    // 7. ClassMember -> VariableDecl | MethodDecl | FuncCall | Comment | RequireCommand
    private void classMember() {
        matchRule("ClassMember");

        if (currentToken.getType().equals("Comment")) {
            comment();
        } else if (currentToken.getText().equals("Require")) {
            requireCommand();
        } else if (isType()) {
            // Check next tokens to determine if it's a variable or method declaration
            int saveIndex = currentTokenIndex;
            Token saveToken = currentToken;

            consume(); // Skip type
            if (currentTokenIndex < tokens.size() && currentToken.getType().equals("Identifier")) {
                consume(); // Skip ID
                if (currentTokenIndex < tokens.size() && currentToken.getText().equals("(")) {
                    // It's a method declaration
                    currentTokenIndex = saveIndex;
                    currentToken = saveToken;
                    methodDecl();
                } else {
                    // It's a variable declaration
                    currentTokenIndex = saveIndex;
                    currentToken = saveToken;
                    variableDecl();
                }
            } else {
                error("Expected identifier after type");
                currentTokenIndex = saveIndex;
                currentToken = saveToken;
                synchronize(";", "{", "}");
            }
        } else if (currentToken.getType().equals("Identifier")) {
            // Handle unknown type case - give specific error
            error("Unknown type '" + currentToken.getText() + "'");
            synchronize(";", "{", "}");
            //funcCall();
        } else {
            error("Invalid class member");
            synchronize(";", "{", "}");
        }
    }

    private boolean isType() {
        String type = currentToken.getType();
        return type.equals("Integer") ||
                type.equals("SInteger") ||
                type.equals("Character") ||
                type.equals("String") ||
                type.equals("Float") ||
                type.equals("SFloat") ||
                type.equals("Void") ||
                type.equals("Boolean");
    }

    // 8. MethodDecl -> FuncDecl ; | FuncDecl { VariableDecls Statements }
    private void methodDecl() {
        matchRule("MethodDecl");

        funcDecl();

        if (matchText(";")) {
            // Method with just declaration
        } else if (matchText("{")) {
            variableDecls();
            statements();

            if (matchText("}")) {
                // Successfully parsed method body
            } else {
                error("Expected } at end of method body");
                synchronize("}", ";");
            }
        } else {
            error("Expected ; or { after function declaration");
            synchronize("{", ";", "}");
        }
    }

    // 9. FuncDecl -> Type ID ( ParameterList )
    private void funcDecl() {
        matchRule("FuncDecl");

        if (isType()) {
            consume(); // Type

            if (currentToken != null && currentToken.getType().equals("Identifier")) {
                consume(); // ID

                if (matchText("(")) {
                    parameterList();

                    if (matchText(")")) {
                        // Successfully parsed function declaration
                    } else {
                        error("Expected ) at end of parameter list");
                        synchronize(";", "{");
                    }
                } else {
                    error("Expected ( after function name");
                    synchronize(")", ";", "{");
                }
            } else {
                error("Expected identifier for function name");
                synchronize("(", ";", "{");
            }
        } else {
            error("Expected type for function declaration");
            synchronize(";", "{");
        }
    }
    /*
    // 10. ParameterList -> ε | Parameters
    private void parameterList() {
        matchRule("ParameterList");

        if (isType()) {
            parameters();
        }
        // Otherwise it's epsilon (empty)
    }

    // 11. Parameters -> Parameter | Parameters , Parameter
    private void parameters() {
        matchRule("Parameters");

        parameter();

        while (matchText(",")) {
            parameter();
        }
    }

    // 12. Parameter -> Type ID
    private void parameter() {
        matchRule("Parameter");

        if (isType()) {
            consume(); // Type

            if (currentToken != null && currentToken.getType().equals("Identifier")) {
                consume(); // ID
            } else {
                error("Expected identifier in parameter");
                synchronize(",", ")");
            }
        } else {
            error("Expected type in parameter");
            synchronize(",", ")");
        }
    }

    // 13. VariableDecl -> Type IDList ; | Type IDList [ ID ] ;
    private void variableDecl() {
        matchRule("VariableDecl");

        if (isType()) {
            consume(); // Type

            idList();

            // Handle initialization if present
            if (currentToken != null && matchText("=")) {
                expression();
            }
            if (currentToken != null && matchText("[")) {
                if (currentToken != null && (currentToken.getType().equals("Identifier") || currentToken.getType().equals("Constant"))) {
                    consume(); // ID

                    if (matchText("]")) {
                        // Array declaration
                    } else {
                        error("Expected ] in array declaration");
                        synchronize(";");
                    }
                } else {
                    error("Expected identifier in array size");
                    synchronize("]", ";");
                }
            }

            if (matchText(";")) {
                // Successfully parsed variable declaration
            } else {
                error("Expected ; at end of variable declaration");
                synchronize("}", "Ity", "Sity", "Cwq", "CwqSequence", "Ifity", "Sifity", "Valueless", "Logical");
            }
        } else {
            error("Expected type in variable declaration");
            synchronize(";", "{", "}");
        }
    }

    // 14. VariableDecls -> VariableDecl VariableDecls | ε
    private void variableDecls() {
        matchRule("VariableDecls");

        if (isType()) {
            variableDecl();
            variableDecls();
        }
        else if (currentToken != null &&
                currentToken.getType().equals("Identifier")) {
            // This catches `int`, `float`, etc. inside methods
            error("Unknown type '" + currentToken.getText() + "'");
            synchronize(";", "{", "}");
            }
        // Otherwise it's epsilon (empty)
    }

    // 15. IDList -> ID | IDList , ID
    private void idList() {
        matchRule("IDList");

        if (currentToken != null && currentToken.getType().equals("Identifier")) {
            consume(); // ID

            while (currentToken != null && matchText(",")) {
                if (currentToken != null && currentToken.getType().equals("Identifier")) {
                    consume(); // ID
                } else {
                    error("Expected identifier after comma in ID list");
                    break;
                }
            }
        } else {
            error("Expected identifier in ID list");
            synchronize(";", "[", "=");
        }
    }

    // 16. Statements -> Statement Statements | ε
    private void statements() {
        matchRule("Statements");

        while (currentToken != null && !currentToken.getText().equals("}") && isStatementStart()) {
            statement();
        }
        // Otherwise it's epsilon (empty)
    }

    private boolean isStatementStart() {
        String type = currentToken.getType();
        String text = currentToken.getText();

        return type.equals("Identifier") ||
                text.equals("TrueFor") ||
                text.equals("However") ||
                text.equals("When") ||
                text.equals("Respondwith") ||
                text.equals("Endthis") ||
                text.equals("Scan") ||
                text.equals("Srap");
    }

    // 17. Statement -> Assignment | TrueForStmt | HoweverStmt | WhenStmt |
    //                 RespondwithStmt | EndthisStmt | ScanStmt | SrapStmt | FuncCallStmt
    private void statement() {
        matchRule("Statement");

        if (currentToken == null) return;

        String text = currentToken.getText();

        if (currentToken.getType().equals("Identifier")) {
            // Check next token to determine if it's an assignment or function call
            int saveIndex = currentTokenIndex;
            Token saveToken = currentToken;

            consume(); // Skip ID
            if (currentTokenIndex < tokens.size() &&
                    currentToken != null &&
                    currentToken.getText().equals("=")) {

                // It's an assignment
                currentTokenIndex = saveIndex;
                currentToken = saveToken;
                assignment();
            } else if (currentTokenIndex < tokens.size() &&
                    currentToken != null &&
                    currentToken.getText().equals("(")) {

                // It's a function call
                currentTokenIndex = saveIndex;
                currentToken = saveToken;
                funcCallStmt();
            } else {
                error("Expected = or ( after identifier in statement");
                currentTokenIndex = saveIndex;
                currentToken = saveToken;
                synchronize(";");
            }
        } else if (text.equals("TrueFor")) {
            trueForStmt();
        } else if (text.equals("However")) {
            howeverStmt();
        } else if (text.equals("When")) {
            whenStmt();
        } else if (text.equals("Respondwith")) {
            respondwithStmt();
        } else if (text.equals("Endthis")) {
            endthisStmt();
        } else if (text.equals("Scan")) {
            scanStmt();
        } else if (text.equals("Srap")) {
            srapStmt();
        } else {
            error("Invalid statement");
            synchronize(";", "{", "}");
        }
    }

    // 18. Assignment -> ID = Expression ;
    private void assignment() {
        matchRule("Assignment");

        if (currentToken.getType().equals("Identifier")) {
            consume(); // ID

            if (matchText("=")) {
                expression();

                if (matchText(";")) {
                    // Successfully parsed assignment
                } else {
                    error("Expected ; at end of assignment");
                    synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
                }
            } else {
                error("Expected = in assignment");
                synchronize(";");
            }
        } else {
            error("Expected identifier in assignment");
            synchronize(";");
        }
    }

    // 19. FuncCall -> ID ( ArgumentList ) ;
    private void funcCall() {
        matchRule("FuncCall");

        if (currentToken != null && currentToken.getType().equals("Identifier")) {
            consume(); // ID

            if (matchText("(")) {
                argumentList();

                if (matchText(")")) {
                    if (matchText(";")) {
                        // Successfully parsed function call
                    } else {
                        error("Expected ; after function call");
                        synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
                    }
                } else {
                    error("Expected ) at end of argument list");
                    synchronize(";");
                }
            } else {
                error("Expected ( after function name");
                synchronize(";");
            }
        } else {
            error("Expected identifier for function call");
            synchronize(";");
        }
    }

    // 20. FuncCallStmt -> FuncCall ;
    private void funcCallStmt() {
        matchRule("FuncCallStmt");
        funcCall();
    }

    // 21. ArgumentList -> ε | ArgumentSequence
    private void argumentList() {
        matchRule("ArgumentList");

        if (isExpressionStart()) {
            argumentSequence();
        }
        // Otherwise it's epsilon (empty)
    }
    */

    private boolean isValidType() {
        String t = currentToken.getType();
        return t.equals("Integer") || t.equals("SInteger") || t.equals("Float") || t.equals("SFloat")
                || t.equals("Character") || t.equals("String") || t.equals("Void") || t.equals("Boolean");
    }

    private void parameterList() {
        if (currentToken.getText().equals(")")) {
            matchRule("ParameterList -> ε");
            return;
        }
        parameters();
        matchRule("ParameterList -> Parameters");
    }

    private void parameters() {
        parameter();
        while (true) {
            if (matchText(",")) {
                parameter();
            } else if (isValidType()) {
                error("Expected ',' between parameters");
                parameter();
            } else {
                break;
            }
        }
        matchRule("Parameters -> Parameter | Parameters , Parameter");
    }

    private void parameter() {
        if (isValidType()) {
            consume();
            if (match("Identifier")) {
                matchRule("Parameter -> Type ID");
            } else {
                error("Expected ID in parameter");
                synchronize(",", ")");
            }
        } else {
            error("Expected Type in parameter");
            synchronize(",", ")");
        }
    }

    private void variableDecl() {
        if (isValidType()) {
            consume();
            idList();
            if (matchText(";")) {
                matchRule("VariableDecl -> Type IDList ;");
            } else if (matchText("[")) {
                if (match("Identifier") && matchText("]") && matchText(";")) {
                    matchRule("VariableDecl -> Type IDList [ ID ] ;");
                } else {
                    error("Invalid array declaration");
                    synchronize(";", "}");
                }
            } else {
                error("Expected ; or [ in variable declaration");
                synchronize(";", "[", "}");
            }
        } else {
            error("Expected Type in variable declaration");
            synchronize(";", "}");
        }
    }

    private void variableDecls() {
        while (isValidType()) {
            variableDecl();
        }
        matchRule("VariableDecls -> VariableDecl VariableDecls | ε");
    }

    private void idList() {
        if (match("Identifier")) {
            while (matchText(",")) {
                if (!match("Identifier")) {
                    error("Expected ID in IDList");
                    synchronize(",", ";");
                }
            }
            matchRule("IDList -> ID | IDList , ID");
        } else {
            error("Expected ID in IDList");
            synchronize(",", ";");
        }
    }

    private void statements() {
        while (isStatementStart()) {
            statement();
        }
        matchRule("Statements -> Statement Statements | ε");
    }

    private boolean isStatementStart() {
        String t = currentToken.getType();
        return t.equals("Identifier") || t.equals("TrueFor") || t.equals("However") ||
                t.equals("When") || t.equals("Respondwith") || t.equals("Endthis") ||
                t.equals("Scan") || t.equals("Srap");
    }

    private void statement() {
        if (currentToken.getType().equals("Identifier")) {
            if (lookAhead().equals("=")) {
                assignment();
            } else if (lookAhead().equals("(")) {
                funcCallStmt();
            } else {
                error("Invalid statement start");
                synchronize(";", "}");
            }
        } else {
            error("Unsupported statement or syntax error");
            synchronize(";", "}");
        }
        matchRule("Statement -> Assignment | FuncCallStmt | ...");
    }

    private void assignment() {
        if (match("Identifier") && matchText("=")) {
            simpleExpression();
            if (matchText(";")) {
                matchRule("Assignment -> ID = Expression ;");
            } else {
                error("Expected ; after assignment");
                synchronize(";", "}");
            }
        } else {
            error("Invalid assignment");
            synchronize(";", "}");
        }
    }

    private void funcCall() {
        if (match("Identifier") && matchText("(")) {
            argumentList();
            if (matchText(")")) {
                matchRule("FuncCall -> ID ( ArgumentList ) ;");
            } else {
                error("Expected ) after arguments");
                synchronize(")", ";");
            }
        } else {
            error("Invalid function call");
            synchronize(")", ";");
        }
    }

    private void funcCallStmt() {
        funcCall();
        if (matchText(";")) {
            matchRule("FuncCallStmt -> FuncCall ;");
        } else {
            error("Expected ; after function call statement");
            synchronize(";", "}");
        }
    }

    private void argumentList() {
        if (currentToken.getText().equals(")")) {
            matchRule("ArgumentList -> ε");
            return;
        }
        simpleExpression();
        while (matchText(",")) {
            simpleExpression();
        }
        matchRule("ArgumentList -> ArgumentSequence");
    }

    private void simpleExpression() {
        if (match("Identifier") || match("Number")) {
            return;
        } else {
            error("Invalid expression");
            synchronize(",", ";", ")");
        }
    }

    private String lookAhead() {
        if (currentTokenIndex + 1 < tokens.size()) {
            return tokens.get(currentTokenIndex + 1).getText();
        }
        return "";
    }

    private boolean isExpressionStart() {
        if (currentToken == null) return false;

        return currentToken.getType().equals("Identifier") ||
                currentToken.getType().equals("Constant") ||
                currentToken.getType().equals("String Literal") ||
                currentToken.getText().equals("(");
    }

    // 22. ArgumentSequence -> Expression | ArgumentSequence , Expression
    private void argumentSequence() {
        matchRule("ArgumentSequence");

        expression();

        while (currentToken != null && matchText(",")) {
            expression();
        }
    }

    // 23. TrueForStmt -> TrueFor ( ConditionExpression ) Block
    //                   | TrueFor ( ConditionExpression ) Block TrueForElse Block
    private void trueForStmt() {
        matchRule("TrueForStmt");

        if (matchText("TrueFor")) {
            if (matchText("(")) {
                conditionExpression();

                if (matchText(")")) {
                    block();

                    if (currentToken != null && currentToken.getText().equals("Else")) {
                        trueForElse();
                        block();
                    }
                } else {
                    error("Expected ) after condition in TrueFor statement");
                    synchronize("{");
                }
            } else {
                error("Expected ( after TrueFor");
                synchronize("(", "{");
            }
        } else {
            error("Expected TrueFor");
            synchronize("{", "}");
        }
    }

    // 24. TrueForElse -> Else
    private void trueForElse() {
        matchRule("TrueForElse");

        if (matchText("Else")) {
            // Successfully parsed Else
        } else {
            error("Expected Else");
            synchronize("{");
        }
    }

    // 25. HoweverStmt -> However ( ConditionExpression ) Block
    private void howeverStmt() {
        matchRule("HoweverStmt");

        if (matchText("However")) {
            if (matchText("(")) {
                conditionExpression();

                if (matchText(")")) {
                    block();
                } else {
                    error("Expected ) after condition in However statement");
                    synchronize("{");
                }
            } else {
                error("Expected ( after However");
                synchronize("(", "{");
            }
        } else {
            error("Expected However");
            synchronize("{", "}");
        }
    }

    // 26. WhenStmt -> When ( Expression ; Expression ; Expression ) Block
    private void whenStmt() {
        matchRule("WhenStmt");

        if (matchText("When")) {
            if (matchText("(")) {
                if (isExpressionStart()) {
                    expression();
                } else {
                    error("Expected expression in When statement");
                    synchronize(";", ")");
                    return;
                }


                if (matchText(";")) {
                    if (isExpressionStart()) {
                        expression();
                    } else {
                        error("Expected expression in When statement");
                        synchronize(";", ")");
                        return;
                    }


                    if (matchText(";")) {
                        if (isExpressionStart()) {
                            expression();
                        } else {
                            error("Expected expression in When statement");
                            synchronize(";", ")");
                            return;
                        }


                        if (matchText(")")) {
                            block();
                        } else {
                            error("Expected ) at end of When statement");
                            synchronize("{");
                        }
                    } else {
                        error("Expected ; in When statement");
                        synchronize(";", ")");
                    }
                } else {
                    error("Expected ; in When statement");
                    synchronize(";", ")");
                }
            } else {
                error("Expected ( after When");
                synchronize("(", "{");
            }
        } else {
            error("Expected When");
            synchronize("{", "}");
        }
    }

    // 27. RespondwithStmt -> Respondwith Expression ; | Respondwith ID ;
    private void respondwithStmt() {
        matchRule("RespondwithStmt");

        if (matchText("Respondwith")) {
            if (currentToken != null && currentToken.getType().equals("Identifier")) {
                consume(); // Identifier
            } else if (isExpressionStart()) {
                expression();
            } else {
                error("Expected identifier or expression after Respondwith");
                synchronize(";");
            }

            if (matchText(";")) {
                // Successfully parsed Respondwith statement
            } else {
                error("Expected ; after Respondwith statement");
                synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
            }
        } else {
            error("Expected Respondwith");
            synchronize(";");
        }
    }

    // 28. EndthisStmt -> Endthis ;
    private void endthisStmt() {
        matchRule("EndthisStmt");

        if (matchText("Endthis")) {
            if (matchText(";")) {
                // Successfully parsed Endthis statement
            } else {
                error("Expected ; after Endthis");
                synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
            }
        } else {
            error("Expected Endthis");
            synchronize(";");
        }
    }

    // 29. ScanStmt -> Scan(Conditionof ID) ;
    private void scanStmt() {
        matchRule("ScanStmt");

        if (matchText("Scan")) {
            if (matchText("(")) {
                if (matchText("Conditionof")) {
                    if (currentToken != null && currentToken.getType().equals("Identifier")) {
                        consume(); // ID

                        if (matchText(")")) {
                            if (matchText(";")) {
                                // Successfully parsed Scan statement
                            } else {
                                error("Expected ; after Scan statement");
                                synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
                            }
                        } else {
                            error("Expected ) in Scan statement");
                            synchronize(";");
                        }
                    } else {
                        error("Expected identifier after Conditionof");
                        synchronize(")", ";");
                    }
                } else {
                    error("Expected Conditionof in Scan statement");
                    synchronize(")", ";");
                }
            } else {
                error("Expected ( after Scan");
                synchronize("(", ";");
            }
        } else {
            error("Expected Scan");
            synchronize(";");
        }
    }

    // 30. SrapStmt -> Srap ( Expression ) ;
    private void srapStmt() {
        matchRule("SrapStmt");

        if (matchText("Srap")) {
            if (matchText("(")) {
                expression();

                if (matchText(")")) {
                    if (matchText(";")) {
                        // Successfully parsed Srap statement
                    } else {
                        error("Expected ; after Srap statement");
                        synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
                    }
                } else {
                    error("Expected ) in Srap statement");
                    synchronize(";");
                }
            } else {
                error("Expected ( after Srap");
                synchronize("(", ";");
            }
        } else {
            error("Expected Srap");
            synchronize(";");
        }
    }

    // 31. Block -> { Statements }
    private void block() {
        matchRule("Block");

        if (matchText("{")) {
            statements();

            if (matchText("}")) {
                // Successfully parsed block
            } else {
                error("Expected } at end of block");
                // Try to recover - look for the next block boundary
                synchronize("Identifier", "TrueFor", "However", "When", "Respondwith", "Endthis", "Scan", "Srap", "}");
            }
        } else {
            error("Expected { at beginning of block");
            synchronize("{", "}");
        }
    }

    // 32. ConditionExpression -> Condition | Condition LogicalOp Condition
    private void conditionExpression() {
        matchRule("ConditionExpression");

        condition();

        if (currentToken != null && isLogicalOp()) {
            logicalOp();
            condition();
        }
    }

    private boolean isLogicalOp() {
        if (currentToken == null) return false;

        String text = currentToken.getText();
        return text.equals("&&") || text.equals("||") || text.equals("~");
    }

    // 33. LogicalOp -> && | || | ~
    private void logicalOp() {
        matchRule("LogicalOp");

        if (matchText("&&") || matchText("||") || matchText("~")) {
            // Successfully parsed logical operator
        } else {
            error("Expected logical operator (&&, ||, ~)");
            // No synchronization needed here since this is part of a larger expression
        }
    }

    // 34. Condition -> Expression ComparisonOp Expression
    private void condition() {
        matchRule("Condition");

        expression();

        if (currentToken != null) {
            comparisonOp();
            expression();
        } else {
            error("Unexpected end of input in condition");
        }
    }

    // 35. ComparisonOp -> == | != | > | >= | < | <=
    private void comparisonOp() {
        matchRule("ComparisonOp");

        if (matchText("==") || matchText("!=") || matchText(">") ||
                matchText(">=") || matchText("<") || matchText("<=")) {
            // Successfully parsed comparison operator
        } else {
            error("Expected comparison operator (==, !=, >, >=, <, <=)");
            // No synchronization needed here since this is part of a larger expression
        }
    }

    // 36. Expression -> Term | Expression AddOp Term
    private void expression() {
        matchRule("Expression");

        term();

        while (currentToken != null && isAddOp()) {
            addOp();
            term();
        }
    }

    private boolean isAddOp() {
        if (currentToken == null) return false;

        String text = currentToken.getText();
        return text.equals("+") || text.equals("-");
    }

    // 37. AddOp -> + | -
    private void addOp() {
        matchRule("AddOp");

        if (matchText("+") || matchText("-")) {
            // Successfully parsed add operator
        } else {
            error("Expected add operator (+ or -)");
            // No synchronization needed here since this is part of a larger expression
        }
    }

    // 38. Term -> Factor | Term MulOp Factor
    private void term() {
        matchRule("Term");

        factor();

        while (currentToken != null && isMulOp()) {
            mulOp();
            factor();
        }
    }

    private boolean isMulOp() {
        if (currentToken == null) return false;

        String text = currentToken.getText();
        return text.equals("*") || text.equals("/");
    }

    // 39. MulOp -> * | /
    private void mulOp() {
        matchRule("MulOp");

        if (matchText("*") || matchText("/")) {
            // Successfully parsed multiply operator
        } else {
            error("Expected multiply operator (* or /)");
            // No synchronization needed here since this is part of a larger expression
        }
    }

    // 40. Factor -> ID | Number | ( Expression )
//    private void factor() {
//        matchRule("Factor");
//
//        if (currentToken.getType().equals("Identifier")) {
//            consume();
//        } else if (currentToken.getType().equals("Constant")) {
//            consume();
//        } else if (matchText("(")) {
//            expression();
//
//            if (matchText(")")) {
//                // Successfully parsed parenthesized expression
//            } else {
//                error("Expected ) at end of expression");
//            }
//        } else {
//            error("Expected identifier, number, or ( in factor");
//        }
//    }
    // 40. Factor -> ID | Number | ( Expression ) | String Literal
    private void factor() {
        matchRule("Factor");

        if (currentToken.getType().equals("Identifier")) {
            consume();
        } else if (currentToken.getType().equals("Constant")) {
            consume();
        } else if (currentToken.getType().equals("String Literal")) {
            // Add support for string literals
            consume();
        } else if (matchText("(")) {
            expression();

            if (matchText(")")) {
                // Successfully parsed parenthesized expression
            } else {
                error("Expected ) at end of expression");
                synchronize("+", "-", "*", "/", ")", ";");
            }
        } else {
            error("Expected identifier, number, string literal, or ( in factor");
            synchronize("+", "-", "*", "/", ")", ";");
        }
    }
    // 41. Comment -> /< STR >/ | /* STR
    private void comment() {
        matchRule("Comment");

        if (currentToken.getType().equals("Comment")) {
            consume();
        } else {
            error("Expected comment");
            synchronize(";", "{", "}");
        }
    }

    // 42. RequireCommand -> Require ( F_name.txt ) ;
    private void requireCommand() {
        matchRule("RequireCommand");

        if (matchText("Require")) {
            if (matchText("(")) {
                fName();

                if (matchText(")")) {
                    if (matchText(";")) {
                        // Successfully parsed require command
                    } else {
                        error("Expected ; after require command");
                        synchronize(";", "{", "}");
                    }
                } else {
                    error("Expected ) in require command");
                    synchronize(";");
                }
            } else {
                error("Expected ( after Require");
                synchronize("(", ";");
            }
        } else {
            error("Expected Require");
            synchronize(";");
        }
    }

    // 43. F_name -> STR
    private void fName() {
        matchRule("F_name");

        if (currentToken.getType().equals("String Literal") ||
                currentToken.getType().equals("Identifier")) {
            consume();
        } else {
            error("Expected file name");
        }
    }

    public String getParserOutput() {
        StringBuilder output = new StringBuilder();

        for (String rule : matchedRules) {
            output.append(rule).append("\n");
        }

        for (String error : errors) {
            output.append(error).append("\n");
        }

        output.append("Total NO of errors: ").append(errorCount);
        return output.toString();
    }
}

