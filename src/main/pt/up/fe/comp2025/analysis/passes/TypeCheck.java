package pt.up.fe.comp2025.analysis.passes;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class TypeCheck extends AnalysisVisitor {

    private String currentMethod;
    @Override
    public void buildVisitor(){
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("LoopStmt", this::visitWhileIfStmt);
        addVisit("IfStmt", this::visitWhileIfStmt);
        addVisit("BinaryExpr", this::visitBinaryOp);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ArrayAcessExpr", this::visitArrayAccess);
        addVisit("ReturnStatement", this::visitReturnStmt);
        addVisit("ThisExpr", this::visitThisExpr);
        addVisit("LengthExpr", this::visitLengthExpr);
        //addVisit("VarArgs", this::visitVarArgs);
   }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        //System.out.println("ESTA A ENTRAR AQUI????");

        // Varargs must be last param
        var params = table.getParameters(currentMethod);
        int i = 0;
        for (var param : params) {
            if (param.getType().getName().equals("int...")){
                //System.out.println("MUSOU PARA ARRAY??" + param);
                param = new Symbol(new Type("int", true), param.getName());
                if (i < params.size() - 1){ // Not last element
                    var message = "VarArgs must be last param.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            method.getLine(),
                            method.getColumn(),
                            message,
                            null)
                    );
                }
                //System.out.println("MUSOU PARA ARRAY??" + param);
            }
            i++;
        }
        Set<String> seenParamNames = new HashSet<>(); // Set para rastrear parâmetros já vistos
        i = 0;

        for (var param : params) {
            String paramName = param.getName(); // Obtém o nome do parâmetro

            // Verifica se o parâmetro já foi visto
            if (seenParamNames.contains(paramName)) {
                String message = "Parâmetro duplicado encontrado: " + paramName;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        message,
                        null)
                );
                //System.out.println("Erro: " + message);
            } else {
                seenParamNames.add(paramName); // Adiciona o nome do parâmetro ao Set
            }

            // Verificação de varargs, se necessário
            if (param.getType().getName().equals("int...")) {
                //System.out.println("Varargs detectado: " + param);
                // (Aqui você pode adicionar a transformação do tipo, conforme necessário)
            }

            i++;
        }
        //debugMethod(method);
        return null;
    }


    private Void visitBinaryOp(JmmNode node, SymbolTable table) {
        //System.out.println("CCCCCCCCCCCCCCCCC");
        String operator = node.get("op");
            if("unknown".equals(TypeUtils.getExprType(node, table, currentMethod).getName())){
                var message = String.format("Expression variables are not compatible with operator '%s'", operator);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        node.getLine(),
                        node.getColumn(),
                        message,
                        null)
                );
                return null;
        }



        return null;
    }

    private Void visitAssignStmt(JmmNode node, SymbolTable table) {
        //System.out.println("amarelo");
        //System.out.println(node);
        String var = node.get("name");
        Type varType = TypeUtils.getVarType(table, var, currentMethod);
        //System.out.println(varType);
        Type exprType;

        //System.out.println(node.getChild(0));
        exprType = TypeUtils.getExprType(node.getChild(0), table, currentMethod);
        //System.out.println(exprType);

        if (varType != null && table.getSuper() != null && table.getImports().contains(varType.getName())) {
            return null;
        }

        var imports = table.getImports();
        Type methodCallObjectType = null;
        if (node.getChild(0).getKind().equals("MethodCallExpr")){
            methodCallObjectType = TypeUtils.getExprType(node.getChild(0).getChild(0), table, currentMethod);
        }

        if (varType != null) {
            if (varType.isArray() != exprType.isArray()) {
                var message = "Assignment types do not match: cannot assign '" + exprType + "' to '" + varType + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        node.getLine(),
                        node.getColumn(),
                        message,
                        null
                ));
            }
            else if (!varType.equals(exprType) && !TypeUtils.isSubtype(varType.getName(), exprType.getName(), table) && !TypeUtils.isSubtype(exprType.getName(), varType.getName(), table)) {
                if (methodCallObjectType != null) {
                    if (methodCallObjectType.hasAttribute("name") && !imports.contains(methodCallObjectType.getName())) {
                        var message = "Assignment types do not match.\n";
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                node.getLine(),
                                node.getColumn(),
                                message,
                                null)
                        );
                    }
                }
                else{
                    var message = "Assignment types do not match.\n";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            node.getLine(),
                            node.getColumn(),
                            message,
                            null)
                    );
                }
            }
        }

    return null;
    }
    private Void visitWhileIfStmt(JmmNode node, SymbolTable table) {
        Type aux = new Type("boolean",false);
        var exprNode = node.getChild(0);
        Type exprType = TypeUtils.getExprType(exprNode, table, currentMethod);
        //System.out.println("ExprTypeeeeeeeeee====================: " + exprType);
        if(!aux.equals(exprType)){
            var message = "No valid condition inside the statement.\n";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }


        return null;
    }
    private Void visitArrayAccess(JmmNode node, SymbolTable table) {
        JmmNode arrayExpr = node.getChild(0);
        //System.out.println("ola: "+arrayExpr);
        Type arrayType = TypeUtils.getExprType(arrayExpr, table, currentMethod);

        //System.out.println("BBBBBBBBBBBBBBBBBBBBBBBBB: " + arrayType);
        if (!arrayType.isArray()) {
            var message = "Attempted to access index of a non-array expression.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
            return null;
        }



        Type aux = new Type("int",false);
        //System.out.println("ai meu deus"+ aux.toString());

        var exprAccess = node.getChild(1);
        Type exprType = TypeUtils.getExprType(exprAccess, table, currentMethod);
        //System.out.println("Tomasssssssssssss: " + exprType);
        if(!aux.equals(exprType)){
            var message = "Invalid expression type for array access.\n";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        //System.out.println("visitReturnStmt chamado para: " + returnStmt);
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");
        var returnExpr = returnStmt.getChildren().get(0);
        visit(returnExpr, table);
        var expectedReturnType = table.getReturnType(currentMethod);
        //System.out.println("expectedReturnType: " + expectedReturnType);
        //System.out.println("ola");
        //System.out.println(returnExpr);
        var actualReturnType = TypeUtils.getExprType(returnExpr, table, currentMethod);
        //System.out.println("actualRetType: " + actualReturnType);
        //System.out.println("olaola");
        //System.out.println(actualReturnType.getName());
        if (!expectedReturnType.equals(actualReturnType) && !actualReturnType.getName().equals("assume")) {
            //System.out.println(actualReturnType.getName() + expectedReturnType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    returnStmt.getLine(),
                    returnStmt.getColumn(),
                    "Incompatible return type: expected '" + expectedReturnType + "', but got '" + actualReturnType + "'.",
                    null)
            );
        }

        return null;
    }

    private Void visitThisExpr(JmmNode node, SymbolTable table) {
        //System.out.println("PESCA52");
        var methodNode = node.getAncestor("MethodDecl");

        if (methodNode.isEmpty()) {
            return null;
        }

        JmmNode method = methodNode.get();
        String methodName = method.get("name");

        if ("main".equals(methodName)) {
            var message = "`this` expression cannot be used in a static method.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null
            ));
        }

        String className = table.getClassName();
        node.put("type", className);
        node.put("isArray", "false");

        return null;
    }

    private Void visitLengthExpr(JmmNode node, SymbolTable table) {
        if(!node.get("name").equals("length")){
            var message = "Invalid length expression.\n";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    node.getLine(),
                    node.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }




}
