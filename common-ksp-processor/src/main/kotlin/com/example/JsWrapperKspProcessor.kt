package com.example

import com.google.devtools.ksp.processing.*
import com.wbd.beam.kmp.annotations.JsWrapperExport
import kotlin.reflect.KClass

class JsWrapperKspProcessor(
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) : BaseProcessor(codeGenerator, logger) {

    override fun getAnnotation(): KClass<out Annotation> {
        return JsWrapperExport::class
    }
}

class JsWrapperKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return JsWrapperKspProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
