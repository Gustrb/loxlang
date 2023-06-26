//
// Created by gustrb on 25/06/23.
//

#ifndef CLOX_COMPILER_H
#define CLOX_COMPILER_H

#include "common.h"
#include "chunk.h"
#include "vm.h"

bool compile(const char *source, Chunk *chunk);

#endif //CLOX_COMPILER_H
