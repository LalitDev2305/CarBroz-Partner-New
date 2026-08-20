package com.carbroz.partner.engine.execution.action

import com.carbroz.partner.engine.execution.binding.BindingExpression
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ActionValueTest {

    @Test
    fun verifyTextValueSemantics() {
        val val1 = ActionValue.Text("hello")
        val val2 = ActionValue.Text("hello")
        assertEquals(val1, val2)
        assertEquals("hello", val1.value)
    }

    @Test
    fun verifyIntegerValueExactness() {
        val val1 = ActionValue.Integer(999999999999L)
        assertEquals(999999999999L, val1.value)
    }

    @Test
    fun verifyDecimalValueExactTextPreservation() {
        val d1 = ActionValue.Decimal("10.50")
        val d2 = ActionValue.Decimal("0.00000001")
        val d3 = ActionValue.Decimal("999999999999999999999999.999999")
        val d4 = ActionValue.Decimal("-1234567890.123456789")

        assertEquals("10.50", d1.value)
        assertEquals("0.00000001", d2.value)
        assertEquals("999999999999999999999999.999999", d3.value)
        assertEquals("-1234567890.123456789", d4.value)
    }

    @Test
    fun verifyDecimalInvalidFormatRejection() {
        assertFailsWith<IllegalArgumentException> {
            ActionValue.Decimal("")
        }
        assertFailsWith<IllegalArgumentException> {
            ActionValue.Decimal("abc")
        }
        assertFailsWith<IllegalArgumentException> {
            ActionValue.Decimal("10.5.0")
        }
    }

    @Test
    fun verifyFlagNullAndBindingValues() {
        val flag = ActionValue.Flag(true)
        val nil = ActionValue.Null
        val bind = ActionValue.Binding(BindingExpression("\${user.id}"))

        assertEquals(true, flag.value)
        assertEquals(ActionValue.Null, nil)
        assertEquals("\${user.id}", bind.expression.rawExpression)
    }

    @Test
    fun verifyObjectDefensiveCopyingAndBlankKeyRejection() {
        val mutableMap = mutableMapOf<String, ActionValue>("a" to ActionValue.Text("valA"))
        val obj = ActionValue.Object.create(mutableMap)

        mutableMap["a"] = ActionValue.Text("MUTATED")
        mutableMap["b"] = ActionValue.Text("NEW")

        assertEquals(ActionValue.Text("valA"), obj.properties["a"])
        assertEquals(1, obj.properties.size)

        assertFailsWith<IllegalArgumentException> {
            ActionValue.Object.create(mapOf("" to ActionValue.Text("val")))
        }
        assertFailsWith<IllegalArgumentException> {
            ActionValue.Object.create(mapOf("  " to ActionValue.Text("val")))
        }
    }

    @Test
    fun verifyObjectToStringPrivacy() {
        val secretValue = "SECRET_PASS"
        val obj = ActionValue.Object.create(mapOf("pass" to ActionValue.Text(secretValue)))
        val str = obj.toString()
        assertFalse(str.contains(secretValue))
    }

    @Test
    fun verifyListDefensiveCopyingAndToStringPrivacy() {
        val secretValue = "SECRET_ITEM"
        val mutableList = mutableListOf<ActionValue>(ActionValue.Text(secretValue), ActionValue.Integer(2))
        val list = ActionValue.List.create(mutableList)

        mutableList.clear()

        assertEquals(2, list.items.size)
        assertEquals(ActionValue.Text(secretValue), list.items[0])

        val str = list.toString()
        assertFalse(str.contains(secretValue))
    }

    @Test
    fun verifyActionParametersDefensiveCopyingAndBlankKeyRejection() {
        val secretValue = "SUPER_SECRET_OTP_9999"
        val mutableParams = mutableMapOf<String, ActionValue>(
            "otp" to ActionValue.Text(secretValue)
        )
        val params = ActionParameters.create(mutableParams)

        mutableParams["otp"] = ActionValue.Text("MUTATED")

        assertEquals(ActionValue.Text(secretValue), params["otp"])

        val str = params.toString()
        assertFalse(str.contains(secretValue))
        assertFalse(str.contains("otp"))
        assertEquals("ActionParameters(size=1)", str)

        assertFailsWith<IllegalArgumentException> {
            ActionParameters.create(mapOf("" to ActionValue.Text("val")))
        }
        assertFailsWith<IllegalArgumentException> {
            ActionParameters.create(mapOf("   " to ActionValue.Text("val")))
        }
    }

    @Test
    fun verifyDeepNestedCollectionImmutability() {
        val innerMutableList = mutableListOf<ActionValue>(ActionValue.Text("deep_initial"))
        val innerObject = ActionValue.Object.create(mapOf("listKey" to ActionValue.List.create(innerMutableList)))
        val outerMutableMap = mutableMapOf<String, ActionValue>("nestedObj" to innerObject)
        val params = ActionParameters.create(outerMutableMap)

        innerMutableList[0] = ActionValue.Text("DEEP_MUTATED")
        innerMutableList.add(ActionValue.Text("NEW_ELEMENT"))
        outerMutableMap["nestedObj"] = ActionValue.Text("OUTER_MUTATED")

        val retrievedObj = params["nestedObj"] as ActionValue.Object
        val retrievedList = retrievedObj.properties["listKey"] as ActionValue.List

        assertEquals(1, retrievedList.items.size)
        assertEquals(ActionValue.Text("deep_initial"), retrievedList.items[0])
    }
}
