package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class ExtraSemanticAnalysisTest {

    @Test
    public void duplicatedImport() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/DuplicatedImport.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedVariable() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/DuplicatedVariable.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void duplicatedMethod() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/DuplicatedMethod.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }
    /*
    @Test
    public void methodOverloading() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/MethodOverloading.jmm"));
        TestUtils.noErrors(result);
    }*/
    @Test
    public void assumeAssignment() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/AssumeAssignment.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void assumeAssignment2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/AssumeAssignment2.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsArrayParam() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/VarargsArrayParam.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void varargsArrayParam2() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/VarargsArrayParam2.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }

    @Test
    public void StaticThis() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/semanticanalysis/extra/StaticThis.jmm"));
        TestUtils.mustFail(result);
        System.out.println(result.getReports());
    }
}
