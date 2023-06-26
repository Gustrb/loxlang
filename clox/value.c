//
// Created by gustrb on 25/06/23.
//
#include <stdio.h>
#include <string.h>

#include "value.h"
#include "memory.h"
#include "object.h"

void initValueArray(ValueArray *array) {
    array->values = NULL;
    array->capacity = 0;
    array->count = 0;
}

void writeValueArray(ValueArray *array, Value value) {
    if (array->capacity < array->count + 1) {
        int oldCapacity = array->capacity;
        array->capacity = GROW_CAPACITY(oldCapacity);
        array->values = GROW_ARRAY(Value, array->values, oldCapacity, array->capacity);
    }

    array->values[array->count] = value;
    array->count++;
}

void freeValueArray(ValueArray *array) {
    FREE_ARRAY(Value, array->values, array->capacity);
    initValueArray(array);
}

void printValue(Value value) {
    switch (value.type) {
        case VAL_BOOL:   printf(AS_BOOL(value) ? "true" : "false"); break;
        case VAL_NIL:    printf("nil"); break;
        case VAL_NUMBER: printf("%g", AS_NUMBER(value)); break;
        case VAL_OBJ:    printObject(value); break;
    }
}

bool valuesEqual(Value a, Value b) {
    // No weird type coercion :D
    if (a.type != b.type) return false;

    switch (a.type) {
        case VAL_BOOL:   return AS_BOOL(a) == AS_BOOL(b);
        case VAL_NIL:    return true;
        case VAL_NUMBER: return AS_NUMBER(a) == AS_NUMBER(b);
        // Since our strings are interned, we can just compare the pointers, which is much faster than comparing the
        // contents of the strings. And for objects, it is the correct semantics to compare the pointers.
        // since Point(1, 2, 3) should not be equal to Point(1, 2, 3).
        // If both of them were equal to (1, 2, 3), then we would have to compare the contents of the objects.
        // which would be a lot slower.
        case VAL_OBJ:    return AS_OBJ(a) == AS_OBJ(b);
        default:         return false; // Unreachable.
    }
}
