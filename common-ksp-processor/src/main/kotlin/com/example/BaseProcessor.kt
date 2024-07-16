package com.example

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toTypeName
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.StringWriter
import kotlin.reflect.KClass

abstract class BaseProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Starting to process symbols with annotation: ${getAnnotation().qualifiedName}")

        val symbols = resolver.getSymbolsWithAnnotation(getAnnotation().qualifiedName!!)

        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            logger.info("Processing class: ${classDeclaration.qualifiedName?.asString()}")
            try {
                generateJsWrapperClass(classDeclaration)
            } catch (e: Exception) {
                logger.error("Error processing class ${classDeclaration.qualifiedName?.asString()}: ${e.message}")
                e.printStackTrace()
            }
        }

        logger.info("Finished processing symbols with annotation: ${getAnnotation().qualifiedName}")

        return emptyList()
    }

    abstract fun getAnnotation(): KClass<out Annotation>

    protected open fun generateJsWrapperClass(classDeclaration: KSClassDeclaration) {
        val packageName = classDeclaration.packageName.asString()
        val className = classDeclaration.simpleName.asString()
        val wrapperClassName = "${className}JsWrapper"

        logger.info("Generating JS wrapper class for: $className")

        val classBuilder = createJsWrapperClassBuilder(packageName, className, classDeclaration)

        val typeSpec = classBuilder.build()
        logger.info("TypeSpec for $wrapperClassName:\n$typeSpec")

        val fileSpec = FileSpec.builder(packageName, wrapperClassName)
            .addType(typeSpec)
            .addImport("kotlin.js", "Promise")
            .addImport("kotlinx.coroutines", "SupervisorJob")
            .addImport("kotlinx.coroutines", "CoroutineScope")
            .addImport("kotlinx.coroutines", "Dispatchers")
            .addImport("kotlinx.coroutines", "launch")
            .build()
        logger.info("FileSpec for $wrapperClassName:\n$fileSpec")

        val stringWriter = StringWriter()
        try {
            fileSpec.writeTo(stringWriter)
        } catch (e: Exception) {
            logger.error("Error writing FileSpec to StringWriter for $wrapperClassName: ${e.message}")
            e.printStackTrace()
            return
        }
        val fileContent = stringWriter.toString()

        logger.info("Generated content for $wrapperClassName:\n$fileContent")

        val file = codeGenerator.createNewFile(
            Dependencies(false, classDeclaration.containingFile!!),
            packageName,
            wrapperClassName
        )

        try {
            file.bufferedWriter().use { writer ->
                writer.write(fileContent)
            }
        } catch (e: Exception) {
            logger.error("Error writing FileSpec for $wrapperClassName: ${e.message}")
            e.printStackTrace()
        }

        logger.info("Generated JS wrapper class: $wrapperClassName")
    }

    private fun createJsWrapperClassBuilder(
        packageName: String,
        className: String,
        classDeclaration: KSClassDeclaration
    ): TypeSpec.Builder {
        val classBuilder = TypeSpec.classBuilder("${className}JsWrapper")
            .addAnnotation(AnnotationSpec.builder(ClassName("kotlin.js", "JsExport")).build())
            .addAnnotation(
                AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    .addMember("%T::class", ClassName("kotlin.js", "ExperimentalJsExport"))
                    .addMember("%T::class", ClassName("kotlinx.coroutines", "ExperimentalCoroutinesApi"))
                    .build()
            )
            .primaryConstructor(createPrimaryConstructor(classDeclaration))
            .addProperty(
                PropertySpec.builder("instance", ClassName(packageName, className))
                    .addModifiers(KModifier.PRIVATE)
                    .initializer(createInstanceInitializer(classDeclaration))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("job", ClassName("kotlinx.coroutines", "Job"))
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("%T()", ClassName("kotlinx.coroutines", "SupervisorJob"))
                    .build()
            )
            .addProperty(
                PropertySpec.builder("scope", ClassName("kotlinx.coroutines", "CoroutineScope"))
                    .addModifiers(KModifier.PRIVATE)
                    .initializer("CoroutineScope(Dispatchers.Default + job)")
                    .build()
            )

        logger.info("Adding functions and properties to wrapper class: ${className}JsWrapper")

        classDeclaration.getAllFunctions()
            .filterNot { it.simpleName.asString() == "<init>" }
            .filterNot { it.simpleName.asString() in listOf("equals", "hashCode", "toString") }
            .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
            .forEach { function ->
                logger.info("Processing function: ${function.simpleName.asString()}")
                if (function.isSuspendFunction()) {
                    classBuilder.addFunction(generateSuspendFunctionWrapper(function))
                } else if (function.isFlowTypeFunction()) {
                    classBuilder.addFunction(
                        generateFlowWrapper(
                            function.simpleName.asString(),
                            function.returnType,
                            function.getDocString()
                        )
                    )
                } else {
                    classBuilder.addFunction(generateNormalFunctionWrapper(function))
                }
            }

        classDeclaration.getAllProperties()
            .filter { it.type.isFlowType() }
            .filterNot { it.modifiers.contains(Modifier.PRIVATE) }
            .forEach { property ->
                logger.info("Processing property: ${property.simpleName.asString()}")
                classBuilder.addFunction(
                    generateFlowWrapper(
                        property.simpleName.asString(),
                        property.type,
                        property.getDocString()
                    )
                )
            }

        classBuilder.addFunction(generateClearFunction())

        return classBuilder
    }

    private fun createPrimaryConstructor(classDeclaration: KSClassDeclaration): FunSpec {
        val constructorBuilder = FunSpec.constructorBuilder()

        classDeclaration.primaryConstructor?.parameters
            ?.filterNot { it.hasDefault }
            ?.forEach { parameter ->
                constructorBuilder.addParameter(parameter.name?.asString() ?: "", parameter.type.toTypeName())
            }

        return constructorBuilder.build()
    }

    private fun generateSuspendFunctionWrapper(function: KSFunctionDeclaration): FunSpec {
        val functionName = function.simpleName.asString()
        return FunSpec.builder(functionName)
            .addKdoc(function.getDocString().orEmpty())
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "NON_EXPORTABLE_TYPE")
                    .build()
            )
            .returns(ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin", "Unit")))
            .addCode(
                """
                return Promise { resolve, reject ->
                    scope.launch {
                        try {
                            instance.$functionName()
                            resolve(Unit)
                        } catch (exception: Throwable) {
                            reject(exception)
                        }
                    }
                }
                """.trimIndent()
            )
            .build()
    }

    private fun generateFlowWrapper(name: String, returnType: KSTypeReference?, docString: String?): FunSpec {
        val resolvedType = returnType?.resolve()
        val flowType = resolvedType?.arguments?.firstOrNull()?.type?.resolve()
        val type = flowType?.toTypeName() ?: ClassName("kotlin", "Any")

        val functionWrapperName = "get${name.replaceFirstChar { it.uppercase() }}"
        return FunSpec.builder(functionWrapperName)
            .addKdoc(docString.orEmpty())
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "NON_EXPORTABLE_TYPE")
                    .build()
            )
            .returns(ClassName("kotlin.js", "Promise").parameterizedBy(ClassName("kotlin", "Unit")))
            .addParameter(
                "success",
                LambdaTypeName.get(
                    null,
                    listOf(ParameterSpec.unnamed(type.copy(nullable = true))),
                    Unit::class.asClassName()
                )
            )
            .addParameter(
                "error",
                LambdaTypeName.get(
                    null,
                    listOf(ParameterSpec.unnamed(ClassName("kotlin", "Throwable"))),
                    Unit::class.asClassName()
                )
            )
            .addCode(
                """
                return Promise { _, reject ->
                    scope.launch {
                        try {
                            instance.$name.collect { value ->
                                success(value)
                            }
                        } catch (exception: Throwable) {
                            error(exception)
                            reject(exception)
                        }
                    }
                }
                """.trimIndent()
            )
            .build()
    }

    private fun generateNormalFunctionWrapper(function: KSFunctionDeclaration): FunSpec {
        val functionName = function.simpleName.asString()
        return FunSpec.builder(functionName)
            .addKdoc(function.getDocString().orEmpty())
            .apply {
                function.parameters.forEach { parameter ->
                    addParameter(
                        parameter.name?.asString() ?: "",
                        parameter.type.toTypeName()
                    )
                }
            }
            .addCode(
                """
                instance.${functionName}(${function.parameters.joinToString { it.name?.asString() ?: "" }})
                """.trimIndent()
            )
            .build()
    }

    private fun generateClearFunction(): FunSpec {
        return FunSpec.builder("clear")
            .addCode("job.cancel()")
            .build()
    }

    private fun createInstanceInitializer(classDeclaration: KSClassDeclaration): CodeBlock {
        val constructorParams = classDeclaration.primaryConstructor?.parameters
            ?.filterNot { it.hasDefault }
            ?.joinToString(", ") { "${it.name?.asString()} = ${it.name?.asString()}" } ?: ""

        return CodeBlock.of("%T($constructorParams)", classDeclaration.toClassName())
    }

    private fun KSFunctionDeclaration.isSuspendFunction(): Boolean {
        return this.modifiers.contains(Modifier.SUSPEND)
    }

    private fun KSFunctionDeclaration.isFlowTypeFunction(): Boolean {
        return this.returnType?.isFlowType() ?: false
    }

    private fun KSTypeReference?.isFlowType(): Boolean {
        if (this == null) return false
        val flowTypeNames = setOf(
            StateFlow::class.qualifiedName,
            SharedFlow::class.qualifiedName,
            MutableStateFlow::class.qualifiedName,
            MutableSharedFlow::class.qualifiedName
        )
        return this.resolve().declaration.qualifiedName?.asString() in flowTypeNames
    }

    private fun KSDeclaration.getDocString(): String? {
        return this.docString?.trimIndent()
    }

    private fun KSClassDeclaration.toClassName(): ClassName {
        val pkg = packageName.asString()
        val simpleName = simpleName.asString()
        return ClassName(pkg, simpleName)
    }
}
