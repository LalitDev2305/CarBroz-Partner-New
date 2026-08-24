package com.carbroz.runtime.form

import com.carbroz.runtime.binding.BindingValueSource

/** Reads live values from the store when binding resolution occurs. */
fun FormStore.asBindingValueSource(): BindingValueSource = BindingValueSource { path ->
    if (path.size != 1) return@BindingValueSource null
    state.value[FormFieldId(path.single())]?.value
}
