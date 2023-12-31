//
// Created by gustrb on 25/06/23.
//

#ifndef CLOX_MEMORY_H
#define CLOX_MEMORY_H

#include "common.h"
#include "value.h"
#include "compiler.h"

#define FREE(type, pointer) \
    reallocate(pointer, sizeof(type), 0)

#define GROW_CAPACITY(capacity) \
    ((capacity) < 8 ? 8 : (capacity) * 2)

#define GROW_ARRAY(type, pointer, oldCount, newCount) \
    (type *)reallocate(pointer, sizeof(type) * (oldCount), sizeof(type) * (newCount))

#define FREE_ARRAY(type, pointer, oldCount) \
    reallocate(pointer, sizeof(type) * (oldCount), 0)

#define ALLOCATE(type, count) \
    (type *)reallocate(NULL, 0, sizeof(type) * (count))

void *reallocate(void *pointer, size_t oldSize, size_t newSize);
void markValue(Value value);
void markObject(Obj *object);
void collectGarbage();
void freeObjects();

#endif //CLOX_MEMORY_H
