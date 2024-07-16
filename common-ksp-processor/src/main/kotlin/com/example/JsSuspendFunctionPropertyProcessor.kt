package com.example

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName
import java.io.OutputStreamWriter

class JsSuspendFunctionPropertyProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Starting to process symbols with annotation: @JsExport and @JsSuspendFunctionPropertyExport")

        // Get all classes annotated with @JsExport
        val symbols = resolver.getSymbolsWithAnnotation("kotlin.js.JsExport")
        val invalidSymbols = symbols.filterNot { it.validate() }

        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            logger.info("Processing class: ${classDeclaration.qualifiedName?.asString()}")
            try {
                generateJsWrapperFunctionsIfNeeded(classDeclaration)
            } catch (e: Exception) {
                logger.error("Error processing class ${classDeclaration.qualifiedName?.asString()}: ${e.message}")
                e.printStackTrace()
            }
        }

        logger.info("Finished processing symbols with annotation: @JsExport and @JsSuspendFunctionPropertyExport")

        return invalidSymbols.toList()
    }

    private fun generateJsWrapperFunctionsIfNeeded(classDeclaration: KSClassDeclaration) {
        val classAnnotations = classDeclaration.annotations.map { it.shortName.asString() }
        if ("JsExport" in classAnnotations) {
            val packageName = classDeclaration.packageName.asString()
            val className = classDeclaration.simpleName.asString()
            val wrapperFileName = "${className}JSWrapper"

            logger.info("Generating JS wrapper functions for: $className")

            val fileSpecBuilder = FileSpec.builder(packageName, wrapperFileName)
                .addAnnotation(
                    AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                        .addMember("%T::class", ClassName("kotlin.js", "ExperimentalJsExport"))
                        .build()
                )
                .addImport("kotlin.js", "Promise")
                .addImport("kotlin.coroutines", "Continuation")
                .addImport("kotlin.coroutines", "EmptyCoroutineContext")
                .addImport("kotlin.coroutines", "startCoroutine")

            var shouldGenerateFile = false

            classDeclaration.getAllProperties().forEach { property ->
                logger.info("Processing property: ${property.simpleName.asString()}")
                property.annotations.filter { it.shortName.asString() == "JsSuspendFunctionPropertyExport" }
                    .forEach { annotation ->
                        val propertyName = property.simpleName.asString()
                        val returnType = property.type.resolve().arguments.first().type?.toTypeName()

                        logger.info("Generating wrapper function for property $propertyName in class $className")

                        val functionBuilder =
                            FunSpec.builder("${className.replaceFirstChar { it.lowercaseChar() }}${propertyName.replaceFirstChar { it.uppercaseChar() }}AsPromise")
                                .receiver(ClassName(packageName, className))
                                .returns(ClassName("kotlin.js", "Promise").parameterizedBy(returnType!!))
                                .addCode(
                                    """
                                return Promise { resolve, reject ->
                                    val completion = Continuation(EmptyCoroutineContext) {
                                        it.onSuccess(resolve).onFailure(reject)
                                    }
                                    this.$propertyName.startCoroutine(completion)
                                }
                                """.trimIndent()
                                )
                                .addAnnotation(ClassName("kotlin.js", "JsExport"))

                        fileSpecBuilder.addFunction(functionBuilder.build())
                        shouldGenerateFile = true
                    }
            }

            if (shouldGenerateFile) {
                val fileSpec = fileSpecBuilder.build()

                val file = codeGenerator.createNewFile(
                    Dependencies(false, classDeclaration.containingFile!!),
                    packageName,
                    wrapperFileName
                )

                OutputStreamWriter(file).use { writer ->
                    fileSpec.writeTo(writer)
                }

                logger.info("Generated JS wrapper functions for: $className")
            }
        }
    }
}

class JsSuspendFunctionPropertyProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return JsSuspendFunctionPropertyProcessor(environment.codeGenerator, environment.logger)
    }
}
