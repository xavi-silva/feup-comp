package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2025.ast.Kind.*;

public class JmmSymbolTableBuilder {

    // In case we want to already check for some semantic errors during symbol table building.
    private List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
    }

    public JmmSymbolTable build(JmmNode root) {

        reports = new ArrayList<>();

        // TODO: After your grammar supports more things inside the program (e.g., imports) you will have to change this
        var classDecl = root.getChild(root.getNumChildren() -1);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superClass = extractSuperClass(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);
        var imports = buildImports(root);

        return new JmmSymbolTable(className, superClass, methods, returnTypes, params, locals, fields, imports);
    }

    // faz o mapping dos metodos da class com o seu respetivo tipo de retorno
    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();
        String methodName;
        JmmNode returnTypeNode;
        Type returnType;
        for (var method : classDecl.getChildren(METHOD_DECL)) {
            methodName = method.get("name");
            //System.out.println(method.toString());
            if("main".equals(methodName)) {
                //System.out.println("PESCA1");
                //System.out.println(methodName);
                returnType= new Type("void", false);
                map.put(methodName, returnType);

            }
            else{
                returnTypeNode = method.getChild(0);
                returnType = TypeUtils.convertType(returnTypeNode);
                //System.out.println(returnType);
                map.put(methodName, returnType);

            }
            //System.out.println("aaaaaaaaaaaa");
            //System.out.println(returnTypeNode.toString());
            //System.out.println("aaaaaaaaaaaa");

            //var returnType = TypeUtils.newIntType();
        }

        return map;
    }


    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            //System.out.println(name);
            //System.out.println("PESCA1");
            List<Symbol> params;
            if ("main".equals(name)) {
                //System.out.println(method.toString());
                var paramName = method.get("paramName");
                //System.out.println(paramName.toString());
                //String paramName = method.getChild(0).get("paramName"); // O nome do argumento (ex: "args")
                //System.out.println(paramName);
                Type stringArrayType = new Type("String", true);
                params = List.of(new Symbol(stringArrayType, paramName));
            } else {
                    params = method.getChildren(PARAM).stream()
                        // TODO: When you support new types, this code has to be updated
                        .map(param -> {
                            JmmNode typeName = param.getChild(0);
                            //System.out.println(typeName.toString());
                            var paramType = TypeUtils.convertType(typeName);
                            return new Symbol(paramType, param.get("name"));
                        })
                        .toList();

            }

            map.put(name, params);
        }

        return map;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        var map = new HashMap<String, List<Symbol>>();

        for (var method : classDecl.getChildren(METHOD_DECL)) {
            var name = method.get("name");
            var locals = method.getChildren(VAR_DECL).stream()
                    .map(varDecl -> {
                        var typeNode = varDecl.getChild(0);
                        return new Symbol(TypeUtils.convertType(typeNode), varDecl.get("name"));
                    })
                    .toList();
            /*
            for (var local : locals) {
                System.out.println("aaaaa");
                System.out.println(local);
                System.out.println("bbbbb");
            }
            */
            map.put(name, locals);
        }

        return map;
    }

    private List<String> buildMethods(JmmNode classDecl) {

        var methods = classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();

        return methods;
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        var fields = new ArrayList<Symbol>();
        //System.out.println("Filhos de classDecl: " + classDecl.getChildren());

        for (var fieldNode : classDecl.getChildren("VarDecl")) {
            //System.out.println(fieldNode.toString());
            var type = TypeUtils.convertType(fieldNode.getChild(0));
            var name = fieldNode.get("name");
            fields.add(new Symbol(type, name));

        }

        //System.out.println("Fields extraídos: " + fields);
        return fields;
    }

    private String extractSuperClass(JmmNode classDecl) {
        //System.out.println("ola");
        //System.out.println("aqui");
        if (classDecl.hasAttribute("superName")) {
            //System.out.println(classDecl);

            return classDecl.get("superName"); // Retorna o nome da superclasse
        }
        return null;
    }


    private List<String> buildImports(JmmNode root){
        List<String> imports = new ArrayList<>();
        for (int i = 0; i < root.getNumChildren() - 1; i++) {
            var importNode = root.getChild(i);
            String importName = importNode.get("name");
            importName =importName.replace("[", "").replace("]", "");
            String[] parts = importName.split("\\,");
            System.out.println("wtf" + parts[parts.length - 1]);
            importName = parts[parts.length - 1];
            System.out.println("pesca347"+importName);
            imports.add(importName);

        }
        for (String imp : imports) {

            System.out.println("-" + imp + "-");
        }

        return imports;
    }


}
