package no.mechatronics.sfi.fmu2jar.templates

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import no.mechatronics.sfi.fmi4j.modeldescription.ModelDescriptionParser
import no.mechatronics.sfi.fmu2jar.TEST_FMUs
import no.mechatronics.sfi.fmu2jar.currentOS
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "TEST_FMUs", matches = ".*")
class CodeGenerationTest {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CodeGenerationTest::class.java)
    }

    @Test
    fun generateBody() {

        val file = File(TEST_FMUs,
                "FMI_2.0/CoSimulation/$currentOS/20sim/4.6.4.8004/" +
                        "ControlledTemperature/ControlledTemperature.fmu")
        Assertions.assertTrue(file.exists())
        val md = ModelDescriptionParser.parse(file)
        LOG.info(CodeGeneration.generateWrapper(md) )

    }

}