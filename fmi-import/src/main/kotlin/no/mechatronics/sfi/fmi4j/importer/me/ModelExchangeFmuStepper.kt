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

package no.mechatronics.sfi.fmi4j.importer.me

import no.mechatronics.sfi.fmi4j.common.FmiSimulation
import no.mechatronics.sfi.fmi4j.common.FmiStatus
import no.mechatronics.sfi.fmi4j.common.FmuInstance
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private const val EPS = 1E-13

/**
 * Wraps a Model Exchange instance, turning it into a FmiSimulation
 *
 * @author Lars Ivar Hatledal
 */
class ModelExchangeFmuStepper internal constructor(
        private val fmuInstance: ModelExchangeFmuInstance,
        private val solver: Solver
) : FmiSimulation, FmuInstance by fmuInstance {

    private val x: DoubleArray
    private val nominalStates: DoubleArray
    private val dx: DoubleArray

    private val pz: DoubleArray
    private val z: DoubleArray

    override var currentTime: Double = 0.0
        private set

    override val modelDescription = fmuInstance.modelDescription

    init {

        val numberOfContinuousStates = modelDescription.numberOfContinuousStates
        val numberOfEventIndicators = modelDescription.numberOfEventIndicators

        this.x = DoubleArray(numberOfContinuousStates)
        this.nominalStates = DoubleArray(numberOfContinuousStates)
        this.dx = DoubleArray(numberOfContinuousStates)

        this.pz = DoubleArray(numberOfEventIndicators)
        this.z = DoubleArray(numberOfEventIndicators)

        this.solver.setEquations(object : Equations {
            override val dimension: Int = modelDescription.numberOfContinuousStates
            override fun computeDerivatives(time: Double, y: DoubleArray, yDot: DoubleArray) {
                for ((index, value) in dx.withIndex()) {
                    yDot[index] = value
                }
            }
        })

    }

    private fun FmiStatus.warnOnStatusNotOK(functionName: String) {
        if (this != FmiStatus.OK) {
            LOG.warn("$functionName return status $this")
        }
    }

    private fun eventIteration(): Boolean {

        fmuInstance.eventInfo.setNewDiscreteStatesNeededTrue()
        fmuInstance.eventInfo.setTerminateSimulationFalse()

        while (fmuInstance.eventInfo.getNewDiscreteStatesNeeded()) {
            fmuInstance.newDiscreteStates().also {
                it.warnOnStatusNotOK("fmuInstance.newDiscreteStates()")
            }
            if (fmuInstance.eventInfo.getTerminateSimulation()) {
                LOG.debug("eventInfo.getTerminateSimulation() returned true. Terminating FMU...")
                terminate()
                return true
            }
        }

        fmuInstance.enterContinuousTimeMode().also {
            it.warnOnStatusNotOK("fmuInstance.enterContinuousTimeMode()")
        }

        return false
    }

    override fun init() {
        init(0.0)
    }

    override fun init(start: Double) {
        init(start, 0.0)
    }

    override fun init(start: Double, stop: Double) {
        if (!isInitialized) {
            fmuInstance.init(start, stop)
            currentTime = start
            if (eventIteration()) {
                throw IllegalArgumentException()
            }
        }
    }

    override fun doStep(stepSize: Double): Boolean {

        if (stepSize <= 0) {
            throw IllegalArgumentException("stepSize must be positive and greater than 0! Was: $stepSize")
        }

        var time: Double = currentTime
        val stopTime: Double = time + stepSize

        while (time < stopTime) {

            var tNext = Math.min(time + stepSize, stopTime)

            val timeEvent = fmuInstance.eventInfo.getNextEventTimeDefined() && fmuInstance.eventInfo.getNextEventTime() <= time
            if (timeEvent) {
                tNext = fmuInstance.eventInfo.getNextEventTime()
            }

            var stateEvent = false
            if ((tNext - time) > EPS) {
                solve(time, tNext).also { result ->
                    stateEvent = result.first
                    time = result.second
                }
            } else {
                time = tNext
            }

            fmuInstance.setTime(time).also {
                it.warnOnStatusNotOK("fmuInstance.setTime()")
            }

            var enterEventMode = false
            if (!modelDescription.completedIntegratorStepNotNeeded) {
                val completedIntegratorStep = fmuInstance.completedIntegratorStep()
                if (completedIntegratorStep.terminateSimulation) {
                    LOG.debug("completedIntegratorStep.terminateSimulation returned true. Terminating FMU...")
                    terminate()
                    return false
                }
                enterEventMode = completedIntegratorStep.enterEventMode
            }

            if (timeEvent || stateEvent || enterEventMode) {

                fmuInstance.enterEventMode().also {
                    it.warnOnStatusNotOK("fmuInstance.enterEventMode()")
                }

                if (eventIteration()) {
                    return false
                }

            }

        }

        currentTime = time
        return true

    }

    private fun solve(t: Double, tNext: Double): Pair<Boolean, Double> {

        fmuInstance.getContinuousStates(x)
        fmuInstance.getDerivatives(dx)

        val dt = (tNext - t)
        val integratedTime = solver.integrate(t, x, (currentTime + dt), x)

        fmuInstance.setContinuousStates(x)

        System.arraycopy(z, 0, pz, 0, z.size)

        fmuInstance.getEventIndicators(z)

        fun stateEvent(): Boolean {

            for (i in pz.indices) {
                if (pz[i] * z[i] < 0) {
                    return true
                }
            }
            return false
        }

        return stateEvent() to integratedTime

    }

    private companion object {
        val LOG: Logger = LoggerFactory.getLogger(ModelExchangeFmuStepper::class.java)
    }

}