//
// Created by gustrb on 26/06/23.
//

#ifndef CLOX_OBJECT_H
#define CLOX_OBJECT_H

#include "common.h"
#include "value.h"
#include "chunk.h"
#include "table.h"

#define OBJ_TYPE(value) (AS_OBJ(value)->type)

#define IS_STRING(value) isObjType(value, OBJ_STRING)
#define IS_FUNCTION(value) isObjType(value, OBJ_FUNCTION)
#define IS_NATIVE(value) isObjType(value, OBJ_NATIVE)
#define IS_CLOSURE(value) isObjType(value, OBJ_CLOSURE)
#define IS_CLASS(value) isObjType(value, OBJ_CLASS)
#define IS_INSTANCE(value) isObjType(value, OBJ_INSTANCE)
#define IS_BOUND_METHOD(value) isObjType(value, OBJ_BOUND_METHOD)

#define AS_STRING(value) ((ObjString*)AS_OBJ(value))
#define AS_FUNCTION(value) ((ObjFunction*)AS_OBJ(value))
#define AS_CSTRING(value) (((ObjString*)AS_OBJ(value))->chars)
#define AS_NATIVE(value) (((ObjNative*)AS_OBJ(value))->function)
#define AS_CLOSURE(value) ((ObjClosure*)AS_OBJ(value))
#define AS_CLASS(value) ((ObjClass*)AS_OBJ(value))
#define AS_INSTANCE(value) ((ObjInstance*)AS_OBJ(value))
#define AS_BOUND_METHOD(value) ((ObjBoundMethod*)AS_OBJ(value))

typedef enum {
    OBJ_STRING,
    OBJ_FUNCTION,
    OBJ_NATIVE,
    OBJ_CLOSURE,
    OBJ_UPVALUE,
    OBJ_CLASS,
    OBJ_INSTANCE,
    OBJ_BOUND_METHOD,
} ObjType;

struct Obj {
    ObjType type;
    bool isMarked;
    struct Obj *next;
};

typedef struct {
    Obj obj;
    int arity;
    int upvalueCount;
    Chunk chunk;
    ObjString *name;
} ObjFunction;

typedef struct ObjUpvalue {
    Obj obj;
    Value *location;
    Value closed;
    struct ObjUpvalue *next;
} ObjUpvalue;

typedef struct {
    Obj obj;
    ObjFunction *function;

    ObjUpvalue **upvalues;
    int upvalueCount;
} ObjClosure;

struct ObjString {
    Obj obj;
    int length;
    char *chars;
    uint32_t hash;
};

typedef Value (*NativeFn)(int argCount, Value *args);

typedef struct {
    Obj obj;
    NativeFn function;
} ObjNative;

typedef struct {
    Obj obj;
    ObjString *name;
    Table methods;
} ObjClass;

typedef struct {
    Obj obj;
    ObjClass *klass;
    Table fields;
} ObjInstance;

typedef struct {
    Obj obj;
    Value receiver;
    ObjClosure *method;
} ObjBoundMethod;

static inline bool isObjType(Value value, ObjType type) {
    return IS_OBJ(value) && AS_OBJ(value)->type == type;
}

ObjBoundMethod *newBoundMethod(Value receiver, ObjClosure *method);
ObjInstance *newInstance(ObjClass *klass);
ObjClass *newClass(ObjString *name);
ObjUpvalue *newUpvalue(Value *slot);
ObjClosure *newClosure(ObjFunction *function);
ObjFunction *newFunction();
ObjNative *newNative(NativeFn function);
ObjString *copyString(const char *chars, int length);
ObjString *takeString(char *chars, int length);
void printObject(Value value);


#endif //CLOX_OBJECT_H
