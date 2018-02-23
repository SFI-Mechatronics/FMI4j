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

package no.mechatronics.sfi.fmi4j.modeldescription.structure

import java.io.Serializable
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute

/**
 *
 * Dependency of scalar Unknown from Knowns in continous-time and event mode (Model Exchange),
 * and at communications points (Co-simulation)
 *
 * @author Lars Ivar Hatledal
 */
interface Unknown {

    /**
     * ScalarVariable index of Unknown
     */
    val index: Int

    /**
     * Defines the dependency of the Unknown (directly or inderectly via auxiliary variables)
     * on the Knowns in Continous-Time and Event Mode (ModelExchange) and at Communication Points (CoSimulation)
     */
    val dependencies: IntArray?

    /**
     * If present, it must be assumed that the Unknown depends on the Knowns
     * without a particular structure.
     */
    val dependenciesKind: DependenciesKind?

}

/**
 * @author Lars Ivar Hatledal
 */
@XmlAccessorType(XmlAccessType.FIELD)
class UnknownImpl: Unknown, Serializable {

    @XmlAttribute(name = "index")
    private var _index: Int? = null

    override val index: Int
        get() = _index ?: throw IllegalStateException("Index was null!")

    @XmlAttribute(name="dependencies")
    private var _dependencies: String? = null

    @delegate:Transient
    override val dependencies: IntArray? by lazy {
        _dependencies?.let {
            if (it.isEmpty()) null else it.split(" ").map { it.toInt() }.toIntArray()
        }
    }

    @XmlAttribute
    override val dependenciesKind: DependenciesKind? = null

    override fun toString(): String {
        return "UnknownImpl(index=$index)"
    }

}