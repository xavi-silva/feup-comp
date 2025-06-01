package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredMethod extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit("MethodCallExpr", this::visitMethodCall);
    }

    private void debugMethod(JmmNode method) {
        for (JmmNode child : method.getChildren()) {
            //System.out.println(" -> Nó filho: " + child.getKind() + " " + child.getAttributes());
        }
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {

        var object = methodCall.getChild(0);
        //if (object.toString().equals("this"));
        var type = TypeUtils.getExprType(object, table, currentMethod);
        var methodName = methodCall.get("name");
        var methodParams = table.getParameters(methodName);
        var callParams = methodCall.getChildren().subList(1, methodCall.getChildren().size());
        var class_ = methodCall.getAncestor(Kind.CLASS_DECL).get();
        var classMethods = class_.getChildren(Kind.METHOD_DECL);
        boolean methodFound = false;
        boolean paramsMatch = true;

        if (class_.get("name").equals(type.getName()) || (object.hasAttribute("value") && object.get("value").equals("this"))){
            for (var classMethod : classMethods) {
                if (classMethod.get("name").equals(methodName)) {
                    methodFound = true;
                    break;
                }
            }
        }

        if (methodParams != null) {

            int fixedParamCount = methodParams.size();
            boolean hasVarargs = false;
            Type varargsType = null;

            if( !methodParams.isEmpty() && methodParams.getLast().getType().getName().equals("int...") ){
                hasVarargs = true;
                fixedParamCount--;
            }

            if (methodParams.size() == callParams.size()){
                for (int i = 0; i < fixedParamCount; i++) {
                    var methodParam = methodParams.get(i);
                    var callParam = callParams.get(i);
                    var methodParamType = TypeUtils.getExprType(callParam, table, currentMethod);
                    var callParamType = methodParam.getType();
                    if (!methodParamType.equals(callParamType)){
                        paramsMatch = false;
                        break;
                    }
                }
            }
            else paramsMatch = false;

            if(hasVarargs){
                paramsMatch = true;

                for (int i = fixedParamCount; i < callParams.size(); i++) {
                    var callParamType = TypeUtils.getExprType(callParams.get(i), table, currentMethod);

                    if (!callParamType.getName().equals("int") || callParamType.isArray()) {
                        paramsMatch = false;
                        break;
                    }
                }
            }


        }
        else if(!callParams.isEmpty()){
            paramsMatch = false;
        }

        if (!TypeUtils.isSubtype(type.getName(), table.getSuper(), table) && !table.getImports().contains(type.getName())) {
            if (!methodFound) {
                if (table.getImports().contains(object.get("name"))) return null;
                var message = "Method not declared: " + methodName;
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodCall.getLine(),
                        methodCall.getColumn(),
                        message,
                        null)
                );
            }
            if (!paramsMatch) {
                var message = "Call params do not match method params";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodCall.getLine(),
                        methodCall.getColumn(),
                        message,
                        null)
                );
            }
        }


        //debugMethod(methodCall);
        return null;
    }
}
