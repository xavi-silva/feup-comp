package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.ClassType;
import org.specs.comp.ollir.inst.*;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Map.entry;
import static org.specs.comp.ollir.OperationType.LTH;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final JasminUtils types;

    private final FunctionClassMap<TreeNode, String> generators;

    private static int labelCounter = 0;
    private final Map<String, Integer> stackUsage = Map.ofEntries(
            entry("aload", 1),
            entry("areturn", -1),
            entry("ArrayLength", 0),
            entry("astore", -1),
            entry("bipush", 1),
            entry("getfield", 0),
            entry("goto", 0),
            entry("iadd", -1),
            entry("iaload", -1),
            entry("iastore", -3),
            entry("iconst", 1),
            entry("idiv", -1),
            entry("iload", 1),
            entry("imul", -1),
            entry("InvokeSpecial", 0),
            entry("InvokeStatic", 1),
            entry("InvokeVirtual", 0),
            entry("ireturn", -1),
            entry("istore", -1),
            entry("isub", -1),
            entry("ixor", -1),
            entry("ldc", 1),
            entry("new", 1),
            entry("newarray", 0),
            entry("putfield", -2),
            entry("return", 0),
            entry("sipush", 1),
            entry("if_icmplt",-2),
            entry("if_icmpge",-2),
            entry("ifge",-1),
            entry("iflt",-1),
            entry("iinc",0)
            );
    private int currentStack = 0;
    private int maxStack = 0;


    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        types = new JasminUtils(ollirResult);

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(NewInstruction.class, this::generateNew);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(ArrayOperand.class, this::generateArrayOperand);
        generators.put(Field.class, this::generateField);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(GotoInstruction.class, this::generateGoTo);

    }

    private String apply(TreeNode node) {
        var code = new StringBuilder();

        // Print the corresponding OLLIR code as a comment
        //code.append("; ").append(node).append(NL);
        //System.out.println("NÓ");
        //System.out.println(node.toString());
        //System.out.println(node.getClass().getSimpleName());

        if (currentMethod != null) {
            var labels = currentMethod.getLabels();
            for (var key : labels.keySet()){
                if (node == labels.get(key)) {
                    code.append(key).append(":").append(NL);
                }
            }
        }

        code.append(generators.apply(node));

        return code.toString();
    }


    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL).append(NL);

        String fullSuperClass;
        if(classUnit.getSuperClass() == null) {
             fullSuperClass = "java/lang/Object";
        }
        else{
            fullSuperClass = classUnit.getSuperClass();
        }


        code.append(".super ").append(fullSuperClass).append(NL);
        for (Field field : ollirResult.getOllirClass().getFields()) {
            code.append(apply(field)).append(NL);
        }

        // generate a single constructor method
        var defaultConstructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial %s/<init>()V
                    return
                .end method
                """.formatted(fullSuperClass);
        code.append(defaultConstructor);


        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {
            currentStack = 0;
            maxStack = 0;
            // Ignore constructor, since there is always one constructor
            // that receives no arguments, and has been already added
            // previously
            if (method.isConstructMethod()) {
                continue;
            }

            code.append(apply(method));
        }

        return code.toString();
    }


    private String generateMethod(Method method) {
        //System.out.println("STARTING METHOD " + method.getMethodName());
        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = types.getModifier(method.getMethodAccessModifier());

        var methodName = method.getMethodName();

        var params = method.getParams();

        code.append("\n.method ").append(modifier);
        if(method.isStaticMethod()){
            code.append("static ");
        }
        code.append(methodName).append("(");
        for(var p : params){
            //System.out.println("PARAMS: " + p.getType().toString());
            String t = switch (p.getType().toString()){
                case "INT32" -> "I";
                case "INT32[]" -> "[I";
                case "BOOLEAN" -> "Z";
                case "STRING[]" -> "[Ljava/lang/String;";
                default -> {
                    if (p.getType() instanceof ClassType classType) {
                        yield "L" + classType.getName() + ";";
                    } else {
                        throw new RuntimeException("Unsupported type: " + p.getType());
                    }
                }
            };
            code.append(t);

        }

        var returnType = switch(method.getReturnType().toString()){
            case "VOID" -> "V";
            case "INT32" -> "I";
            case "INT32[]" -> "[I";
            case "BOOLEAN" -> "Z";
            default -> "";
        };


        code.append(")" +returnType).append(NL);

        // Add limits


        StringBuilder instructionCode = new StringBuilder();
        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            instructionCode.append(instCode);
        }

        code.append(TAB).append(".limit stack ").append(maxStack).append(NL);
        Descriptor maxRegister = method.getVarTable().values().stream().max(Comparator.comparingInt(Descriptor::getVirtualReg)).orElse(new Descriptor(0));

        int locals = maxRegister.getVirtualReg() + 1;
        code.append(TAB).append(".limit locals ").append(locals).append(NL);
        code.append(instructionCode).append(NL);

        code.append(".end method\n");

        // unset method
        currentMethod = null;
        //System.out.println("ENDING METHOD " + method.getMethodName());
        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        //code.append(apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand dest)) {
            throw new NotImplementedException(lhs.getClass());
        }
        var reg = currentMethod.getVarTable().get(dest.getName());

        if (assign.getRhs() instanceof BinaryOpInstruction binaryOpInstruction) {
            if (binaryOpInstruction.getRightOperand() instanceof LiteralElement literal &&
                    binaryOpInstruction.getLeftOperand() instanceof Operand operand) {
                if (dest.getName().equals(operand.getName()) && (Integer.parseInt(literal.getLiteral()) >= -128) && (Integer.parseInt(literal.getLiteral()) <= 127)) {
                    code.append("iinc ").append(reg.getVirtualReg()).append(" ").append(literal.getLiteral());
                    return code.toString();
                }
            }
            else if (binaryOpInstruction.getLeftOperand() instanceof LiteralElement literal &&
                    binaryOpInstruction.getRightOperand() instanceof Operand operand) {
                if (dest.getName().equals(operand.getName()) && (Integer.parseInt(literal.getLiteral()) >= -128) && (Integer.parseInt(literal.getLiteral()) <= 127)) {
                    code.append("iinc ").append(reg.getVirtualReg()).append(" ").append(literal.getLiteral());
                    updateStack("iinc");
                    return code.toString();
                }
            }

        }

        if (dest instanceof ArrayOperand arrayOperand) {
            code.append(generateOperand(arrayOperand));
            code.append(generators.apply(arrayOperand.getIndexOperands().getFirst()));
            code.append(generators.apply(assign.getRhs()));
            code.append("iastore").append(NL);
            updateStack("iastore");
            return code.toString();
        }
        code.append(apply(assign.getRhs()));




        // get register


        // TODO: Hardcoded for int type, needs to be expanded
        //System.out.println("TYPE: " + operand.getType().toString());
        var store = store(reg.getVirtualReg(), dest.getType().toString());
        code.append(store).append(NL);



        //code.append("istore ").append(reg.getVirtualReg()).append(NL);

        return code.toString();
    }

    private String replaceWithIinc(String code) {
        var lines = new ArrayList<>(List.of(code.split("\\R"))); // Split on newline
        for (int i = 0; i < lines.size() - 5; i++) {
            String l1 = lines.get(i).trim();
            String l2 = lines.get(i + 1).trim();
            String l3 = lines.get(i + 2).trim();
            String l4 = lines.get(i + 3).trim();
            String l5 = lines.get(i + 4).trim();
            String l6 = lines.get(i + 5).trim();

            if (l1.startsWith("iload") && (l2.startsWith("iconst") || l2.startsWith("bipush") ||
                    l2.startsWith("sipush") || l2.startsWith("ldc") &&
                    l3.equals("iadd")) && l4.startsWith("istore") &&
                    l5.equals("iload") && l6.startsWith("istore"))
            {

                int regLoad = extractReg(l1);
                int regStore = extractReg(l6);
                int regLoad2 = extractReg(l5);
                int regStore2 = extractReg(l4);
                if (regLoad != regStore) continue;
                if (regLoad2 != regStore2) continue;

                int value = extractImmediate(l2);
                lines.set(i, "iinc " + regLoad + " " + value);
                // Remove i+1 to i+3
                lines.subList(i + 1, i + 6).clear();
                break; // Only optimize once for now; remove break to do multiple
            }
        }
        return String.join(System.lineSeparator(), lines);
    }

    private int extractReg(String line) {
        return Integer.parseInt(line.substring("iload ".length()));
    }

    private int extractImmediate(String line) {
        if (line.startsWith("iconst")) {
            return Integer.parseInt(line.substring("iconst ".length()));
        } else if (line.startsWith("bipush") || (line.startsWith("sipush"))) {
            return Integer.parseInt(line.substring("bipush ".length()));
        } else if (line.startsWith("ldc")) {
            return Integer.parseInt(line.substring("ldc ".length()));
        }
        throw new RuntimeException("Unsupported immediate pattern: " + line);
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        String lit = literal.getLiteral();
        try {
            int value = Integer.parseInt(lit);
            return constant(value) + NL;
        } catch (NumberFormatException e) {
            updateStack("ldc");
            return "ldc " + lit + NL;
        }
    }

    private String generateArrayOperand(ArrayOperand arrayOperand) {
        StringBuilder code = new StringBuilder();

        code.append(generateOperand(arrayOperand));
        code.append(generators.apply(arrayOperand.getIndexOperands().get(0)));
        code.append("iaload").append(NL);
        updateStack("iaload");
        return code.toString();
    }

    private String generateOperand(Operand operand) {
        System.out.println("oaaaaaaaaaa + " + operand.getName());
        if (operand.getName().equals("this")) {
            updateStack("aload");
            return "aload_0" + NL;
        }

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName());

        String typeString;

        if (operand instanceof ArrayOperand) {
            typeString = "array"; // ou "REFERENCE"
        } else {
            typeString = operand.getType().toString();
        }


        var load = load(reg.getVirtualReg(), typeString);
        return load + NL;
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right

        boolean islt = false;
        boolean isge = false;

        if (binaryOp.getOperation().getOpType().equals(LTH)){
            if (binaryOp.getRightOperand().isLiteral() && ((LiteralElement)binaryOp.getRightOperand()).getLiteral().equals("0")) {
                islt = true;
            }
            else code.append(apply(binaryOp.getLeftOperand()));

            if (binaryOp.getLeftOperand().isLiteral() && ((LiteralElement)binaryOp.getLeftOperand()).getLiteral().equals("0")) {
                isge = true;
            }
            else code.append(apply(binaryOp.getRightOperand()));
        }
        else{
            code.append(apply(binaryOp.getLeftOperand()));
            code.append(apply(binaryOp.getRightOperand()));
        }

        var typePrefix = "i";

        // apply operation
        var opType = binaryOp.getOperation().getOpType();
        String opCode;

        switch (opType) {
            case ADD ->{
                opCode = typePrefix + "add" + NL;
                updateStack(typePrefix + "add");
            }
            case SUB ->{
                updateStack(typePrefix + "sub");
                opCode = typePrefix + "sub" + NL;
            }
            case MUL -> {
                updateStack(typePrefix + "mul");
                opCode = typePrefix + "mul" + NL;
            }
            case DIV ->{
                updateStack(typePrefix + "div");
                opCode = typePrefix + "div" + NL;
            }

            case LTH-> {
                String ifType;
                if (islt) ifType = "iflt";
                else if (isge) ifType = "ifge";
                else ifType = "if_icmplt";

                var labelId = getUniqueLabel();
                var trueLabel = "j_true_" + labelId;
                var endLabel = "j_end" + labelId;

                code.append(ifType).append(" ").append(trueLabel).append(NL);
                code.append("iconst_0").append(NL);
                code.append("goto ").append(endLabel).append(NL);
                code.append(trueLabel).append(":").append(NL);
                code.append("iconst_1").append(NL);
                code.append(endLabel).append(":").append(NL);

                updateStack(ifType);
                updateStack("iconst");
                updateStack("goto");
                updateStack("iconst");

                return code.toString();
            }
            case GTE->{
                var labelId = getUniqueLabel();
                var trueLabel = "j_true_" + labelId;
                var endLabel = "j_end" + labelId;

                code.append("if_icmpge ").append(trueLabel).append(NL);
                code.append("iconst_0").append(NL);
                code.append("goto ").append(endLabel).append(NL);
                code.append(trueLabel).append(":").append(NL);
                code.append("iconst_1").append(NL);
                code.append(endLabel).append(":").append(NL);

                updateStack("if_icmpge");
                updateStack("iconst");
                updateStack("goto");
                updateStack("iconst");
                return code.toString();
            }

            default -> throw new NotImplementedException(opType);
        }

        code.append(opCode);
        return code.toString();
    }


    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        returnInst.getOperand().ifPresent(op -> {
            code.append(apply(op));
        });

        var type = returnInst.getReturnType().toString();
        //System.out.println("TYPE: " + type);

        String retType = switch (type){
            case  "INT32", "BOOLEAN" -> "ireturn";
            case "VOID" -> "return";
            default -> "areturn";
        };
        /*if(!children.isEmpty() && children.get(0) instanceof Operand operand) {
            code.append(apply(operand));
        }*/
        updateStack(retType);
        code.append(retType).append(NL);

        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putField) {
        var code = new StringBuilder();

        // Carrega o objeto (normalmente "this")
        code.append(apply(putField.getObject()));

        // Carrega o valor a ser guardado no campo
        code.append(apply(putField.getValue()));

        // Nome da classe
        var className = ollirResult.getOllirClass().getClassName();

        // Nome do campo
        Operand fieldOperand = putField.getField();
        String fieldName = fieldOperand.getName();


        code.append("putfield ").append(className).append("/")
                .append(fieldName).append(" ")
                .append(getJasminType(fieldOperand.getType())).append(NL);
        updateStack("putfield");

        return code.toString();
    }

    private String generateGetField(GetFieldInstruction inst) {
        var code = new StringBuilder();

        // Carrega o objeto (geralmente 'this')
        code.append(apply(inst.getObject()));

        String className = ollirResult.getOllirClass().getClassName();

        Operand fieldOperand = inst.getField();
        String fieldName = fieldOperand.getName();


        code.append("getfield ")
                .append(className).append("/")
                .append(fieldName).append(" ")
                .append(getJasminType(fieldOperand.getType())).append(NL);
        updateStack("getfield");

        return code.toString();
    }

    private String generateCall(CallInstruction callInst) {
        var code = new StringBuilder();


        var invocType = callInst.getInvocationKind();

        if (!invocType.equals("InvokeStatic")) {
            code.append(apply(callInst.getCaller()));
        }

        for(var arg: callInst.getArguments()) {
            code.append(apply(arg));
        }

        Operand caller = (Operand) callInst.getCaller();
        String callerName = caller.getName(); //nome da variávael local

        String className = "";
        String methodName = "";
        if (callInst.getInvocationKind().equals("InvokeStatic")) {
            className = ((Operand) caller).getName();
            methodName = ((LiteralElement) callInst.getMethodName()).getLiteral();
        } else if (caller.getType() instanceof ClassType ct) {
            className = ct.getName();
            methodName = ((LiteralElement) callInst.getMethodName()).getLiteral();
        }
        else if (caller.getType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) caller.getType();
            className = arrayType.getElementType().toString();
            className = "";
        } else {
        }

        System.out.println("CLASS NAME : " + className);
        //ClassType callerClass = (ClassType) caller.getType();
        //String className = callerClass.getName(); //nome da classe da qual estamos a chamar o método ("Test")


        String returnType = callInst.getReturnType().toString();
        System.out.println("RETURN TYPE : " + returnType);
        String retType = switch (returnType){
            case  "INT32" -> "I";
            case "VOID" -> "V";
            case "BOOLEAN" -> "Z";
            case "INT32[]" -> "[I";
            default -> "";
        };



        /*System.out.println("CALLER NAME: " + callerName);
        System.out.println("METHOD NAME: " + methodName);
        System.out.println("class name: " + className );
        System.out.println("RET TYPE: " + retType );*/

        switch (invocType) {
            case "InvokeSpecial" -> code.append("invokespecial "); //nao percebi porque é q às vezes é invokenonvirtual??????
            case "InvokeVirtual" -> code.append("invokevirtual ");
            case "InvokeStatic" -> code.append("invokestatic ");
            case "ArrayLength" -> code.append("arraylength \n");

            default -> throw new NotImplementedException("Unsupported call type: " + invocType);
        }
        updateStack(invocType, callInst.getArguments().size());

        //tipos dos argumentos
        String argsDescriptor = callInst.getArguments().stream()
                .map(arg -> getJasminType(arg.getType()))
                .collect(Collectors.joining());


        if(!invocType.equals("ArrayLength")) {
            code.append(className).append("/")
                    .append(methodName).append("(")
                    .append(argsDescriptor).append(")")
                    .append(retType).append(NL);
        }

        return code.toString();
    }


    private String generateNew(NewInstruction newInst) {
        var code = new StringBuilder();

        var caller = newInst.getCaller();
        var type = caller.getType();

        if (type instanceof ClassType classType) {
            // Caso normal: new Test
            String className = classType.getName();
            code.append("new ").append(className).append(NL);
            updateStack("new");

        } else if (type instanceof ArrayType arrayType) {
            Element size = newInst.getArguments().get(0);

            if (size instanceof LiteralElement literal) {
                String lit = literal.getLiteral();
                int value = Integer.parseInt(lit);
                code.append(constant(value)).append(NL);
            }
            else if(size instanceof Operand operand){
                var reg = currentMethod.getVarTable().get(operand.getName());
                code.append(load(reg.getVirtualReg(), "INT32")).append(NL);
            }


            code.append("newarray int").append(NL);
            updateStack("newarray");
        } else {
            throw new NotImplementedException("Expected ClassType or ArrayType, got: " + type.getClass());
        }

        /*if (!(type instanceof ClassType callerClass)) {
            throw new NotImplementedException("Expected ClassType in generateNew, got: " + type.getClass());
        }*/
        //String callerType = callerClass.getName();

        //code.append("new ").append(callerType).append(NL);


        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCond) {
        String condition = apply(singleOpCond.getCondition());
        return condition + "ifne " + singleOpCond.getLabel();
    }

    private String generateOpCond(OpCondInstruction opCond) {
        String condition = apply(opCond.getCondition());
        return condition + "ifne " + opCond.getLabel();
    }

    private String generateGoTo(GotoInstruction goTo) {
        return "goto " + goTo.getLabel();
    }

    private String generateField(Field field) {
        StringBuilder code = new StringBuilder();
        code.append(".field public  '").append(field.getFieldName()).append("' ").append(getJasminType(field.getFieldType()));
        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOpInst) {
        StringBuilder code = new StringBuilder();
        code.append(apply(unaryOpInst.getOperand()));
        code.append("iconst_1").append(NL);
        code.append("ixor").append(NL);
        updateStack("iconst");
        updateStack("ixor");
        return code.toString();
    }

    //UTILS
    private String load(int reg, String type) {

        StringBuilder code = new StringBuilder();
        String prefix = switch (type) {
            case "INT32", "BOOLEAN" -> "i";
            default -> "a";
        };
        updateStack(prefix+"load");
        if (reg <= 3) return code.append(prefix).append("load_").append(reg).toString();
        return code.append(prefix).append("load ").append(reg).toString();
    }

    private String constant(int value){
        StringBuilder code = new StringBuilder();
        if (value == -1){
            updateStack("iconst");
            return code.append("iconst_m1").toString();
        }
        else if (value >= 0 && value <= 5){
            updateStack("iconst");
            return code.append("iconst_").append(value).toString();
        }
        else if (value >= Byte.MIN_VALUE && value <= 127){
            updateStack("bipush");
            return code.append("bipush ").append(value).toString();
        }
        else if (value >= -32768 && value <= 32767){
            updateStack("sipush");
            return code.append("sipush ").append(value).toString();
        }
        updateStack("ldc");
        return code.append("ldc ").append(value).toString();
    }

    private String store(int reg, String type) {
        String prefix = switch(type){
            case "INT32", "BOOLEAN" -> "i";
            default -> "a";
        };
        StringBuilder code = new StringBuilder();
        updateStack(prefix+"store");
        if (reg >= 0 && reg <= 3) return code.append(prefix).append("store_").append(reg).toString();
        return code.append(prefix).append("store ").append(reg).toString();
    }


    private String getJasminType(Type type) {
        System.out.println("JASMIN TYPE: " + type.toString());
        String typeStr = type.toString();
        if (typeStr.startsWith("OBJECTREF(") || typeStr.startsWith("CLASS(") || typeStr.startsWith("THIS(")) {
            String className = typeStr.substring(typeStr.indexOf('(') + 1, typeStr.indexOf(')'));
            return "L" + className + ";";
        }

        return switch (type.toString()) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "INT32[]" -> "[" + getJasminType(((ArrayType) type).getElementType());
            case "OBJECTREF", "CLASS" -> "L" + ((ClassType) type).getName() + ";";
            case "THIS" -> null;
            case "STRING" -> "Ljava/lang/String;";
            case "VOID" -> "V";
            default -> null;
        };
    }


    private String getUniqueLabel() {
        return String.valueOf(labelCounter++);
    }



    private void updateStack(String instruction) {
        updateStack(instruction, 0);
    }

    private void updateStack(String instruction, int argumentsNumber) {

        this.currentStack += this.stackUsage.get(instruction) - argumentsNumber;

        if (this.currentStack > this.maxStack) {
            this.maxStack = this.currentStack;
        }
        System.out.println("CurrentMethod:"+ currentMethod.getMethodName() + instruction + "-> current stack:" + currentStack + "max stack:" + maxStack );
    }
}