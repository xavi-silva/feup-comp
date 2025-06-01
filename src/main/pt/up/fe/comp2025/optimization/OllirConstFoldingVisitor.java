package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.ast.AJmmNode;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;

import java.util.List;

import static pt.up.fe.comp2025.ast.Kind.BINARY_EXPR;

public class OllirConstFoldingVisitor extends AJmmVisitor<Void, Boolean> {
    @Override
    protected void buildVisitor(){
        addVisit("BinaryExpr", this::visitBinaryExpr);
        addVisit("ParenExpr", this::visitParenExpr);
        addVisit("UnaryExpr", this::visitUnaryExpr);
        setDefaultVisit(this::defaultVisit);
    }

    private boolean visitUnaryExpr(JmmNode node, Void unused) {
        JmmNode expr = node.getChild(0);

        if (expr.getKind().equals("Identifier")) {
            JmmNode newNode = null;
            boolean value = Boolean.parseBoolean(expr.get("value"));
            value = !value;
            newNode = new JmmNodeImpl(List.of("Identifier"));
            newNode.put("value", Boolean.toString(value));
            node.replace(newNode);
            return true;
        }
        return visit(node.getChild(0));

    }

    private boolean visitParenExpr(JmmNode node, Void unused) {
        JmmNode expr = node.getChild(0);

        if (expr.getKind().equals("IntegerLiteral") || expr.getKind().equals("Identifier")) {
            node.replace(expr);
            return true;
        }

        return visit(expr);
    }

    private boolean visitBinaryExpr(JmmNode node, Void unused) {
        var left = node.getChild(0);
        var right = node.getChild(1);

        if((left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) || (left.getKind().equals("Identifier") && right.getKind().equals("Identifier"))){
            boolean boolResult;
            JmmNode newNode = null;
            int opResult;
            switch (node.get("op")) {
                case "+":
                    opResult = Integer.parseInt(left.get("value")) + Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(List.of("IntegerLiteral"));
                    newNode.put("value", Integer.toString(opResult));
                    break;
                case "-":
                    opResult = Integer.parseInt(left.get("value")) - Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(List.of("IntegerLiteral"));
                    newNode.put("value", Integer.toString(opResult));
                    break;
                case "*":
                    opResult = Integer.parseInt(left.get("value")) * Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(List.of("IntegerLiteral"));
                    newNode.put("value", Integer.toString(opResult));
                    break;
                case "/":
                    opResult = Integer.parseInt(left.get("value")) / Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(List.of("IntegerLiteral"));
                    newNode.put("value", Integer.toString(opResult));
                    break;
                case "&&":
                    boolResult = Boolean.parseBoolean(left.get("value")) && Boolean.parseBoolean(right.get("value"));
                    newNode = new JmmNodeImpl(List.of("Identifier"));
                    newNode.put("value", Boolean.toString(boolResult));
                    break;
                case "<":
                    boolResult = Integer.parseInt(left.get("value")) < Integer.parseInt(right.get("value"));
                    newNode = new JmmNodeImpl(List.of("Identifier"));
                    newNode.put("value", Boolean.toString(boolResult));
                    break;
                default:
                    break;
            }
            node.replace(newNode);
            return true;
        }
        boolean folded = false;
        for (JmmNode child : node.getChildren()) {
            folded = visit(child) || folded;
        }

        return folded;
    }
    private boolean defaultVisit(JmmNode node, Void unused) {
        boolean folded = false;

        for (var child : node.getChildren()) {
            folded = visit(child) || folded;
        }

        return folded;
    }
}
