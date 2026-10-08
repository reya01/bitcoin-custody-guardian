cmake_minimum_required(VERSION 3.22)
project(llama_btcg)

set(CMAKE_CXX_STANDARD 17)

# llama.cpp source: point LLAMA_DIR at the pinned checkout, or drop a copy in
# app/src/main/cpp/llama.cpp (submodule-style) - whichever the build host has.
if(NOT DEFINED LLAMA_DIR)
    set(LLAMA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")
endif()

add_library(llama STATIC IMPORTED)
set_target_properties(llama PROPERTIES IMPORTED_LOCATION ${LLAMA_DIR}/build-android-arm64/src/libllama.a)
if(NOT EXISTS ${LLAMA_DIR}/build-android-arm64/src/libllama.a)
    message(FATAL_ERROR "libllama.a missing: build llama.cpp for android arm64 first (see docs/BUILD.md)")
endif()

add_library(ggml STATIC IMPORTED)
set_target_properties(ggml PROPERTIES IMPORTED_LOCATION ${LLAMA_DIR}/build-android-arm64/ggml/src/libggml.a)
add_library(ggml-base STATIC IMPORTED)
set_target_properties(ggml-base PROPERTIES IMPORTED_LOCATION ${LLAMA_DIR}/build-android-arm64/ggml/src/libggml-base.a)
add_library(ggml-cpu STATIC IMPORTED)
set_target_properties(ggml-cpu PROPERTIES IMPORTED_LOCATION ${LLAMA_DIR}/build-android-arm64/ggml/src/libggml-cpu.a)

target_include_directories(llama INTERFACE ${LLAMA_DIR}/include ${LLAMA_DIR}/ggml/include)

add_library(llama_btcg SHARED llama_jni.cpp)
target_link_libraries(llama_btcg llama ggml ggml-base ggml-cpu log)
