# Kotlin Multiplatform JavaScript Export

This project demonstrates how to handle the export of Suspend functions and Flows to JavaScript in a Kotlin Multiplatform project. Since suspend functions and Flows are not natively exported to JavaScript, a Kotlin Symbol Processing (KSP) compiler plugin has been written to generate wrapper classes for these functions.

## Overview

The attached project showcases two types of exports:

### Class with Flow and Suspend Functions:

1. Creates a wrapper for the entire class that includes Flow and Suspend functions. Supported by adding annotation @JsWrapperExport
    - a. Only public functions and member variables are wrapped.
    - b. A Job/Scope is created and a clear function is provided for managing coroutine scopes from JavaScript.
    - c. Flow and Suspend functions are wrapped and exposed as Promises in JavaScript.

### Data Class with Suspend Function Variables:

2. Demonstrates how to handle data classes that include suspend function variables. Supported by adding annotation @JsSuspendFunctionPropertyExport

Example generated output for the above can be found: [here](https://github.com/DarshanNair/KotlinJSSuspendFlowTesting/tree/main/shared/build/generated/ksp/js/jsMain/kotlin)
