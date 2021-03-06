/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

data class CallableReferenceKey(
    val declaration: IrFunction,
    val hasDispatchReference: Boolean,
    val hasExtensionReceiver: Boolean
)

// TODO: generate $metadata$ property and fill it with corresponding KFunction/KProperty interface
class CallableReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val callableNameConst = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KCALLABLE_NAME)
    private val getterConst = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KPROPERTY_GET)
    private val setterConst = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KPROPERTY_SET)
    private val callableToGetterFunction = context.callableReferencesCache

    private val newDeclarations = mutableListOf<IrDeclaration>()
    private val implicitDeclarationFile = context.implicitDeclarationFile

    override fun lower(irFile: IrFile) {
        newDeclarations.clear()
        irFile.transformChildrenVoid(CallableReferenceLowerTransformer())
        implicitDeclarationFile.declarations += newDeclarations
    }

    private fun makeCallableKey(declaration: IrFunction, reference: IrCallableReference) =
        CallableReferenceKey(declaration, reference.dispatchReceiver != null, reference.extensionReceiver != null)

    inner class CallableReferenceLowerTransformer : IrElementTransformerVoid() {
        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            expression.transformChildrenVoid(this)
            val declaration = expression.symbol.owner
            if (declaration.origin == JsIrBackendContext.callableClosureOrigin) return expression
            val key = makeCallableKey(declaration, expression)
            val factory = callableToGetterFunction.getOrPut(key) { lowerKFunctionReference(declaration, expression) }
            return redirectToFunction(expression, factory)
        }

        override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)
            val declaration = expression.getter!!.owner
            val key = makeCallableKey(declaration, expression)
            val factory = callableToGetterFunction.getOrPut(key) { lowerKPropertyReference(declaration, expression) }
            return redirectToFunction(expression, factory)
        }

        override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)
            val key = makeCallableKey(expression.getter.owner, expression)
            val factory = callableToGetterFunction.getOrPut(key) { lowerLocalKPropertyReference(expression) }
            return redirectToFunction(expression, factory)
        }
    }

    private fun redirectToFunction(callable: IrCallableReference, newTarget: IrFunction) =
        IrCallImpl(
            callable.startOffset, callable.endOffset,
            newTarget.symbol.owner.returnType,
            newTarget.symbol,
            newTarget.symbol.descriptor,
            callable.origin
        ).apply {
            copyTypeArgumentsFrom(callable)
            var index = 0
            callable.dispatchReceiver?.let { putValueArgument(index++, it) }
            callable.extensionReceiver?.let { putValueArgument(index++, it) }
            for (i in 0 until callable.valueArgumentsCount) {
                val arg = callable.getValueArgument(i)
                if (arg != null) {
                    putValueArgument(index++, arg)
                }
            }
        }

    private fun createFunctionClosureGetterName(declaration: IrDeclaration) = createHelperFunctionName(declaration, "KReferenceGet")
    private fun createPropertyClosureGetterName(declaration: IrDeclaration) = createHelperFunctionName(declaration, "KPropertyGet")
    private fun createClosureInstanceName(declaration: IrDeclaration) = createHelperFunctionName(declaration, "KReferenceClosure")

    private fun createHelperFunctionName(declaration: IrDeclaration, suffix: String): String {
        val nameBuilder = StringBuilder()
        if (declaration is IrConstructor) {
            val klass = declaration.parent as IrClass
            nameBuilder.append(klass.name.asString())
            nameBuilder.append('_')
        }

        when (declaration) {
            is IrFunction -> nameBuilder.append(declaration.name)
            is IrProperty -> nameBuilder.append(declaration.name)
            is IrVariable -> nameBuilder.append(declaration.name)
            else -> TODO("Unexpected declaration type")
        }

        nameBuilder.append('_')
        nameBuilder.append(suffix)
        return nameBuilder.toString()
    }


    private fun getReferenceName(declaration: IrDeclaration) = when (declaration) {
        is IrConstructor -> (declaration.parent as IrClass).name.identifier
        is IrProperty -> declaration.name.identifier
        is IrSimpleFunction -> declaration.name.asString()
        is IrVariable -> declaration.name.asString().replace("\$delegate", "")
        else -> TODO("Unexpected declaration type")
    }

    private fun lowerKFunctionReference(declaration: IrFunction, functionReference: IrFunctionReference ): IrSimpleFunction {
        // transform
        // x = Foo::bar ->
        // x = Foo_bar_KreferenceGet(c1: closure$C1, c2: closure$C2) : KFunctionN<Foo, T2, ..., TN, TReturn> {
        //   [ if ($cache$ == null) { ] // in case reference has no closure param cache it
        //     val x = fun Foo_bar_KreferenceClosure(p0: Foo, p1: T2, p2: T3): TReturn {
        //        return p0.bar(c1, c2, p1, p2)
        //     }
        //     x.callableName = "bar"
        //   [ $cache$ = x } ]
        //   return {$cache$|x}
        // }

        // KFunctionN<Foo, T2, ..., TN, TReturn>, arguments.size = N + 1

        val refGetFunction = buildGetFunction(declaration, functionReference, createFunctionClosureGetterName(declaration))
        val refClosureFunction = buildClosureFunction(declaration, refGetFunction, functionReference)

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val irClosureReference = JsIrBuilder.buildFunctionReference(functionReference.type, refClosureFunction.symbol)

            val irVar = JsIrBuilder.buildVar(irClosureReference.type, refGetFunction, initializer = irClosureReference)

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            val irSetName = JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.stringType, getReferenceName(declaration)))
            }
            Pair(listOf(refClosureFunction, irVar, irSetName), irVar.symbol)
        }

        newDeclarations += additionalDeclarations + refGetFunction

        return refGetFunction
    }

    private fun lowerKPropertyReference(getterDeclaration: IrSimpleFunction, propertyReference: IrPropertyReference): IrSimpleFunction {
        // transform
        // x = Foo::bar ->
        // x = Foo_bar_KreferenceGet() : KPropertyN<Foo, PType> {
        //   if ($cache$ == null) { // very likely property reference is going to be cached
        //     val x = fun Foo_bar_KreferenceClosure_get(r: Foo): PType {
        //        return r.<get>()
        //     }
        //     x.get = x
        //     x.callableName = "bar"
        //     if (mutable) {
        //       x.set = fun Foo_bar_KreferenceClosure_set(r: Foo, v: PType>) {
        //         r.<set>(v)
        //       }
        //     }
        //     $cache$ = x
        //   }
        //   return $cache$
        // }

        val getterName = createPropertyClosureGetterName(getterDeclaration.correspondingProperty!!)
        val refGetFunction = buildGetFunction(propertyReference.getter!!.owner, propertyReference, getterName)

        val getterFunction = propertyReference.getter?.let { buildClosureFunction(it.owner, refGetFunction, propertyReference) }!!
        val setterFunction = propertyReference.setter?.let { buildClosureFunction(it.owner, refGetFunction, propertyReference) }

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val statements = mutableListOf<IrStatement>(getterFunction)

            val getterFunctionType = context.builtIns.getFunction(getterFunction.valueParameters.size + 1)
            val type = getterFunctionType.toIrType(symbolTable = context.symbolTable)
            val irGetReference = JsIrBuilder.buildFunctionReference(type, getterFunction.symbol)
            val irVar = JsIrBuilder.buildVar(type, refGetFunction, initializer = irGetReference)

            statements += irVar

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                putValueArgument(1, getterConst)
                putValueArgument(2, JsIrBuilder.buildGetValue(irVar.symbol))
            }

            if (setterFunction != null) {
                statements += setterFunction
                val setterFunctionType = context.builtIns.getFunction(setterFunction.valueParameters.size + 1)
                val irSetReference = JsIrBuilder.buildFunctionReference(
                    setterFunctionType.toIrType(symbolTable = context.symbolTable),
                    setterFunction.symbol
                )
                statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                    putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                    putValueArgument(1, setterConst)
                    putValueArgument(2, irSetReference)
                }
            }

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(
                    2,
                    JsIrBuilder.buildString(
                        context.irBuiltIns.stringType,
                        getReferenceName(getterDeclaration.correspondingProperty!!)
                    )
                )
            }

            Pair(statements, irVar.symbol)
        }

        newDeclarations += additionalDeclarations + refGetFunction

        return refGetFunction
    }

    private fun lowerLocalKPropertyReference(propertyReference: IrLocalDelegatedPropertyReference): IrSimpleFunction {
        // transform
        // ::bar ->
        // Foo_bar_KreferenceGet() : KPropertyN<Foo, PType> {
        //   if ($cache$ == null) {
        //     val x = fun Foo_bar_KreferenceClosure_get(): PType {
        //        throw IllegalStateException()
        //     }
        //     x.get = x
        //     x.callableName = "bar"
        //     $cache$ = x
        //   }
        //   return $cache$
        // }

        val declaration = propertyReference.delegate.owner
        val getterName = createPropertyClosureGetterName(declaration)
        val refGetFunction = buildGetFunction(propertyReference.getter.owner, propertyReference, getterName)
        val getterFunction = buildClosureFunction(context.irBuiltIns.throwIseFun, refGetFunction, propertyReference)

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val statements = mutableListOf<IrStatement>(getterFunction)

            val getterFunctionType = context.builtIns.getFunction(getterFunction.valueParameters.size + 1)
            val type = getterFunctionType.toIrType(symbolTable = context.symbolTable)
            val irGetReference = JsIrBuilder.buildFunctionReference(type, getterFunction.symbol)
            val irVar = JsIrBuilder.buildVar(type, refGetFunction, initializer = irGetReference)
            val irVarSymbol = irVar.symbol

            statements += irVar

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, getterConst)
                putValueArgument(2, JsIrBuilder.buildGetValue(irVarSymbol))
            }

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.stringType, getReferenceName(declaration)))
            }

            Pair(statements, irVarSymbol)
        }

        newDeclarations += additionalDeclarations + refGetFunction

        return refGetFunction
    }

    private fun generateGetterBodyWithGuard(
        getterFunction: IrSimpleFunction,
        builder: () -> Pair<List<IrStatement>, IrValueSymbol>
    ): List<IrDeclaration> {

        val (bodyStatements, varSymbol) = builder()
        val statements = mutableListOf<IrStatement>()
        val returnValue: IrExpression
        val returnStatements: List<IrDeclaration>
        if (getterFunction.valueParameters.isEmpty()) {
            // compose cache for 'direct' closure
            // if ($cache$ === null) {
            //   $cache$ = <body>
            // }
            //
            val cacheName = "${getterFunction.name}_${Namer.KCALLABLE_CACHE_SUFFIX}"
            val type = getterFunction.returnType
            val irNull = JsIrBuilder.buildNull(context.irBuiltIns.nothingNType)
            val cacheVar = JsIrBuilder.buildVar(type, getterFunction.parent, cacheName, true, initializer = irNull)

            val irCacheValue = JsIrBuilder.buildGetValue(cacheVar.symbol)
            val irIfCondition = JsIrBuilder.buildCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irCacheValue)
                putValueArgument(1, irNull)
            }
            val irSetCache =
                JsIrBuilder.buildSetVariable(cacheVar.symbol, JsIrBuilder.buildGetValue(varSymbol), context.irBuiltIns.unitType)
            val thenStatements = mutableListOf<IrStatement>().apply {
                addAll(bodyStatements)
                add(irSetCache)
            }
            val irThenBranch = JsIrBuilder.buildBlock(context.irBuiltIns.unitType, thenStatements)
            val irIfNode = JsIrBuilder.buildIfElse(context.irBuiltIns.unitType, irIfCondition, irThenBranch)
            statements += irIfNode
            returnValue = irCacheValue
            returnStatements = listOf(cacheVar)
        } else {
            statements += bodyStatements
            returnValue = JsIrBuilder.buildGetValue(varSymbol)
            returnStatements = emptyList()
        }

        statements += JsIrBuilder.buildReturn(getterFunction.symbol, returnValue, context.irBuiltIns.nothingType)

        getterFunction.body = JsIrBuilder.buildBlockBody(statements)
        return returnStatements
    }

    private fun generateSignatureForClosure(
        callable: IrFunction,
        getter: IrFunction,
        closure: IrSimpleFunction,
        reference: IrCallableReference
    ): List<IrValueParameter> {
        val result = mutableListOf<IrValueParameter>()

        /*
         * class D {
         *   fun foo(a, b, c) {} <= callable signature
         * }
         *
         * val d = D()
         * val reference = d::foo <= captures dispatch receiver
         *
         * is translated into
         *
         * function foo_get(d) { <= getter signature(d)
         *   return function (a, b, c) { <= result signature (a, b, c)
         *     return d.foo(a, b, c)
         *   }
         * }
         */
        var capturedParams = getter.valueParameters.size

        callable.dispatchReceiverParameter?.let { dispatch ->
            if (reference.dispatchReceiver == null) {
                result.add(JsIrBuilder.buildValueParameter(dispatch.name, result.size, dispatch.type).also { it.parent = closure })
            } else {
                // do not consider implicit receiver in result signature if it was captured
                capturedParams--
            }
        }

        callable.extensionReceiverParameter?.let { ext ->
            if (reference.extensionReceiver == null) {
                result.add(JsIrBuilder.buildValueParameter(ext.name, result.size, ext.type).also { it.parent = closure })
            } else {
                // the same as for dispatch
                capturedParams--
            }
        }

        // pass through value parameters
        for (i in capturedParams until callable.valueParameters.size) {
            val param = callable.valueParameters[i]
            val paramName = param.name.run { if (!isSpecial) identifier else "p$i" }
            result += JsIrBuilder.buildValueParameter(paramName, result.size, param.type).also { it.parent = closure }
        }

        return result
    }

    private fun buildGetFunction(declaration: IrFunction, reference: IrCallableReference, getterName: String): IrSimpleFunction {

        val callableType = reference.type
        var kFunctionValueParamsCount = (callableType as? IrSimpleType)?.arguments?.size?.minus(1) ?: 0

        if (declaration.dispatchReceiverParameter != null && reference.dispatchReceiver == null) {
            kFunctionValueParamsCount--
        }

        if (declaration.extensionReceiverParameter != null && reference.extensionReceiver == null) {
            kFunctionValueParamsCount--
        }

        assert(kFunctionValueParamsCount >= 0)

        // The `getter` function takes only closure parameters
        val receivers = mutableListOf<IrValueParameter>()
        if (reference.dispatchReceiver != null) {
            if (declaration.dispatchReceiverParameter != null) {
                receivers += declaration.dispatchReceiverParameter!!
            }
        }
        if (reference.extensionReceiver != null) {
            if (declaration.extensionReceiverParameter != null) {
                receivers += declaration.extensionReceiverParameter!!
            }
        }

        val getterValueParameters = receivers + declaration.valueParameters.dropLast(kFunctionValueParamsCount)


        val refGetDeclaration = JsIrBuilder.buildFunction(getterName, declaration.visibility)

        refGetDeclaration.parent = implicitDeclarationFile
        refGetDeclaration.returnType = callableType

        for ((i, p) in getterValueParameters.withIndex()) {
            val descriptor = WrappedValueParameterDescriptor()
            refGetDeclaration.valueParameters += IrValueParameterImpl(
                p.startOffset,
                p.endOffset,
                p.origin,
                IrValueParameterSymbolImpl(descriptor),
                p.name,
                i,
                p.type,
                p.varargElementType,
                p.isCrossinline,
                p.isNoinline
            ).also {
                descriptor.bind(it)
                it.parent = refGetDeclaration
            }
        }

        val typeParameters =
            if (declaration is IrConstructor) (declaration.parent as IrClass).typeParameters else declaration.typeParameters

        for (t in typeParameters) {
            val descriptor = WrappedTypeParameterDescriptor()
            refGetDeclaration.typeParameters += IrTypeParameterImpl(
                t.startOffset,
                t.endOffset,
                t.origin,
                IrTypeParameterSymbolImpl(descriptor),
                t.name,
                t.index,
                t.isReified,
                t.variance
            ).also {
                descriptor.bind(it)
                it.parent = refGetDeclaration
            }
        }

        return refGetDeclaration
    }

    private fun buildClosureFunction(
        declaration: IrFunction,
        refGetFunction: IrSimpleFunction,
        reference: IrCallableReference
    ): IrFunction {
        val closureName = createClosureInstanceName(declaration)
        val refClosureDeclaration =
            JsIrBuilder.buildFunction(closureName, Visibilities.LOCAL, origin = JsIrBackendContext.callableClosureOrigin)
                .also { it.parent = refGetFunction }

        // the params which are passed to closure
        val closureParamDeclarations = generateSignatureForClosure(declaration, refGetFunction, refClosureDeclaration, reference)
        val closureParamSymbols = closureParamDeclarations.map { it.symbol }
        val returnType = declaration.returnType

        refClosureDeclaration.valueParameters += closureParamDeclarations
        refClosureDeclaration.returnType = returnType

        val irCall = JsIrBuilder.buildCall(declaration.symbol, type = returnType)

        var cp = 0
        var gp = 0

        if (declaration.dispatchReceiverParameter != null) {
            val dispatchReceiverDeclaration =
                if (reference.dispatchReceiver != null) refGetFunction.valueParameters[gp++].symbol else closureParamSymbols[cp++]
            irCall.dispatchReceiver = JsIrBuilder.buildGetValue(dispatchReceiverDeclaration)
        }

        if (declaration.extensionReceiverParameter != null) {
            val extensionReceiverDeclaration =
                if (reference.extensionReceiver != null) refGetFunction.valueParameters[gp++].symbol else closureParamSymbols[cp++]
            irCall.extensionReceiver = JsIrBuilder.buildGetValue(extensionReceiverDeclaration)
        }

        var j = 0

        for (i in gp until refGetFunction.valueParameters.size) {
            irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(refGetFunction.valueParameters[i].symbol))
        }

        for (i in cp until closureParamSymbols.size) {
            irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(closureParamSymbols[i]))
        }

        val irClosureReturn = JsIrBuilder.buildReturn(refClosureDeclaration.symbol, irCall, context.irBuiltIns.nothingType)

        refClosureDeclaration.body = JsIrBuilder.buildBlockBody(listOf(irClosureReturn))

        return refClosureDeclaration
    }
}