cmake_minimum_required(VERSION 3.20)
project(eve LANGUAGES CXX VERSION 5.4.0)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Release)
endif()

set(EVE_CORE_SOURCES
    src/eve.cpp
    src/eve_compile.cpp
    src/eve_lang.cpp
    src/eve_strict.cpp
    src/eve_expanda.cpp
)

add_library(eve_core STATIC ${EVE_CORE_SOURCES})
target_include_directories(eve_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/src)
set_target_properties(eve_core PROPERTIES POSITION_INDEPENDENT_CODE ON)

if(MSVC)
    target_compile_options(eve_core PRIVATE /W4 /O2)
else()
    target_compile_options(eve_core PRIVATE -Wall -Wextra -O2)
endif()

option(EVE_BUILD_TESTS "Build the conformance + unit tests" ON)
if(EVE_BUILD_TESTS)
    add_executable(conformance test/conformance.cpp)
    target_link_libraries(conformance PRIVATE eve_core)

    add_executable(conformance_eve test/conformance_eve.cpp)
    target_link_libraries(conformance_eve PRIVATE eve_core)

    add_executable(langtest test/langtest.cpp)
    target_link_libraries(langtest PRIVATE eve_core)

    enable_testing()
    add_test(NAME langtest COMMAND langtest)
endif()

option(EVE_BUILD_JNI "Build the JNI shared library (requires JNI headers)" OFF)
if(EVE_BUILD_JNI)

    set(EVE_JNI_INCLUDES "")
    if(DEFINED JAVA_HOME AND EXISTS "${JAVA_HOME}/include/jni.h")
        set(EVE_JNI_INCLUDES "${JAVA_HOME}/include")
        foreach(sub linux win32 darwin)
            if(EXISTS "${JAVA_HOME}/include/${sub}/jni_md.h")
                list(APPEND EVE_JNI_INCLUDES "${JAVA_HOME}/include/${sub}")
            endif()
        endforeach()
    elseif(DEFINED ENV{JAVA_HOME} AND EXISTS "$ENV{JAVA_HOME}/include/jni.h")
        set(EVE_JNI_INCLUDES "$ENV{JAVA_HOME}/include")
        foreach(sub linux win32 darwin)
            if(EXISTS "$ENV{JAVA_HOME}/include/${sub}/jni_md.h")
                list(APPEND EVE_JNI_INCLUDES "$ENV{JAVA_HOME}/include/${sub}")
            endif()
        endforeach()
    else()
        find_package(JNI REQUIRED)
        set(EVE_JNI_INCLUDES ${JNI_INCLUDE_DIRS})
    endif()

    add_library(eve SHARED jni/eve_jni.cpp)
    target_link_libraries(eve PRIVATE eve_core)
    target_include_directories(eve PRIVATE ${EVE_JNI_INCLUDES})
    set_target_properties(eve PROPERTIES OUTPUT_NAME eve PREFIX "lib")
    if(WIN32)
        set_target_properties(eve PROPERTIES PREFIX "")
    endif()
endif()
