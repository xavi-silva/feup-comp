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

import java.util.HashSet;
import java.util.Set;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class Duplicates extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        //System.out.println("buildVisitor foi chamado!");
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit("Import", this::visitImport);
    }

    private void debugMethod(JmmNode method) {
        //System.out.println("Método analisado: " + method.get("name"));
        for (JmmNode child : method.getChildren()) {
          //  System.out.println(" -> Nó filho: " + child.getKind() + " " + child.getAttributes());
        }
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        //var newParams = method.getChildren(Kind.PARAM);

        var methods = table.getMethods();
        //boolean paramsMatch;
        int count = 0;
        for(var existingMethod : methods){
            //paramsMatch = true;
            if (existingMethod.equals(method.get("name"))) {
                count++;
                /* Logic for method overloading
                var currentParams = table.getParameters(currentMethod);
                if (currentParams.size() == newParams.size()) {
                    for (int i = 0; i < newParams.size(); i++) {
                        if (!currentParams.get(i).getType().getName().
                                equals(newParams.get(i).getChild(0).get("dataType"))) {
                            paramsMatch = false;
                        }
                    }
                    if (paramsMatch){
                        count++;
                    }
                }*/
            }
        }

        if (count > 1){
            var message = "Duplicated method:" + method.get("name");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    method.getLine(),
                    method.getColumn(),
                    message,
                    null)
            );
        }
    /*
        if (methods.contains(currentMethod)) {
            // check if params match
            var currentParams = table.getParameters(currentMethod);
            if (currentParams.size() == newParams.size()) {
                for (int i = 0; i < newParams.size(); i++) {
                    if (!currentParams.get(i).getType().getName().
                            equals(newParams.get(i).getChild(0).get("dataType"))) {
                        paramsMatch = false;
                        break;
                    }
                }
            }
        }
        if (paramsMatch) {
            var message = "Duplicated method:" + method.get("name");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    method.getLine(),
                    method.getColumn(),
                    message,
                    null)
            );
        }*/
        return null;
    }

    private Void visitVarDecl(JmmNode var, SymbolTable table) {
        var varName = var.get("name");
        var localVars = table.getLocalVariables(currentMethod);
        var fieldVars = table.getFields();
        int countField = 0;
        int count = 0;


        JmmNode typeNode = var.getChild(0);
        String kind = typeNode.getKind();
        if(kind.equals("VarArgs")){
            String message = "Varargs (e.g., int...) can only be used in method parameters.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    var.getLine(),
                    var.getColumn(),
                    message,
                    null
            ));
            return null;
        }
        if (localVars != null && currentMethod != null) {
            for (var localVar : localVars){
                if (localVar.getName().equals(varName)) {
                    count++;
                }
            }
        }
        if (fieldVars != null && currentMethod == null){
            for (var fieldVar : fieldVars){
                if (fieldVar.getName().equals(varName)) {
                    countField++;
                }
            }
        }

        if (count > 1){
            var message = "Duplicated variable:" + varName;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    var.getLine(),
                    var.getColumn(),
                    message,
                    null)
            );
        }
        if (countField > 1){
            var message = "Duplicated field:" + varName;
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    var.getLine(),
                    var.getColumn(),
                    message,
                    null)
            );
        }
        return null;
    }
    private final Set<String> seenSimpleNames = new HashSet<>();

    private Void visitImport(JmmNode newImport, SymbolTable table) {
        var currentImports = table.getImports();
        //var newImports = program.getChildren().subList(0, program.getChildren().size()-1); // Remove class

        var fullImportList = newImport.getObjectAsList("name", String.class);

        if (fullImportList == null || fullImportList.isEmpty()) return null;

        String simpleName = fullImportList.get(fullImportList.size() - 1);
        String fullImport = String.join(".", fullImportList);
        var count = 0;
        if (seenSimpleNames.contains(simpleName)) {
            var message = "Import conflict: multiple classes named '" + simpleName + "' imported from different packages.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    newImport.getLine(),
                    newImport.getColumn(),
                    message,
                    null)
            );
        } else {
            seenSimpleNames.add(simpleName);
        }

        return null;


        /*if (count > 1){
            var message = "Duplicated import: " + newImport.get("name");
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    newImport.getLine(),
                    newImport.getColumn(),
                    message,
                    null)
            );
        }*/
        /*
        for (var import_ : newImports){
            if (currentImports.contains(import_.get("name"))) {
                var message = "Duplicated import: " + import_.get("name");
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        import_.getLine(),
                        import_.getColumn(),
                        message,
                        null)
                );
            }
        }*/
        //return null;
    }
}
