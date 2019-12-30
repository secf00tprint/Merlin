/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////
package de.micromata.merlin.excel.importer

import de.micromata.merlin.importer.PropertyDelta
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.collections4.MapUtils
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import java.io.Serializable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.util.*

/**
 * Stores one imported object (e. g. MS Excel row as bean object). It also contains information about the status: New object or modified
 * object.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class ImportedElement<T>
/**
 * Later the diff properties should be replaced by HibernateHistory and AbstractBaseDO mechanism.
 * @param index The index of the element.
 * @param clazz Needed for reflection.
 * @param diffProperties List of property names which will be used for display property changes.
 */(
        /**
         * Should be unique in the ImportedSheet and is use-able for indexed properties (e. g. check boxes).
         * @return The index of this element.
         */
        val index: Int,
        val clazz: Class<T>, vararg diffProperties: String) : Serializable {

    private val diffProperties = diffProperties

    var value: T? = null
        set(value) {
            propertyDeltas = null
            field = value
        }

    var oldValue: T? = null
        set(value) {
            propertyDeltas = null
            field = value
        }

    private var propertyDeltas: List<PropertyDelta>? = null

    val propertyChanges: List<PropertyDelta>?
        get() {
            if (oldValue == null)
                return null
            if (propertyDeltas != null)
                return propertyDeltas


            val deltas = mutableListOf<PropertyDelta>()
            diffProperties.forEach { fieldname ->
                val method = determineGetter(clazz, fieldname)
                        ?: throw UnsupportedOperationException("Oups, no getter for property '$fieldname' found for (maybe typo in fieldname?): $value")
                val newValue = method.invoke(value)
                val origValue = method.invoke(oldValue)
                createPropertyDelta(fieldname, newValue, origValue, method.returnType)?.let { deltas.add(it) }
            }
            deltas.addAll(addAdditionalPropertyDeltas());

            if (deltas.isNotEmpty()) {
                propertyDeltas = deltas;
            }
            return propertyDeltas
        }


    /**
     * Wurde dieser Eintrag schon verprobt? Erst, wenn er verprobt wurde, ergeben die anderen Abfragen isModified etc. Sinn.
     * @return true if reconciled, otherwise false.
     */
    var isReconciled = false
    /**
     * Only selected values will be imported. If hasErrors = true, always false will be returned.
     * @return true if selected.
     */
    /**
     * If hasErrors == true then this item will be deselected.
     * @param selected The value to set.
     */
    var selected = false
        get() = isFaulty == false && field
        set(selected) {
            field = if (isFaulty == false) {
                selected
            } else {
                false
            }
        }
    private var errorProperties: MutableMap<String?, Any?>? = null

    open fun valueAsString(value: Any?): String? {
        return value?.toString()
    }

    /**
     * Can be overridden by sub class to add additional property deltas.
     *
     * @return Collection of additional property deltas.
     */
    open protected fun addAdditionalPropertyDeltas(): Collection<PropertyDelta> {
        return emptyList()
    }

    protected fun createPropertyDelta(fieldname: String?, newValue: Any?, origValue: Any?, type: Class<*>?): PropertyDelta? {
        var modified =
                if (type?.isAssignableFrom(BigDecimal::class.java) == true) {
                    when {
                        origValue == null -> newValue != null
                        newValue == null -> true
                        else -> (newValue as BigDecimal).compareTo(origValue as BigDecimal) != 0
                    }
                } else {
                    newValue != origValue
                }
        if (modified) {
            return PropertyDelta(fieldname, valueAsString(origValue), valueAsString(newValue))
        }
        return null
    }

    /**
     * Noch nicht verprobte Datensätze (isReconciled == false) gelten nicht als modifiziert.
     * @return true if modified, otherwise false.
     */
    val isModified: Boolean
        get() = isReconciled == true && oldValue != null && CollectionUtils.isEmpty(propertyChanges) == false

    /**
     * Noch nicht verprobte Datensätze (isReconciled == false) gelten weder als modifiziert noch als nicht modifiziert.
     * @return true if unmodified, otherwise false.
     */
    val isUnmodified: Boolean
        get() = isReconciled == true && oldValue != null && oldValue == value == true

    /**
     * Noch nicht verprobte Datensätze (isReconciled == false) gelten nicht als neu.
     * @return true if new.
     */
    val isNew: Boolean
        get() = isReconciled == true && oldValue == null

    /**
     * @return true, if errorProperties is not empty, otherwise false.
     */
    val isFaulty: Boolean
        get() = MapUtils.isNotEmpty(errorProperties)

    /**
     * For properties which can't be mapped due to errors (e. g. referenced element not found).
     * @param key Key of the error property.
     * @param value Value of the error property.
     */
    fun putErrorProperty(key: String?, value: Any?) {
        if (errorProperties == null) {
            errorProperties = HashMap()
        }
        errorProperties!![key] = value
    }

    fun removeErrorProperty(key: String?) {
        if (errorProperties != null) {
            errorProperties!!.remove(key)
        }
    }

    fun getErrorProperties(): Map<String?, Any?>? {
        return errorProperties
    }

    /**
     * @param key The key of the error property.
     * @return The error property if found, otherwise null.
     */
    fun getErrorProperty(key: String?): Any? {
        var obj: Any? = null
        if (errorProperties != null) {
            obj = errorProperties!![key]
        }
        return obj
    }

    companion object {
        private const val serialVersionUID = -3405918702811291053L

        private fun determineGetter(clazz: Class<*>, fieldname: String, onlyPublicGetter: Boolean = true): Method? {
            val cap = StringUtils.capitalize(fieldname)
            val methods: Array<Method> = getAllDeclaredMethods(clazz) ?: return null
            for (method in methods) {
                if (onlyPublicGetter == true && Modifier.isPublic(method.modifiers) == false) {
                    continue
                }
                var matches = false
                matches = if (Boolean::class.javaPrimitiveType!!.isAssignableFrom(method.returnType) == true) {
                    "is$cap" == method.name == true || "has$cap" == method.name == true || "get$cap" == method.name == true
                } else {
                    "get$cap" == method.name == true
                }
                if (matches == true) {
                    if (method.isBridge == false) { // Don't return bridged methods (methods defined in interface or super class with different return type).
                        return method
                    }
                }
            }
            return null

        }

        private fun getAllDeclaredMethods(clazz: Class<*>): Array<Method>? {
            var clazz = clazz
            var methods = clazz.declaredMethods
            while (clazz.superclass != null) {
                clazz = clazz.superclass
                methods = ArrayUtils.addAll(methods, *clazz.declaredMethods) as Array<Method>
            }
            return methods
        }
    }

}
