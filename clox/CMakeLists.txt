cmake_minimum_required(VERSION 3.25)
project(clox C)

set(CMAKE_C_STANDARD 17)

add_executable(clox main.c common.h chunk.h chunk.c memory.h memory.c debug.h debug.c value.c value.h vm.c vm.h)
