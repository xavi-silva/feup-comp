package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public class JmmSymbolTable extends AJmmSymbolTable {

    private final String className;
    private final String superClassName;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final Map<String, List<Symbol>> locals;
    private final List<Symbol> fields;
    private final List<String> imports;


    public JmmSymbolTable(String className,String superClassName,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          Map<String, List<Symbol>> locals, List<Symbol> fields,
                          List<String> imports) {

        this.className = className;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.locals = locals;
        this.fields = fields;
        this.superClassName = superClassName;
        this.imports = imports;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }


    @Override
    public List<String> getMethods() {
        return methods;
    }


    @Override
    public Type getReturnType(String methodSignature) {
        return returnTypes.get(methodSignature);
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.get(methodSignature);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return locals.get(methodSignature);
    }

    @Override
    public String toString() {
        return print();
    }


}
