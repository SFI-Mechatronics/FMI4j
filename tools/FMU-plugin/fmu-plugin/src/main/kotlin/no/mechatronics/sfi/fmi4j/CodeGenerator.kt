/*
 * The MIT License
 *
 * Copyright 2017-2018 Norwegian University of Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING  FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.mechatronics.sfi.fmi4j

import no.mechatronics.sfi.fmi4j.modeldescription.CommonModelDescription
import no.mechatronics.sfi.fmi4j.modeldescription.ModelDescriptionProvider
import no.mechatronics.sfi.fmi4j.modeldescription.variables.*
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @author Lars Ivar Hatledal
 */
class CodeGenerator(
        private val md: CommonModelDescription
) {

    private val LOG = LoggerFactory.getLogger(CodeGenerator::class.java)

    private val modelName = md.modelName

    fun generateBody(): String {

        var solverImport = ""
        if (md.supportsModelExchange) {
            solverImport = "import no.mechatronics.sfi.fmi4j.solvers.Solver"
        }

        return """
package no.mechatronics.sfi.fmi4j;

import java.net.URL;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Iterator;
import java.io.IOException;
import no.mechatronics.sfi.fmi4j.common.*;
import no.mechatronics.sfi.fmi4j.importer.*;
import no.mechatronics.sfi.fmi4j.modeldescription.*;
import no.mechatronics.sfi.fmi4j.modeldescription.variables.*;
$solverImport

/***
 * Class wrapping FMU named '$modelName' auto-generated by FmuPlugin
 *
 * @author Lars Ivar Hatledal
 */
public class $modelName implements FmiSimulation {

    private static Fmu fmu = null;
    private final FmiSimulation instance;

    private $modelName(FmiSimulation instance) {
        this.instance = instance;
    }

    private static Fmu getOrCreateFmu() {
        if (fmu == null) {
            try {
                URL url = $modelName.class.getClassLoader().getResource("fmus/$modelName.fmu");
                fmu = Fmu.from(new File(url.getFile()));
            } catch (IOException ex) {
                throw new RuntimeException();
            }
        }
        return fmu;
    }

    ${generateFactories()}

    @Override
    public CommonModelDescription getModelDescription() {
        return instance.getModelDescription();
    }

    @Override
    public ModelVariables getModelVariables() {
        return instance.getModelVariables();
    }

    @Override
    public FmuVariableAccessor getVariableAccessor() {
        return instance.getVariableAccessor();
    }

    @Override
    public FmiStatus getLastStatus() {
        return instance.getLastStatus();
    }

    @Override
    public boolean isInitialized() {
        return instance.isInitialized();
    }

    @Override
    public boolean isTerminated() {
        return instance.isTerminated();
    }

    @Override
    public double getCurrentTime() {
        return instance.getCurrentTime();
    }

    @Override
    public void init() {
        instance.init();
    }

    @Override
    public void init(double start) {
        instance.init(start);
    }

    @Override
    public void init(double start, double stop) {
        instance.init(start, stop);
    }

    public boolean doStep(double stepSize) {
        return instance.doStep(stepSize);
    }

    @Override
    public boolean terminate() {
        return instance.terminate();
    }

    @Override
    public boolean reset() {
        return instance.reset();
    }

    private Locals locals;
    private Inputs inputs;
    private Outputs outputs;
    private Parameters parameters;
    private CalculatedParameters calculatedParameters;

    /**
     * Access variables with causality=LOCAL
     */
    public Locals getLocals() {
        if (locals == null) {
            locals = new Locals();
        }
        return locals;
    }

    /**
     * Access variables with causality=INPUT
     */
    public Inputs getInputs() {
     if (inputs == null) {
            inputs = new Inputs();
        }
        return inputs;
    }

    /**
     * Access variables with causality=OUTPUT
     */
    public Outputs getOutputs() {
     if (outputs == null) {
            outputs = new Outputs();
        }
        return outputs;
    }

    /**
     * Access variables with causality=PARAMETER
     */
    public Parameters getParameters() {
     if (parameters == null) {
            parameters = new Parameters();
        }
        return parameters;
    }

    /**
     * Access variables with causality=CALCULATED_PARAMETER
     */
    public CalculatedParameters getCalculatedParameters() {
     if (calculatedParameters == null) {
            calculatedParameters = new CalculatedParameters();
        }
        return calculatedParameters;
    }

    public class AbstractParameters implements Iterable<TypedScalarVariable<?>> {

        private final List<TypedScalarVariable<?>> vars;

        private AbstractParameters(Causality causality) {
            this.vars = getModelVariables().getByCausality(causality);
        }

        public int size() {
            return vars.size();
        }

        public List<TypedScalarVariable<?>> get() {
            return vars;
        }

        @Override
        public Iterator<TypedScalarVariable<?>> iterator() {
            return vars.iterator();
        }

    }

    public class Inputs extends AbstractParameters {

        private Inputs() {
            super(Causality.INPUT);
        }
        ${generateAccessors(Causality.INPUT)}

    }

    public class Outputs extends AbstractParameters {

        private Outputs() {
            super(Causality.OUTPUT);
        }
        ${generateAccessors(Causality.OUTPUT)}

    }

    public class Parameters extends AbstractParameters {

        private Parameters() {
            super(Causality.PARAMETER);
        }
        ${generateAccessors(Causality.PARAMETER)}

    }

    public class CalculatedParameters extends AbstractParameters {

        private CalculatedParameters() {
            super(Causality.CALCULATED_PARAMETER);
        }
        ${generateAccessors(Causality.CALCULATED_PARAMETER)}

    }

    public class Locals extends AbstractParameters {

        private Locals() {
            super(Causality.LOCAL);
        }
        ${generateAccessors(Causality.LOCAL)}

    }

}

"""

    }

    private fun generateFactories(): String {

        var result = ""
        if (md.supportsCoSimulation) {
            result += """

            public static $modelName newInstance() {
                FmiSimulation instance = getOrCreateFmu().asCoSimulationFmu().newInstance();
                return new $modelName(Objects.requireNonNull(instance));
            }
            """

        }

        if (md.supportsModelExchange) {

            result += """

            public static $modelName newInstance(Solver solver) {
                FmiSimulation instance = getOrCreateFmu().asModelExchangeFmu(solver).newInstance()
                return new $modelName(Objects.requireNonNull(instance));
            }
            """

        }

        return result

    }

    private fun generateAccessors(causality: Causality): String {

        return StringBuilder().also { sb ->

            sb.append("\n")

            md.modelVariables.getByCausality(causality).forEach { v ->

                if (!v.name.contains("[")) {

                    val functionName = v.name.replace(".", "_").decapitalize()

                    sb.append("""
                    |${generateJavaDoc(v)}
                    |public ${v.typeName}Variable $functionName() {
                    |    return instance.getModelDescription().getModelVariables().getByName("${v.name}").as${v.typeName}Variable();
                    |}
                    |
                    """.trimMargin())

                }


            }

        }.toString()

    }

    private fun generateJavaDoc(v: TypedScalarVariable<*>) : String {

        val star = " *"
        val newLine = "\n$star\n"
        return StringBuilder().apply {

            append("\n")
            append("/**\n")
            append("$star ").append("Name:").append(v.name)

            v.start?.also { append(newLine).append("$star Start=$it") }
            v.causality?.also { append(newLine).append("$star Causality=$it") }
            v.variability?.also { append(newLine).append("$star Variability=$it") }
            v.initial?.also { append(newLine).append("$star Initial=$it") }

            when (v) {
                is IntegerVariable -> {
                    v.min?.also { append(newLine).append("$star min=$it") }
                    v.max?.also { append(newLine).append("$star max=$it") }
                }
                is RealVariable -> {
                    v.min?.also { append(newLine).append("$star min=$it") }
                    v.max?.also { append(newLine).append("$star max=$it") }
                    v.nominal?.also { append(newLine).append("$star nominal=$it") }
                    v.unbounded?.also { append(newLine).append("$star unbounded=$it") }
                }
                is EnumerationVariable -> {
                    v.min?.also { append("$star min=") }
                    v.max?.also { append("$star max=") }
                    v.quantity?.also { append("$star quantity=") }
                }
            }

            v.description?.also { append(newLine).append("$star ").append("Description: $it") }

            append("\n */")

        }.toString()

    }

}

