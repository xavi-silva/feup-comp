package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.Kind;

import java.util.HashMap;
import java.util.Map;

import static pt.up.fe.comp2025.ast.Kind.*;

public class OllirConstPropVisitor extends AJmmVisitor<Map<String, JmmNode>, Boolean> {
    //private final Map<String, JmmNode> fieldConstMap = new HashMap<>();

    @Override
    protected void buildVisitor(){
        addVisit(CLASS_DECL, this::visitClassDecl);
        addVisit(VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit("LoopStmt", this::visitLoopStmt);
        addVisit("IfStmt", this::visitIfStmt);
        //addVisit("BinaryExpr", this::visitBinaryOp);


        //addVisit(METHOD_DECL, this::visitVarRefExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean visitBinaryOp(JmmNode node, Map<String, JmmNode> localConstMap) {
        boolean propagated = false;
        for (int i = 0; i < node.getNumChildren(); i++) {
            var child = node.getChild(i);
            propagated = visit(child, localConstMap) || propagated;
        }
        return propagated;
    }

    private boolean visitClassDecl(JmmNode node, Map<String, JmmNode> unused) {
        //fieldConstMap.clear();
        boolean propagated = false;
        for (var child : node.getChildren()) {
            propagated = visit(child) || propagated;
        }
        return propagated;
    }
    private boolean visitMethodDecl(JmmNode node, Map<String, JmmNode> unused) {
        Map<String, JmmNode> localConstMap = new HashMap<>();
        boolean propagated = false;
        for (var child : node.getChildren()) {
            propagated = visit(child, localConstMap) || propagated;
        }

        node.getChildren().removeIf(child ->
                child.getKind().equals(ASSIGN_STMT) && "true".equals(child.get("remove"))
        );

        return propagated;
    }


    private boolean visitLoopStmt(JmmNode node, Map<String, JmmNode> localConstMap) {
        JmmNode stmt = node.getChild(1);
        for (var child : stmt.getDescendants(ASSIGN_STMT)){
            localConstMap.remove(child.get("name"));
        }
        boolean propagated = false;
        for (var child : node.getChildren()){
            propagated = visit(child, localConstMap) || propagated;
        }
        return propagated;
    }

    private boolean visitIfStmt(JmmNode node, Map<String, JmmNode> localConstMap) {
        boolean propagated = false;
        propagated |= visit(node.getChild(0), localConstMap);
        HashMap<String, JmmNode> thenMap = new HashMap<>(localConstMap);
        HashMap<String, JmmNode> elseMap = new HashMap<>(localConstMap);

        propagated = visit(node.getChild(1), thenMap) || propagated;
        propagated = visit(node.getChild(2), elseMap) || propagated;

        localConstMap.keySet().removeIf(var ->
                !thenMap.containsKey(var) ||
                        !elseMap.containsKey(var) ||
                        !thenMap.get(var).equals(elseMap.get(var))
        );

        return propagated;

    }

    private boolean visitAssignStmt(JmmNode node, Map<String, JmmNode> localConstMap) {

        boolean propagated = false;

        for (var child : node.getChildren()){
            propagated = visit(child, localConstMap) || propagated;
        }

        String varName = node.get("name");
        JmmNode valueExpr = node.getChild(0);

        System.out.println("Visiting AssignStmt: " + varName + " = " + valueExpr);
        System.out.println("Before propagation, ConstMap = " + localConstMap);

        if (valueExpr.getKind().equals("IntegerLiteral") || valueExpr.getKind().equals("Identifier")) {
            localConstMap.put(varName, valueExpr);
        } else {
            localConstMap.remove(varName);
        }

        if (valueExpr.getKind().equals("IntegerLiteral")) {
            localConstMap.put(varName, valueExpr);

            //localConstMap.remove(varName);
            node.put("remove", "true");
        }


        return propagated;
    }



    private boolean visitVarRefExpr(JmmNode node, Map<String, JmmNode> localConstMap) {
        System.out.println("VAR_REF: " + node.get("name"));

        var name = node.get("name");
        if (localConstMap.containsKey(name)) {
            JmmNode original = localConstMap.get(name);
            JmmNode replacement = original.copy(original.getHierarchy());

            // Copiar todos os atributos
            for (String attr : original.getAttributes()) {
                replacement.put(attr, original.get(attr));
            }


            node.replace(replacement);
            return true;
        }

        return false;
    }

    private boolean defaultVisit(JmmNode node, Map<String, JmmNode> localConstMap) {
        boolean propagated = false;
        for (var child : node.getChildren()) {
            propagated = visit(child, localConstMap) || propagated;
        }
        return propagated;
    }


}


