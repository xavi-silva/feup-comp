package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

import javax.swing.plaf.synth.SynthTextAreaUI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods regarding types.
 */
public class TypeUtils {


    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }
    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    public static Type convertType(JmmNode typeNode) {

        // TODO: When you support new types, this must be updated

        //System.out.println("olaaaaa");
        // Arrays
        if (typeNode.getKind().equals("Array") || typeNode.getKind().equals("NewArrayExpr")) {
            JmmNode baseTypeNode = typeNode.getChild(0);
            String baseType = baseTypeNode.get("dataType");
            //System.out.println("yeyyyyyyyyy" + baseType);

            return new Type(baseType, true);
        }

        // Varargs
        else if(typeNode.get("dataType").equals("int...") || typeNode.get("dataType").equals("int ...")) {
            return new Type("int...", true);
        }

        else{
            var name = typeNode.get("dataType");
            return new Type(typeNode.get("dataType"), false);
        }

    }

    public static boolean isSubtype(String subtype, String supertype, SymbolTable table) {
        //System.out.println("pesca346"+ subtype+"ola"+supertype);
        //System.out.println("pesca349"+table.getImports());
        for (String imp : table.getImports()) {
            //System.out.println("-" + imp + "-");
        }
        if (table.getImports().contains(subtype) && table.getImports().contains(supertype)) {
            return true;
        }

        while (subtype != null && !subtype.equals(supertype)) {

            String parent = table.getSuper();

            if (parent == null || parent.equals(subtype)) {
                //System.out.println("DEBUG -> Não há superclasse ou loop detectado! Retornando false.");
                return false;
            }

            subtype = parent;
        }

        //System.out.println("DEBUG -> Encontrado! " + subtype + " é subtipo de " + supertype);
        return subtype != null;
    }


    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table, String currentMethod) {
        //System.out.println("qwe "+ expr.getKind().toString());
        //System.out.println(">> currentMethod: " + currentMethod);
        //System.out.println(">> params keys: " + table.getMethods());
        switch (expr.getKind()) {

            case "VarRefExpr": {
                var varName = expr.get("name");
                List<Symbol> params = table.getParameters(currentMethod);
                if (params == null) {
                    // handle gracefully, log warning or throw a descriptive error
                    System.err.println("No parameters found for method: " + currentMethod);
                    return new Type("undefined", false); // or something meaningful
                }
                else{
                    var paramss = table.getParameters(currentMethod).stream()
                            .filter(p -> p.getName().equals(varName))
                            .findFirst();
                    if (paramss.isPresent()) return paramss.get().getType();
                }


                var localVar = table.getLocalVariables(currentMethod).stream()
                        .filter(v -> v.getName().equals(varName))
                        .findFirst();
                if (localVar.isPresent()) return localVar.get().getType();

                var field = table.getFields().stream()
                        .filter(f -> f.getName().equals(varName))
                        .findFirst();
                if (field.isPresent()) return field.get().getType();

                break;
            }

            case "ArrayAcessExpr": {
                JmmNode arrayExpr = expr.getChild(0);
                Type arrayType = getExprType(arrayExpr, table, currentMethod);

                if (!arrayType.isArray()) {
                    //System.out.println("Erro: Tentativa de aceder um elemento de algo que não é um array!");
                    return new Type("unknown", false);
                }

                return new Type("int", false);
            }

            case "NewExpr":
                return new Type(expr.get("name"), false);

            case "NewArrayExpr":
                return new Type("int", true);

            case "ArrayExpr": {
                for (JmmNode element : expr.getChildren()) {
                    Type elementType = getExprType(element, table, currentMethod);
                    if (!elementType.getName().equals("int") || elementType.isArray()) {
                        //System.out.println("Erro: Todos os elementos do array devem ser inteiros!");
                        return new Type("unknown", true);
                    }
                }
                return new Type("int", true);
            }

            case "ParenExpr":
                return getExprType(expr.getChild(0), table, currentMethod);

            case "BinaryExpr": {
                //System.out.println("pesca1");
                //System.out.println(expr.getChildren().get(0));
                //System.out.println(expr.getChildren().get(1));
                var leftType = getExprType(expr.getChildren().get(0), table, currentMethod);
                var rightType = getExprType(expr.getChildren().get(1), table, currentMethod);
                var op = expr.get("op");
                //System.out.println("pesca2");
                //System.out.println(leftType);
                //System.out.println(rightType);
                if (leftType.isArray() || rightType.isArray()) return new Type("unknown", false);

                if ((op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) &&
                        leftType.getName().equals("int") && rightType.getName().equals("int")) {
                    return new Type("int", false);
                }

                if (op.equals("&&") && leftType.getName().equals("boolean") && rightType.getName().equals("boolean")) {
                    return new Type("boolean", false);
                }

                if (op.equals("<") && leftType.getName().equals("int") && rightType.getName().equals("int")) {
                    return new Type("boolean", false);
                }

                return new Type("unknown", false);
            }
            case "UnaryExpr": {
                JmmNode exprNode = expr.getChild(0);
                Type exprType = getExprType(exprNode, table, currentMethod);

                if (!exprType.getName().equals("boolean") || exprType.isArray()) {
                    //System.out.println("Erro: O operador '!' só pode ser usado em valores booleanos.");
                    return new Type("unknown", false);
                }

                return new Type("boolean", false);
            }

            case "ThisExpr":{
                //var object = expr.getClass("name");
                var type = expr.getAncestor(Kind.CLASS_DECL).get().get("name");
                //System.out.println("pesquinha1" + type);
                return new Type(type, false);
            }

            case "IntegerLiteral":
                return new Type("int", false);

            case "Identifier":
                return new Type("boolean", false);

            case "MethodCallExpr": {
                var methodName = expr.get("name");
                var returnType = table.getReturnType(methodName);
                if (returnType != null) return returnType;

                var object = expr.getChild(0).get("name");
                var class_ = expr.getAncestor(Kind.CLASS_DECL).get().get("name");
                var type = getVarType(table, object, currentMethod).getName();

                if (type.equals("assume") || isSubtype(class_, type, table) || table.getImports().contains(type)) {
                    return new Type("assume", false);
                }

                return new Type("unknown", false);
            }
            case "LengthExpr":
                if (expr.get("name").equals("length"))
                    return new Type("int", false);
            default:
                return new Type("unknown", false);
        }



        return new Type("unknown", false);
    }

    public static Type getVarType(SymbolTable table, String varName, String currentMethod) {
        var locals = table.getLocalVariables(currentMethod);
        var parameters = table.getParameters(currentMethod);
        var fields = table.getFields();
        Type varType;
        if (varName.equals("this")) {
            String currentClass = table.getClassName();
            //System.out.println(table.getClassName());
            return new Type(currentClass, false); // "this" é uma instância da classe, não um array
        }
        for(var local : locals){
            //System.out.println("pesca101");
            if (local.getName().equals(varName)) {
                varType = local.getType();
                return varType;
            }
        }
        for(var parameter : parameters){
            //System.out.println("pesca102");
            if (parameter.getName().equals(varName)) {
                varType = parameter.getType();
                return varType;
            }
        }

        if (table.getImports().contains(varName)) {
            return new Type("assume", false);
        }
        return new Type("assume", false);
    }
    public boolean isLocal(String varName, SymbolTable table, String currentMethod) {
        return table.getLocalVariables(currentMethod).stream().anyMatch(local -> local.getName().equals(varName));
    }

    public boolean isParameter(String varName, SymbolTable table, String currentMethod) {
        return table.getParameters(currentMethod).stream().anyMatch(parameter -> parameter.getName().equals(varName));
    }
    public boolean isField(String varName, SymbolTable table, String currentMethod) {
        return !isLocal(varName, table, currentMethod) && !isParameter(varName, table, currentMethod) && table.getFields().stream().anyMatch(field -> field.getName().equals(varName));
    }
    public Type getFieldType(String varName, SymbolTable table) {
        var fields = table.getFields();
        for (var field : fields) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }
        return null;
    }

}
