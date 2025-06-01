package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.inst.*;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.CompilerConfig;
import org.specs.comp.ollir.Method;

import java.util.*;

public class JmmOptimizationImpl implements JmmOptimization {

    private JmmSemanticsResult savedSemanticsResult;
    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // Create visitor that will generate the OLLIR code
        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());

        // Visit the AST and obtain OLLIR code
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        //System.out.println("\nOLLIR:\n\n" + ollirCode);

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    public record Triple<A, B, C>(A first, B second, C third) {}

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        this.savedSemanticsResult = semanticsResult;
        //TODO: Do your AST-based optimizations here
        if (CompilerConfig.getOptimize(semanticsResult.getConfig())) {
            var constantPropVisitor = new OllirConstPropVisitor();
            var constantFoldingVisitor = new OllirConstFoldingVisitor();

            while (true) {
                boolean constantFolded = constantFoldingVisitor.visit(semanticsResult.getRootNode());
                boolean constantPropagated = constantPropVisitor.visit(semanticsResult.getRootNode());
                if (!constantFolded && !constantPropagated) {
                    break;
                }
            }
        }

        return semanticsResult;

    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        //System.out.println("Registering allocation");
        var config = ollirResult.getConfig();
        int k = CompilerConfig.getRegisterAllocation(config);
        //System.out.println("print n"+k);

        if (k == -1) {
            return ollirResult;
        }

        ollirResult.getOllirClass().buildCFGs();

        for (var method : ollirResult.getOllirClass().getMethods()) {
            if (k == 0) k = Integer.MAX_VALUE;
            inOutReg(method, k);
        }

        int max = 0;

        // 4 registos
        // 0 -> this
        // 1 -> args
        // 2, 3 -> a, b, temp0

        // if usedColors - this - params > k: erro

        List<Report> newLogs = new ArrayList<>(ollirResult.getReports());

        for (var method : ollirResult.getOllirClass().getMethods()) {
            var VT = method.getVarTable();
            for (var reg : VT.keySet()) {
                int val = VT.get(reg).getVirtualReg();
                max = Math.max(max, val);

                /*
                System.out.println("VAL"+ val);
                val=val-1-method.getParams().size();
                //max=max-1-method.getParams().size();
                max = Math.max(max, val);*/
            }
            int regs = max + 1 - 1 - method.getParams().size();

            if (regs > k){
                List<Report> newReports = new ArrayList<>(ollirResult.getReports());
                newReports.add(new Report(
                        ReportType.ERROR,
                        Stage.OPTIMIZATION,
                        -1,
                        -1,
                        "Value of n should be at least " + regs + ". Total registers used: " + (max + 1) + ".\n"
                ));
                TestUtils.noErrors(newReports);
                // Retornar novo OllirResult com os reports
                return new OllirResult(
                        savedSemanticsResult,
                        ollirResult.getOllirCode(),
                        newReports
                );
            }
            else{
                StringBuilder log = new StringBuilder();
                log.append("Register allocation for method ").append(method.getMethodName())
                        .append(": ").append(max + 1).append(" registers are needed\n");
                for (var x : VT.keySet()) {
                    log.append("Variable ").append(x).append(" is at register #").append(VT.get(x).getVirtualReg()).append("\n");
                }
                System.out.printf(log.toString());

                newLogs.add(Report.newLog(
                        Stage.OPTIMIZATION,
                        -1,
                        -1,
                        log.toString(),
                        null
                ));
            }
        }

        return ollirResult;
    }

    private void inOutReg(Method m, int k){
        int sz = m.getInstructions().size();

        Map<Instruction, Set<String>>IN = new HashMap<>();
        Map<Instruction, Set<String>>OUT = new HashMap<>();
        Map<Instruction, Set<String>>OLD_IN = new HashMap<>();
        Map<Instruction, Set<String>>OLD_OUT = new HashMap<>();
        Map<Instruction, Set<String>>DEF = new HashMap<>();
        Map<Instruction, Set<String>>USE = new HashMap<>();

        for (int i = 0; i < sz; i++) {
            Instruction inst = m.getInstructions().get(i);
            DEF.put(inst, getDefs(inst));
            USE.put(inst, getUses(inst));
            IN.put(inst, new HashSet<>());
            OUT.put(inst, new HashSet<>());
        }

        while(true){
            for(var inst: m.getInstructions()){
                OLD_IN.put(inst, IN.get(inst));
                OLD_OUT.put(inst, OUT.get(inst));

                Set<String> aux = new HashSet<>();
                for (var succ : inst.getSuccessors()) {
                    if (IN.get(succ) != null) {
                        aux.addAll(IN.get(succ));
                    }
                }
                OUT.put(inst, aux);

                Set<String> aux2 = new HashSet<>(OUT.get(inst));
                aux2.removeAll(DEF.get(inst));
                Set<String> inSet = new HashSet<>(USE.get(inst));
                inSet.addAll(aux2);
                IN.put(inst, inSet);

            }

            if(OLD_IN.equals(IN) && OLD_OUT.equals(OUT)){break;}


        }

        Map<String, Set<String>> interferenceGraph = new HashMap<>();

        for (String var : m.getVarTable().keySet()){
            interferenceGraph.put(var, new HashSet<>());
        }

        for(var p : m.getParams()){
            interferenceGraph.remove(((Operand) p).getName());
        }
        interferenceGraph.remove("this");

        for (Instruction inst : m.getInstructions()) {
            for(String a : DEF.get(inst)){
                for(String b : OUT.get(inst)){
                    if(!a.equals(b)){
                        if (interferenceGraph.containsKey(a) && interferenceGraph.containsKey(b)){
                            interferenceGraph.get(a).add(b);
                            interferenceGraph.get(b).add(a);
                        }
                    }
                }
            }

        }


        Stack<Triple<String, Set<String>, Integer>> stack = new Stack<>();
        Map<String, Integer> colorMap = new HashMap<>();
        Set<String> spilled = new HashSet<>();

        while(true) {
            Set<String> oldSpilled = new HashSet<>(spilled);

            while (!interferenceGraph.isEmpty()) {
                boolean flag = false;
                List<String> toRemove = new ArrayList<>();
                for (var node : new HashSet<>(interferenceGraph.keySet())) {
                    if (interferenceGraph.get(node).size() < k) {
                        Triple<String, Set<String>, Integer> vars = new Triple<>(node, interferenceGraph.get(node), -1);
                        stack.push(vars);
                        toRemove.add(node);
                        for (String node2 : toRemove) {
                            interferenceGraph.remove(node2);
                            for (String other : interferenceGraph.keySet()) {
                                interferenceGraph.get(other).remove(node2);
                            }
                        }
                        flag = true;
                    }
                }
                if (!flag) {
                    for (var node : new HashSet<>(interferenceGraph.keySet())) {
                        Triple<String, Set<String>, Integer> vars = new Triple<>(node, interferenceGraph.get(node), 0);
                        stack.push(vars);
                        interferenceGraph.remove(node);
                        for (var node2 : interferenceGraph.keySet()) {
                            interferenceGraph.get(node2).remove(node);
                        }
                    }
                }

            }

            while (!stack.isEmpty()) {
                Triple<String, Set<String>, Integer> vars = stack.pop();
                Set<Integer> usedColors = new HashSet<>();
                for (String neighbor : vars.second) {
                    if (colorMap.containsKey(neighbor)) {
                        usedColors.add(colorMap.get(neighbor));
                    }
                }
                if (vars.third.equals(-1)) {
                    interferenceGraph.put(vars.first, vars.second);

                    int color = 1 + m.getParams().size();
                    while (usedColors.contains(color)) {
                        color++;
                    }

                    colorMap.put(vars.first, color);
                } else if (vars.third.equals(0)) {
                    if (usedColors.size() < k) {
                        interferenceGraph.put(vars.first, vars.second);

                        int color = 1 + m.getParams().size();
                        while (usedColors.contains(color)) {
                            color++;
                        }

                        colorMap.put(vars.first, color);
                    } else {
                        spilled.add(vars.first);
                    }
                }
            }

            if(oldSpilled.equals(spilled)){break;}


        }

        //System.out.println("\n--- Alocação de Registradores ---");
        for (var entry : colorMap.entrySet()) {
            String var = entry.getKey();
            int reg = entry.getValue();
            if (reg >= 0) {
                //System.out.println(var + " → r" + reg);
            }
        }

        for(var s: spilled){
            //System.out.println("SPILLED "+s);
        }


        var VT = m.getVarTable();

        for (var color : colorMap.keySet()) {
            VT.put(color, new Descriptor(colorMap.get(color)));
        }

        int number = colorMap.size();
        for (var s : spilled) {
            number++;
            //System.out.println("number spilled" + number);
            VT.put(s, new Descriptor(number));
        }


        VT.put("this", new Descriptor(0));

        number = 1;
        for(var p : m.getParams()){
            //System.out.println("number param" + number);
            VT.put(((Operand) p).getName(), new Descriptor(number));
            number++;
        }
    }

    private Set<String> getDefs(Instruction instruction) {
        Set<String> defs = new HashSet<>();
        if (instruction instanceof AssignInstruction assign) {
            if (assign.getDest() instanceof Operand dest) {
                defs.add(dest.getName());
            }
        }
        return defs;
    }

    private Set<String> getUses(Instruction instruction) {
        Set<String> uses = new HashSet<>();

        if (instruction instanceof AssignInstruction assign) {
            var rhs = assign.getRhs();

            if (rhs instanceof BinaryOpInstruction binOp) {
                Element left = binOp.getLeftOperand();
                Element right = binOp.getRightOperand();

                if (left instanceof Operand lOp && !lOp.isLiteral()) {
                    uses.add(lOp.getName());
                }
                if (right instanceof Operand rOp && !rOp.isLiteral()) {
                    uses.add(rOp.getName());
                }
            }

            else if (rhs instanceof SingleOpInstruction singleOp) {
                Element single = singleOp.getSingleOperand();

                if (single instanceof Operand op && !op.isLiteral()) {
                    uses.add(op.getName());
                }
            }

        }
        else if (instruction instanceof GetFieldInstruction gf) {
            uses.add(gf.getField().getName());
        }
        else if (instruction instanceof ReturnInstruction ret) {
            ret.getOperand().ifPresent(elem -> {
                if (elem instanceof Operand op && !op.isLiteral()) {
                    uses.add(op.getName());
                }
            });

        }
        return uses;
    }


}
