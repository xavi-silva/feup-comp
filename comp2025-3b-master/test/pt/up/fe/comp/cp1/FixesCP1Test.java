package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class FixesCP1Test {
    @Test
    public void _StringField() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/fixes/1_StringField.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void Array() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/fixes/Arrays.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void Boolean() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/fixes/Boolean.jmm"));
        TestUtils.noErrors(result);
    }


    @Test
    public void ArgumentThis() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/fixes/ArgumentThis.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void FixThis() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/fixes/FixThis.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void Main() {
        var result = TestUtils
                .analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/fixes/Main.jmm"));
        TestUtils.noErrors(result);
    }

    /*
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

     */
}
