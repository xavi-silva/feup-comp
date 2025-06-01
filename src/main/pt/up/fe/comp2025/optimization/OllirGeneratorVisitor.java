package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;
    private String currentMethod;
    private final TypeUtils types;
    private final OptUtils ollirTypes;


    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(VAR_DECL, this::visitField);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("SimpleStmt", this::visitSimpleStmt);
        addVisit("IfStmt", this::visitIfStmt);
        addVisit("BlockStmt", this::visitBlockStmt);
        addVisit("ArrayStmt", this::visitArrayStmt);
        addVisit("LoopStmt", this::visitWhileStmt);
        addVisit("Import", this::visitImport);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var rhs = exprVisitor.visit(node.getChild(0));

        StringBuilder code = new StringBuilder();

        var left = node.get("name");
        Type thisType;
        if (types.isField(left, table, currentMethod)){
            thisType = types.getFieldType(left, table);
        }
        else{
            thisType = types.getVarType(table, left, currentMethod);
        }
        String typeString = ollirTypes.toOllirType(thisType);
        String leftOllirType = ollirTypes.toOllirType(types.getExprType(node.getChild(0), table, currentMethod));

        String rhsComp = rhs.getComputation();
        String rhsCode = rhs.getCode();
        rhsComp = rhsComp.replace(".assume", typeString);
        rhsCode = rhsCode.replace(".assume", typeString);

        // code to compute the children
        code.append(rhsComp);

        // code to compute self
        // statement has type of lhs

        if(types.isField(left, table, currentMethod)) {
            code.append("putfield(this, ").append(left).append(leftOllirType).append(',').append(SPACE).append(rhsCode).append(").V").append(END_STMT);
        }
        else{
            var varCode = left + typeString;
            code.append(varCode);
            code.append(SPACE);

            code.append(ASSIGN);
            code.append(typeString);
            code.append(SPACE);

            code.append(rhsCode);

            code.append(END_STMT);
        }

        return code.toString();
    }

    private String visitArrayStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getChild(0));
        var rhs = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();
        code.append(lhs.getComputation()).append(rhs.getComputation());

        var varName = node.get("name");

        if (types.isField(varName, table, currentMethod)) {
            String tempArray = exprVisitor.ollirTypes.nextTemp("arr");
            String arrayType = ".array.i32";

            code.append(tempArray).append(arrayType)
                    .append(SPACE).append(ASSIGN).append(arrayType).append(SPACE)
                    .append("getfield(this, ").append(varName).append(arrayType).append(")").append(arrayType).append(END_STMT);

            code.append(tempArray).append("[").append(lhs.getCode()).append("]").append(".i32")
                    .append(SPACE).append(ASSIGN).append(".i32").append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        } else {
            code.append(varName).append("[").append(lhs.getCode()).append("]").append(".i32")
                    .append(SPACE).append(ASSIGN).append(".i32").append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        }

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        Type retType  = table.getReturnType(currentMethod);

        StringBuilder code = new StringBuilder();

        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;


        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        JmmNode type = node.getChild(0);
        String typeCode;
        if (type.getKind().equals("VarArgs")){
            typeCode = ".array.i32";
        }
        else{
            typeCode = ollirTypes.toOllirType(node.getChild(0));
        }
        var id = node.get("name");

        return id + typeCode;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = node.getBoolean("isPublic", false);

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = node.getBoolean("isStatic", false);

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);
        currentMethod = name;
        exprVisitor.setCurrentMethod(name);

        // params
        // TODO: Hardcoded for a single parameter, needs to be expanded(ja ta )

        String methodName = node.get("name");

        if (methodName.equals("main")) {

            String paramName = node.get("paramName");
            code.append("(").append(paramName).append(".array.String)");

        } else {
            var paramsCode = node.getChildren(PARAM).stream()
                    .map(this::visit)
                    .collect(Collectors.joining(", "));

            code.append("(").append(paramsCode).append(")");
        }


        // TODO: Hardcoded for int, needs to be expanded
        int childrenSize = node.getChildren().size();

        String retType;
        if (!node.hasAttribute("void_")){
            var retTypeNode = node.getChild(0);
            //Type type = types.getExprType(node, table, currentMethod);
            retType = ollirTypes.toOllirType(retTypeNode);
            code.append(retType);
            code.append(L_BRACKET);
        }
        else{
            retType =".V";
            code.append(retType);
            code.append(L_BRACKET);
        }

        // rest of its children stmts
        var stmtsCode = node.getChildren().stream()
                .filter(child -> child.getKind().endsWith("Statement") || child.getKind().endsWith("Stmt"))
                .map(this::visit)
                .collect(Collectors.joining("\n   ", "   ", ""));

        code.append(stmtsCode);

        if (retType.equals(".V")) {
            code.append("   ret.V").append(END_STMT);  // Add return statement for void methods
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());


        String superClass = table.getSuper();
        if (superClass != null && !superClass.isEmpty()) {
            code.append(" extends ").append(superClass);
        }
        
        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

        for (var child : node.getChildren(VAR_DECL)) {
            var result = visit(child);
            code.append(result);
        }
        code.append(NL);

        code.append(buildConstructor());
        code.append(NL);


        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        for (JmmNode child : node.getChildren()) {
            code.append(visit(child));
        }


        return code.toString();
    }
    private String visitSimpleStmt(JmmNode node, Void unused) {
        var childNode = node.getChild(0);

        if (childNode.isInstance("MethodCallExpr")) {
            String op = childNode.get("name");
            List<String> codes = new ArrayList<>();
            StringBuilder computation = new StringBuilder();

            // Process the caller object first
            JmmNode callerNode = childNode.getChild(0);
            OllirExprResult callerResult = exprVisitor.visit(callerNode);
            computation.append(callerResult.getComputation());
            String callerCode = callerResult.getCode();

            // Process all the arguments
            for (int i = 1; i < childNode.getChildren().size(); i++) {
                OllirExprResult argResult = exprVisitor.visit(childNode.getChild(i));
                computation.append(argResult.getComputation());
                codes.add(argResult.getCode());
            }

            String object;
            // This method
            if (callerNode.hasAttribute("value") && callerNode.get("value").equals("this")){
                object = "this." + table.getClassName();
                return computation + exprVisitor.invokeVirtual(object, op, codes) + ".V" + END_STMT;
            }

            // Static method
            else if (table.getImports().contains(callerNode.get("name"))) {
                object = callerNode.get("name");
                return computation.toString() + exprVisitor.invokeStatic(object, op, codes) + ".V" + END_STMT;
            }

            // Virtual method
            else{
                object = callerNode.get("name") + "." + TypeUtils.getVarType(table, callerNode.get("name"), currentMethod).getName();
                return computation + exprVisitor.invokeVirtual(object, op, codes) + ".V" + END_STMT;
            }
        }

        // Handle non-method call expressions
        OllirExprResult child = exprVisitor.visit(childNode);
        return child.getComputation() + child.getCode() + END_STMT;
    }
    private String visitWhileStmt(JmmNode node, Void unused) {
        StringBuilder result = new StringBuilder();
        OllirExprResult childExpr = exprVisitor.visit(node.getChild(0));
        String stmt = visit(node.getChild(1));
        String temp = exprVisitor.ollirTypes.nextTemp("");
        String endif = exprVisitor.ollirTypes.nextTemp("endif");

        result.append("while").append(temp).append(":").append(NL);
        result.append(childExpr.getComputation());
        result.append("if (!.bool").append(SPACE).append(childExpr.getCode()).append(")").append(SPACE).append("goto").append(SPACE).append(endif).append(END_STMT);
        result.append(stmt).append(NL);
        result.append("goto while").append(temp).append(END_STMT);
        result.append(endif).append(":").append(NL);
        return result.toString();


    }

    private String visitIfStmt(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        OllirExprResult exprResult = exprVisitor.visit(node.getChild(0));
        String thenCode = visit(node.getChild(1));
        String elseCode = visit(node.getChild(2));
        String then = exprVisitor.ollirTypes.nextTemp("then");
        String endif = exprVisitor.ollirTypes.nextTemp("endif");

        code.append(exprResult.getComputation());
        code.append("if(").append(exprResult.getCode()).append(") goto ").append(then).append(END_STMT);

        code.append(elseCode);
        code.append("goto ").append(endif).append(END_STMT);
        code.append(then).append(":\n");
        code.append(thenCode);
        code.append(endif).append(":\n");


        return code.toString();
    }

    private String visitBlockStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        for (JmmNode stmt : node.getChildren()) {
            var child = visit(stmt);
            code.append(child);
        }
        return code.toString();
    }
    private String visitImport(JmmNode node, Void unused) {
        StringBuilder result = new StringBuilder();
        result.append("import ");
        List<String> imports = node.getObjectAsList("name", String.class);
        if(imports.size() == 1){
            result.append(imports.get(0));
        }
        else {
            for (int i = 0; i < imports.size(); i++) {
                result.append(imports.get(i));
                if (i < imports.size() - 1) {
                    result.append(".");
                }
            }
        }
        result.append(END_STMT);
        return result.toString();
    }
    private String visitField(JmmNode node, Void unused) {
        if (node.getParent().getKind().equals(CLASS_DECL.toString())) { // penso que é desnecessario mas é para garantir
            StringBuilder result = new StringBuilder();
            result.append("field public ");
            String name = node.get("name");
            String type = ollirTypes.toOllirType(node.getChild(0));
            return ".field public " + name + type + END_STMT;
        }
        return "";
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
