/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.*


/** This file generated by :compiler:fir:tree:generateVisitors. DO NOT MODIFY MANUALLY! */
abstract class FirTransformer<in D> : FirVisitor<CompositeTransformResult<FirElement>, D>() {
    abstract fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E>

    open fun transformDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(declaration, data)
    }

    open fun transformCallableMember(callableMember: FirCallableMember, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(callableMember, data)
    }

    open fun transformDeclarationWithBody(declarationWithBody: FirDeclarationWithBody, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(declarationWithBody, data)
    }

    open fun transformAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: D
    ): CompositeTransformResult<FirDeclaration> {
        return transformDeclarationWithBody(anonymousInitializer, data)
    }

    open fun transformFunction(function: FirFunction, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclarationWithBody(function, data)
    }

    open fun transformConstructor(constructor: FirConstructor, data: D): CompositeTransformResult<FirDeclaration> {
        return transformFunction(constructor, data)
    }

    open fun transformNamedFunction(namedFunction: FirNamedFunction, data: D): CompositeTransformResult<FirDeclaration> {
        return transformFunction(namedFunction, data)
    }

    open fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): CompositeTransformResult<FirDeclaration> {
        return transformFunction(propertyAccessor, data)
    }

    open fun transformErrorDeclaration(errorDeclaration: FirErrorDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(errorDeclaration, data)
    }

    open fun transformNamedDeclaration(namedDeclaration: FirNamedDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(namedDeclaration, data)
    }

    open fun transformMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformNamedDeclaration(memberDeclaration, data)
    }

    open fun transformClass(klass: FirClass, data: D): CompositeTransformResult<FirDeclaration> {
        return transformMemberDeclaration(klass, data)
    }

    open fun transformEnumEntry(enumEntry: FirEnumEntry, data: D): CompositeTransformResult<FirDeclaration> {
        return transformClass(enumEntry, data)
    }

    open fun transformTypeAlias(typeAlias: FirTypeAlias, data: D): CompositeTransformResult<FirDeclaration> {
        return transformMemberDeclaration(typeAlias, data)
    }

    open fun transformTypeParameter(typeParameter: FirTypeParameter, data: D): CompositeTransformResult<FirDeclaration> {
        return transformNamedDeclaration(typeParameter, data)
    }

    open fun transformProperty(property: FirProperty, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(property, data)
    }

    open fun transformTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(typedDeclaration, data)
    }

    open fun transformValueParameter(valueParameter: FirValueParameter, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(valueParameter, data)
    }

    open fun transformVariable(variable: FirVariable, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(variable, data)
    }

    open fun <E : FirElement> transformImport(import: E, data: D): CompositeTransformResult<E> {
        return transformElement(import, data)
    }

    open fun <E : FirElement> transformPackageFragment(packageFragment: E, data: D): CompositeTransformResult<E> {
        return transformElement(packageFragment, data)
    }

    open fun <E : FirPackageFragment> transformFile(file: E, data: D): CompositeTransformResult<E> {
        return transformPackageFragment(file, data)
    }

    open fun <E : FirElement> transformStatement(statement: E, data: D): CompositeTransformResult<E> {
        return transformElement(statement, data)
    }

    open fun <E : FirStatement> transformExpression(expression: E, data: D): CompositeTransformResult<E> {
        return transformStatement(expression, data)
    }

    open fun <E : FirExpression> transformBody(body: E, data: D): CompositeTransformResult<E> {
        return transformExpression(body, data)
    }

    open fun <E : FirExpression> transformCall(call: E, data: D): CompositeTransformResult<E> {
        return transformExpression(call, data)
    }

    open fun <E : FirCall> transformAnnotationCall(annotationCall: E, data: D): CompositeTransformResult<E> {
        return transformCall(annotationCall, data)
    }

    open fun <E : FirCall> transformConstructorCall(constructorCall: E, data: D): CompositeTransformResult<E> {
        return transformCall(constructorCall, data)
    }

    open fun <E : FirElement> transformType(type: E, data: D): CompositeTransformResult<E> {
        return transformElement(type, data)
    }

    open fun <E : FirType> transformDelegatedType(delegatedType: E, data: D): CompositeTransformResult<E> {
        return transformType(delegatedType, data)
    }

    open fun <E : FirType> transformErrorType(errorType: E, data: D): CompositeTransformResult<E> {
        return transformType(errorType, data)
    }

    open fun <E : FirType> transformImplicitType(implicitType: E, data: D): CompositeTransformResult<E> {
        return transformType(implicitType, data)
    }

    open fun <E : FirType> transformTypeWithNullability(typeWithNullability: E, data: D): CompositeTransformResult<E> {
        return transformType(typeWithNullability, data)
    }

    open fun <E : FirTypeWithNullability> transformDynamicType(dynamicType: E, data: D): CompositeTransformResult<E> {
        return transformTypeWithNullability(dynamicType, data)
    }

    open fun <E : FirTypeWithNullability> transformFunctionType(functionType: E, data: D): CompositeTransformResult<E> {
        return transformTypeWithNullability(functionType, data)
    }

    open fun <E : FirTypeWithNullability> transformResolvedType(resolvedType: E, data: D): CompositeTransformResult<E> {
        return transformTypeWithNullability(resolvedType, data)
    }

    open fun <E : FirTypeWithNullability> transformUserType(userType: E, data: D): CompositeTransformResult<E> {
        return transformTypeWithNullability(userType, data)
    }

    open fun <E : FirElement> transformTypeProjection(typeProjection: E, data: D): CompositeTransformResult<E> {
        return transformElement(typeProjection, data)
    }

    open fun <E : FirTypeProjection> transformStarProjection(starProjection: E, data: D): CompositeTransformResult<E> {
        return transformTypeProjection(starProjection, data)
    }

    open fun <E : FirTypeProjection> transformTypeProjectionWithVariance(
        typeProjectionWithVariance: E,
        data: D
    ): CompositeTransformResult<E> {
        return transformTypeProjection(typeProjectionWithVariance, data)
    }

    final override fun visitCall(call: FirCall, data: D): CompositeTransformResult<FirElement> {
        return transformCall(call, data)
    }

    final override fun visitClass(klass: FirClass, data: D): CompositeTransformResult<FirElement> {
        return transformClass(klass, data)
    }

    final override fun visitDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirElement> {
        return transformDeclaration(declaration, data)
    }

    final override fun visitDeclarationWithBody(
        declarationWithBody: FirDeclarationWithBody,
        data: D
    ): CompositeTransformResult<FirElement> {
        return transformDeclarationWithBody(declarationWithBody, data)
    }

    final override fun visitElement(element: FirElement, data: D): CompositeTransformResult<FirElement> {
        return transformElement(element, data)
    }

    final override fun visitExpression(expression: FirExpression, data: D): CompositeTransformResult<FirElement> {
        return transformExpression(expression, data)
    }

    final override fun visitFunction(function: FirFunction, data: D): CompositeTransformResult<FirElement> {
        return transformFunction(function, data)
    }

    final override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): CompositeTransformResult<FirElement> {
        return transformMemberDeclaration(memberDeclaration, data)
    }

    final override fun visitNamedDeclaration(namedDeclaration: FirNamedDeclaration, data: D): CompositeTransformResult<FirElement> {
        return transformNamedDeclaration(namedDeclaration, data)
    }

    final override fun visitPackageFragment(packageFragment: FirPackageFragment, data: D): CompositeTransformResult<FirElement> {
        return transformPackageFragment(packageFragment, data)
    }

    final override fun visitStatement(statement: FirStatement, data: D): CompositeTransformResult<FirElement> {
        return transformStatement(statement, data)
    }

    final override fun visitType(type: FirType, data: D): CompositeTransformResult<FirElement> {
        return transformType(type, data)
    }

    final override fun visitTypeProjection(typeProjection: FirTypeProjection, data: D): CompositeTransformResult<FirElement> {
        return transformTypeProjection(typeProjection, data)
    }

    final override fun visitTypeWithNullability(
        typeWithNullability: FirTypeWithNullability,
        data: D
    ): CompositeTransformResult<FirElement> {
        return transformTypeWithNullability(typeWithNullability, data)
    }

    final override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): CompositeTransformResult<FirElement> {
        return transformAnnotationCall(annotationCall, data)
    }

    final override fun visitConstructorCall(constructorCall: FirConstructorCall, data: D): CompositeTransformResult<FirElement> {
        return transformConstructorCall(constructorCall, data)
    }

    final override fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): CompositeTransformResult<FirElement> {
        return transformEnumEntry(enumEntry, data)
    }

    final override fun visitCallableMember(callableMember: FirCallableMember, data: D): CompositeTransformResult<FirElement> {
        return transformCallableMember(callableMember, data)
    }

    final override fun visitErrorDeclaration(errorDeclaration: FirErrorDeclaration, data: D): CompositeTransformResult<FirElement> {
        return transformErrorDeclaration(errorDeclaration, data)
    }

    final override fun visitProperty(property: FirProperty, data: D): CompositeTransformResult<FirElement> {
        return transformProperty(property, data)
    }

    final override fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): CompositeTransformResult<FirElement> {
        return transformTypedDeclaration(typedDeclaration, data)
    }

    final override fun visitValueParameter(valueParameter: FirValueParameter, data: D): CompositeTransformResult<FirElement> {
        return transformValueParameter(valueParameter, data)
    }

    final override fun visitVariable(variable: FirVariable, data: D): CompositeTransformResult<FirElement> {
        return transformVariable(variable, data)
    }

    final override fun visitAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        data: D
    ): CompositeTransformResult<FirElement> {
        return transformAnonymousInitializer(anonymousInitializer, data)
    }

    final override fun visitImport(import: FirImport, data: D): CompositeTransformResult<FirElement> {
        return transformImport(import, data)
    }

    final override fun visitBody(body: FirBody, data: D): CompositeTransformResult<FirElement> {
        return transformBody(body, data)
    }

    final override fun visitConstructor(constructor: FirConstructor, data: D): CompositeTransformResult<FirElement> {
        return transformConstructor(constructor, data)
    }

    final override fun visitNamedFunction(namedFunction: FirNamedFunction, data: D): CompositeTransformResult<FirElement> {
        return transformNamedFunction(namedFunction, data)
    }

    final override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): CompositeTransformResult<FirElement> {
        return transformPropertyAccessor(propertyAccessor, data)
    }

    final override fun visitTypeAlias(typeAlias: FirTypeAlias, data: D): CompositeTransformResult<FirElement> {
        return transformTypeAlias(typeAlias, data)
    }

    final override fun visitTypeParameter(typeParameter: FirTypeParameter, data: D): CompositeTransformResult<FirElement> {
        return transformTypeParameter(typeParameter, data)
    }

    final override fun visitFile(file: FirFile, data: D): CompositeTransformResult<FirElement> {
        return transformFile(file, data)
    }

    final override fun visitDelegatedType(delegatedType: FirDelegatedType, data: D): CompositeTransformResult<FirElement> {
        return transformDelegatedType(delegatedType, data)
    }

    final override fun visitErrorType(errorType: FirErrorType, data: D): CompositeTransformResult<FirElement> {
        return transformErrorType(errorType, data)
    }

    final override fun visitImplicitType(implicitType: FirImplicitType, data: D): CompositeTransformResult<FirElement> {
        return transformImplicitType(implicitType, data)
    }

    final override fun visitStarProjection(starProjection: FirStarProjection, data: D): CompositeTransformResult<FirElement> {
        return transformStarProjection(starProjection, data)
    }

    final override fun visitTypeProjectionWithVariance(
        typeProjectionWithVariance: FirTypeProjectionWithVariance,
        data: D
    ): CompositeTransformResult<FirElement> {
        return transformTypeProjectionWithVariance(typeProjectionWithVariance, data)
    }

    final override fun visitDynamicType(dynamicType: FirDynamicType, data: D): CompositeTransformResult<FirElement> {
        return transformDynamicType(dynamicType, data)
    }

    final override fun visitFunctionType(functionType: FirFunctionType, data: D): CompositeTransformResult<FirElement> {
        return transformFunctionType(functionType, data)
    }

    final override fun visitResolvedType(resolvedType: FirResolvedType, data: D): CompositeTransformResult<FirElement> {
        return transformResolvedType(resolvedType, data)
    }

    final override fun visitUserType(userType: FirUserType, data: D): CompositeTransformResult<FirElement> {
        return transformUserType(userType, data)
    }

}
