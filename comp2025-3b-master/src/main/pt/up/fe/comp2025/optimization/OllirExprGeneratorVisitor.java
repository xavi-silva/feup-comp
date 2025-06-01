package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private static final String INT_TYPE = ".i32";
    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private String currentMethod;

    private final TypeUtils types;
    public final OptUtils ollirTypes;


    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
    }


    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit("ParenExpr", this::visitParenExpr);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("NewExpr", this::visitNewObjectExpr);
        addVisit("NewArrayExpr", this::visitNewArrayExpr);
        addVisit("ArrayAcessExpr", this::visitArrayAcessExpr);
        addVisit("ArrayExpr", this::visitArrayExpr);
        addVisit("UnaryExpr", this::visitUnaryExpr);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit("LengthExpr", this::visitLengthExpr);
        addVisit("ThisExpr", this::visitThisExpr);


//        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = TypeUtils.newIntType();
        String ollirIntType = ollirTypes.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();
        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));
        StringBuilder code = new StringBuilder();

        if (node.get("op").equals("&&")) {
            var then = ollirTypes.nextTemp("then");
            var end = ollirTypes.nextTemp("endif");
            var andTmp = ollirTypes.nextTemp("andTmp");

            computation.append(lhs.getComputation());

            computation.append("if (").append(lhs.getCode()).append(") goto ").append(then).append(";\n");
            computation.append(andTmp).append(".bool :=.bool 0.bool;\n");
            computation.append("goto ").append(end).append(";\n");

            computation.append(then).append(":\n");

            computation.append(rhs.getComputation());
            computation.append(andTmp).append(".bool").append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append(rhs.getCode()).append(";\n");

            computation.append(end).append(":\n");
            code.append(andTmp).append(".bool");
        }



        else{
            Type resType = types.getExprType(node, table, currentMethod);
            String resOllirType = ollirTypes.toOllirType(resType);
            if (((node.getChild(0).isInstance(VAR_REF_EXPR) && node.getChild(1).isInstance(INTEGER_LITERAL)) ||
                    (node.getChild(0).isInstance(INTEGER_LITERAL) && node.getChild(1).isInstance(VAR_REF_EXPR))) &&
                            node.getParent().isInstance(ASSIGN_STMT)) {
                code.append(lhs.getCode())
                        .append(SPACE)
                        .append(node.get("op"))
                        .append(resOllirType)
                        .append(SPACE)
                        .append(rhs.getCode());
            }
            else{
                // code to compute the children
                computation.append(lhs.getComputation());
                computation.append(rhs.getComputation());

                // code to compute self
                code.append(ollirTypes.nextTemp()).append(resOllirType);

                computation.append(code).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE)
                        .append(lhs.getCode()).append(SPACE);

                Type type = types.getExprType(node, table, currentMethod);
                computation.append(node.get("op")).append(ollirTypes.toOllirType(type)).append(SPACE)
                        .append(rhs.getCode()).append(END_STMT);
            }
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        var id = node.get("name");

        if(types.isField(id, table, currentMethod)) {
            StringBuilder code = new StringBuilder();
            StringBuilder computation = new StringBuilder();

            Type fieldType = types.getFieldType(id, table);
            var ollirType = ollirTypes.toOllirType(fieldType);

            code.append(ollirTypes.nextTemp()).append(ollirType);

            computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("getfield(this, ").append(id).append(ollirType);
            computation.append(")").append(ollirType).append(END_STMT);
            return new OllirExprResult(code.toString(), computation.toString());

        }

        Type type = types.getExprType(node, table, currentMethod);
        if (type == null) {
            type = new Type("unknown", false);
        }

        String ollirType = ollirTypes.toOllirType(type);
        String code = id + ollirType;

        return new OllirExprResult(code);
    }
    private OllirExprResult visitParenExpr(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }
    private OllirExprResult visitIdentifier(JmmNode node, Void unused) {
        Type booleanType = TypeUtils.newBooleanType();
        String ollirBooleanType = ollirTypes.toOllirType(booleanType);
        String value = node.get("value");
        if(value.equals("true")) {
            return new OllirExprResult(1 + ollirBooleanType);
        }
        return new OllirExprResult(0 + ollirBooleanType);
    }
    private OllirExprResult visitNewObjectExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String temp = ollirTypes.nextTemp();
        Type type = types.getExprType(node, table, currentMethod);
        String ollirType = ollirTypes.toOllirType(type);

        String code = temp + ollirType;
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("new(").append(type.getName()).append(")").append(ollirType).append(END_STMT);
        computation.append("invokespecial(").append(temp).append(ollirType).append(", \"<init>\").V").append(END_STMT);

        return new OllirExprResult(code, computation.toString());

    }


    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();
        String temp = ollirTypes.nextTemp();

        Type type = types.getExprType(node, table, currentMethod);
        String ollirType = ollirTypes.toOllirType(type);

        String code = temp + ollirType;

        JmmNode sizeExpr = node.getChild(0);
        OllirExprResult sizeResult = visit(sizeExpr);

        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("new(array, ").append(sizeResult.getCode()).append(")").append(ollirType).append(END_STMT);

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitArrayAcessExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();
        String temp = ollirTypes.nextTemp();

        Type type = types.getExprType(node, table, currentMethod);
        String ollirType = ollirTypes.toOllirType(type);

        String code = temp + ollirType;

        OllirExprResult arrayResult = visit(node.getChild(0));
        OllirExprResult indexResult = visit(node.getChild(1));

        computation.append(indexResult.getComputation());
        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append(arrayResult.getCode()).append("[").append(indexResult.getCode()).append("]").append(ollirType).append(END_STMT);


        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitArrayExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();
        String temp = ollirTypes.nextTemp();

        Type type = types.getExprType(node, table, currentMethod);
        String ollirType = ollirTypes.toOllirType(type);

        String code = temp + ollirType;

        int arraySize = node.getNumChildren();

        computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE).append("new(array, ").append(arraySize).append(INT_TYPE).append(")").append(ollirType).append(END_STMT);

        for(int i =0; i<arraySize; i++){
            JmmNode elem = node.getChild(i);
            OllirExprResult result = visit(elem);

            computation.append(temp).append("[").append(i).append(INT_TYPE).append("]").append(INT_TYPE).append(SPACE).append(ASSIGN).append(INT_TYPE).append(SPACE).append(result.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code, computation.toString());
    }

    private OllirExprResult visitUnaryExpr(JmmNode node, Void unused) {
        var expr = visit(node.getChild(0));

        StringBuilder computation = new StringBuilder();
        computation.append(expr.getComputation());

        Type resType = types.getExprType(node, table, currentMethod);
        String resOllirType = ollirTypes.toOllirType(resType);
        String code = ollirTypes.nextTemp() + resOllirType;

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE)
                .append(node.get("op")).append(resOllirType).append(SPACE).append(expr.getCode())
                .append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitMethodCallExpr(JmmNode node, Void unused) {
        String first;
        if (node.getChild(0).hasAttribute("name")){
            first = node.getChild(0).get("name");
        }
        else{
            first = node.getChild(0).get("value");
        }

        String second = node.get("name");

        var object = visit(node.getChild(0));
        var objectCode = object.getCode();

        StringBuilder computation = new StringBuilder();
        computation.append(object.getComputation());

        Type returnType = types.getExprType(node, table, currentMethod);
        var ollirObjectType = ollirTypes.toOllirType(returnType);

        List<String> codes = new ArrayList<>();
        var params = table.getParameters(node.get("name"));

        int n_varargs;
        if (params != null && params.getLast().getType().getName().equals("int...")) {
            n_varargs = node.getNumChildren() - params.size();
        }
        else {
            n_varargs = 0;
        }

        for (int i = 1; i < node.getChildren().size() - n_varargs; i++) {
            OllirExprResult argResult = visit(node.getChild(i));
            computation.append(argResult.getComputation());
            codes.add(argResult.getCode());
        }

        if (n_varargs > 0){
            var array = ollirTypes.nextTemp();
            computation.append(array).append(".array.i32 :=.array.i32 new(array, ").append(n_varargs)
                    .append(".i32).array.i32").append(END_STMT);
            for (int i = 0; i < n_varargs; i++){
                computation.append(array).append("[").append(i).append(".i32].i32 :=.i32 ")
                        .append(node.getChild(params.size() + i).get("value")).append(".i32").append(END_STMT);
            }
            codes.add(array + ".array.i32");
        }

        /*
        for (int i = 1; i < node.getChildren().size(); i++) {
            OllirExprResult argResult = visit(node.getChild(i));
            computation.append(argResult.getComputation());
            codes.add(argResult.getCode());
        }*/

        boolean isStatic = table.getImports().contains(first);

        String methodCallCode;
        if (isStatic) {
            methodCallCode = invokeStatic(first, second, codes);
        } else {
            methodCallCode = invokeVirtual(objectCode, second, codes);
        }

        // Se o método retorna void
        if (returnType.getName().equals("void")) {
            computation.append(methodCallCode).append(ollirObjectType).append(END_STMT);
            return new OllirExprResult("", computation.toString());
        } else {
            String temp = ollirTypes.nextTemp();
            computation.append(temp).append(ollirObjectType)
                    .append(SPACE).append(ASSIGN).append(ollirObjectType).append(SPACE)
                    .append(methodCallCode).append(ollirObjectType).append(END_STMT);
            return new OllirExprResult(temp + ollirObjectType, computation.toString());
        }
        //String code = ollirTypes.nextTemp() + ollirObjectType;

        // Static
        /*if (table.getImports().contains(varName)) {
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirObjectType).append(SPACE)
                    .append(invokeStatic(varName, op, codes)).append(ollirObjectType).append(END_STMT);
        }

        // Virtual
        else{
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirObjectType).append(SPACE)
                    .append(invokeVirtual(object.getCode(), op, codes)).append(ollirObjectType).append(END_STMT);
        }*/


        //return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitLengthExpr(JmmNode node, Void unused) {
        String temp = ollirTypes.nextTemp();
        String code = temp + ".i32";
        StringBuilder computation = new StringBuilder();
        String name = node.getChild(0).get("name");
        computation.append(code).append(SPACE).append(ASSIGN).append(".i32").append(SPACE)
                .append("arraylength(").append(name).append(".array.i32).i32").append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitThisExpr(JmmNode node, Void unused) {
        String code = "this." + table.getClassName();
        return new OllirExprResult(code);
    }

    private String invokeSpecial(String code) {
        return """ 
                    invokespecial(%s, "<init>").V;
                }
                """.formatted(code);
    }

    public String invokeVirtual(String code, String op, List<String> params) {
        boolean isParamsEmpty = params.isEmpty();
        String paramPlaceholders = String.join(", ", params.stream().map(p -> "%s").toList());
        String template = """
            invokevirtual(%s, "%s", %s)
            """.formatted(code, op, paramPlaceholders);
        if (isParamsEmpty) {
            template = """
            invokevirtual(%s, "%s")
            """.formatted(code, op);
        }
        return template.formatted(params.toArray());
    }

    public String invokeStatic(String code, String op, List<String> params) {
        boolean isParamsEmpty = params.isEmpty();

        String paramPlaceholders = String.join(", ", params.stream().map(p -> "%s").toList());

        String template = """
        invokestatic(%s, "%s", %s)
        """;

        if (isParamsEmpty) {
            template = """
            invokestatic("%s", "%s")
            """;
        }

        return template.formatted(code, op, paramPlaceholders).formatted(params.toArray());
    }




    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

    public void setCurrentMethod(String methodName) {
        this.currentMethod = methodName;
    }

}
