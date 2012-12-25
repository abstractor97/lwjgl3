/* 
 * Copyright LWJGL. All rights reserved.
 * License terms: http://lwjgl.org/license.php
 */
package org.lwjgl.generator

import java.io.PrintWriter
import java.util.ArrayList

abstract class ConstantType<T>(
	val javaType: Class<T>
) {
	abstract fun print(value: T): String
	abstract fun nullValue(): T
}

val IntConstant = object: ConstantType<Int>(javaClass<Int>()) {
	override fun print(value: Int): String = "0x" + Integer.toHexString(value)!!.toUpperCase()
	override fun nullValue(): Int = 0
}

val LongConstant = object: ConstantType<Long>(javaClass<Long>()) {
	override fun print(value: Long): String = "0x" + java.lang.Long.toHexString(value)!!.toUpperCase() + "L"
	override fun nullValue(): Long = 0.toLong()
}

// Extension property for long literals. This is needed due to how Kotlin handles negative literals atm.
val Int.L: Long
	get() = this.toLong()

public class ConstantBlock<T>(
	val nativeClass: NativeClass,
	val constantType: ConstantType<T>,
	val documentation: String,
	vararg val constants: Constant<T>
) {

	private var noPrefix = false

	public fun noPrefix(): Unit = noPrefix = true

	private fun getConstantName(name: String) = if ( noPrefix || nativeClass.prefix.isEmpty() ) name else "${nativeClass.prefix}_$name"

	fun generate(writer: PrintWriter) {
		writer.generateBlock()
	}

	private fun PrintWriter.generateBlock() {
		println(documentation)

		println("\tpublic static final ${constantType.javaType.getSimpleName()}")

		// Find maximum constant name length
		val alignment = constants.map {
			it.name.size
		}.fold(0) {
			(left, right) -> Math.max(left, right)
		}

		for ( i in 0..constants.lastIndex - 1 ) {
			printConstant(constants[i], alignment)
			println(',')
		}
		printConstant(constants[constants.lastIndex], alignment)
		println(";\n")
	}

	private fun PrintWriter.printConstant(constant: Constant<T>, alignment: Int) {
		val (name, value) = constant

		print("\t\t${getConstantName(name)}")
		for ( i in 0..(alignment - name.size - 1) )
			print(' ')

		print(" = ")
		if ( constant is ConstantExpression<T> )
			print(constant.expression)
		else
			print(constantType.print(value!!))
	}

	public fun toJavaDocLinks(): String {
		val builder = StringBuilder()

		builder append '#'
		builder append getConstantName(constants[0].name)
		for ( i in 1..constants.lastIndex ) {
			builder append " #"
			builder append getConstantName(constants[i].name)
		}

		return builder.toString()
	}

}

public open data class Constant<T: Any>(val name: String, val value: T?)
public class ConstantExpression<T>(name: String, val expression: String): Constant<T>(name, null)

/** Adds a new constant. */
public fun <T> String._(value: T): Constant<T> =
	Constant<T>(this, value)

/** Adds a new constant whose value is a function of previously defined constants. */
public fun <T> String.expr(expression: String): ConstantExpression<T> =
	ConstantExpression<T>(this, expression)
