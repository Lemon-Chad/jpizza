package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.types.GenericType;
import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;
import lemon.jpizza.compiler.types.objects.FuncType;
import lemon.jpizza.compiler.types.objects.MapType;
import lemon.jpizza.compiler.types.objects.MaybeType;
import lemon.jpizza.compiler.types.objects.VecType;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.JClosure;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JPCollections extends JPExtension {
    public JPCollections(VM vm) {
        super(vm);
    }

    @Override
    public String name() {
        return "collections";
    }

    @Override
    public void setup() {
        vector();
        map();

        // Optionals
        define("maybe", (args) -> args.length == 1 ? NativeResult.Ok(args[0]) : NativeResult.Ok(), new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
            @Override
            public Type call(Type[] arguments, Type[] generics) {
                // maybe<T>()
                // maybe(T)
                if (arguments.length == 0 && generics.length == 1) {
                    return new MaybeType(generics[0]);
                }
                else if (arguments.length == 1 && generics.length == 0) {
                    if (arguments[0] == Types.VOID)
                        return null;
                    return new MaybeType(arguments[0]);
                }
                else if (arguments.length == 1 && generics.length == 1) {
                    if (arguments[0] != Types.VOID && arguments[0] != generics[0]) {
                        return null;
                    }
                    return new MaybeType(arguments[0]);
                }
                return null;
            }
        });

        // is(maybe<T>, callback(T)) -> maybe<T>
        define("is", (args) -> {
            if (args[0].isNull)
                return NativeResult.Ok(args[0]);
            JClosure closure = args[1].asClosure();
            VM.Run(closure, new Value[]{ args[0] });
            return NativeResult.Ok(args[0]);
        }, new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
            @Override
            public Type call(Type[] arguments, Type[] generics) {
                if (arguments.length != 2) return null;
                if (!(arguments[0] instanceof MaybeType)) return null;

                MaybeType maybeType = (MaybeType) arguments[0];
                Type optionalType = maybeType.getOptionalType();
                if (!arguments[1].equals(new FuncType(Types.VOID, new Type[]{ optionalType }, new GenericType[0], false))) {
                    return null;
                }
                return maybeType;
            }
        });
        // isnt(maybe<T>, callback()) -> maybe<T>
        define("isnt", (args) -> {
            if (!args[0].isNull)
                return NativeResult.Ok(args[0]);
            JClosure closure = args[1].asClosure();
            VM.Run(closure, new Value[0]);
            return NativeResult.Ok(args[0]);
        }, new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
            @Override
            public Type call(Type[] arguments, Type[] generics) {
                if (arguments.length != 2) return null;
                if (!(arguments[0] instanceof MaybeType)) return null;

                if (!arguments[1].equals(new FuncType(Types.VOID, new Type[0], new GenericType[0], false))) {
                    return null;
                }
                return arguments[0];
            }
        });
    }

    private void vector() {
        GenericType genericItem = new GenericType("(T)");

        define("vec", (args) -> NativeResult.Ok(new Value(new ArrayList<>(Arrays.asList(args)))),
                new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
                    @Override
                    public Type call(Type[] arguments, Type[] generics) {
                        // vec<T>()
                        // vec(T)
                        // vec(T, T, ...)
                        Type itemType;
                        if (generics.length == 0 && arguments.length > 0) {
                            itemType = arguments[0];
                        } else if (generics.length == 1) {
                            itemType = generics[0];
                        } else {
                            return null;
                        }

                        for (int i = 0; i < arguments.length; i++) {
                            if (!arguments[i].equals(itemType)) {
                                return null;
                            }
                        }

                        return new VecType(itemType);
                    }
                });

        define("push", (args) -> {
            Value vec = args[0];
            Value item = args[1];
            vec.append(item);
            return NativeResult.Ok();
        }, vecFunction(Types.VOID, genericItem));

        define("removeItem", (args) -> {
            Value vec = args[0];
            Value item = args[1];
            vec.remove(item);
            return NativeResult.Ok();
        }, vecFunction(Types.VOID, genericItem));

        define("merge", (args) -> {
            Value vec = args[0];
            Value other = args[1];
            vec.add(other);
            return NativeResult.Ok();
        }, vecFunction(Types.VOID, new VecType(genericItem)));

        define("add", (args) -> {
            Value vec = args[0];
            Value item = args[1];
            Value index = args[2];

            if (vec.asList().size() < index.asNumber() || index.asNumber() < 0) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            vec.insert(index.asNumber(), item);
            return NativeResult.Ok();
        }, vecFunction(Types.VOID, genericItem, Types.INT));

        define("setAt", (args) -> {
            Value vec = args[0];
            Value item = args[1];
            Value index = args[2];

            if (vec.asList().size() < index.asNumber() || index.asNumber() < 0) {
                return NativeResult.Err("Scope", "Index out of bounds");
            }

            vec.set(index.asNumber(), item);
            return NativeResult.Ok();
        }, vecFunction(Types.VOID, genericItem, Types.INT));
    }

    private FuncType vecFunction(Type functionReturn, Type... argumentTypes) {
        GenericType genericItem = new GenericType("(T)");
        return new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
            @Override
            public Type call(Type[] arguments, Type[] generics) {
                if (arguments.length != argumentTypes.length + 1) return null;
                if (!(arguments[0] instanceof VecType)) return null;

                VecType vecType = (VecType) arguments[0];
                Type itemType = vecType.getItemType();

                Map<Type, Type> genericsMap = new HashMap<>();
                genericsMap.put(genericItem, itemType);

                return forwardGenerics(functionReturn, genericsMap, arguments, argumentTypes);
            }
        };
    }

    private void map() {
        GenericType genericKey = new GenericType("(K)");
        GenericType genericValue = new GenericType("(V)");

        define("map", (args) -> {
            Map<Value, Value> map = new HashMap<>();
            for (int i = 0; i < args.length; i += 2) {
                map.put(args[i], args[i + 1]);
            }
            return NativeResult.Ok(new Value(map));
        }, new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
            @Override
            public Type call(Type[] arguments, Type[] generics) {
                // map<K, V>()
                // map(K, V)
                // map(K, V, K, V, ...)
                Type keyType;
                Type valueType;
                if (generics.length == 0 && arguments.length > 1) {
                    keyType = arguments[0];
                    valueType = arguments[1];
                } else if (generics.length == 2) {
                    keyType = generics[0];
                    valueType = generics[1];
                } else {
                    return null;
                }

                // Make sure the # of arguments is even
                if (arguments.length % 2 != 0) {
                    return null;
                }

                for (int i = 0; i < arguments.length; i += 2) {
                    if (!arguments[i].equals(keyType) || !arguments[i + 1].equals(valueType)) {
                        return null;
                    }
                }

                return new MapType(keyType, valueType);
            }
        });

        define("put", (args) -> {
            args[0].asMap().put(args[1], args[2]);
            return NativeResult.Ok();
        }, mapFunction(Types.VOID, genericKey, genericValue));

        define("putReplacing", (args) -> {
            args[0].asMap().replace(args[1], args[2]);
            return NativeResult.Ok();
        }, mapFunction(Types.VOID, genericKey, genericValue));

        define("getKey", (args) -> {
            if (args[0].asMap().containsKey(args[1])) {
                return NativeResult.Ok(args[0].asMap().get(args[1]));
            }
            return NativeResult.Err("Key", "Key not found");
        }, mapFunction(genericValue, genericKey));

        define("removeKey", (args) -> {
            args[0].delete(args[1]);
            return NativeResult.Ok();
        }, mapFunction(Types.VOID, genericKey));
    }

    private FuncType mapFunction(Type functionReturn, Type... argumentTypes) {
        GenericType genericKey = new GenericType("(K)");
        GenericType genericValue = new GenericType("(V)");
        return new FuncType(Types.ANY, new Type[0], new GenericType[0], true) {
            @Override
            public Type call(Type[] arguments, Type[] generics) {
                if (arguments.length != argumentTypes.length + 1) return null;
                if (!(arguments[0] instanceof MapType)) return null;

                MapType vecType = (MapType) arguments[0];
                Type keyType = vecType.getKeyType();
                Type valueType = vecType.getValueType();

                Map<Type, Type> genericsMap = new HashMap<>();
                genericsMap.put(genericKey, keyType);
                genericsMap.put(genericValue, valueType);

                return forwardGenerics(functionReturn, genericsMap, arguments, argumentTypes);
            }
        };
    }

    private Type forwardGenerics(Type returnType, Map<Type, Type> genericsMap, Type[] arguments, Type[] argumentTypes) {
        for (int i = 1; i < arguments.length; i++) {
            Type expected = argumentTypes[i - 1].applyGenerics(genericsMap);
            if (!arguments[i].equals(expected)) {
                return null;
            }
        }
        return returnType.applyGenerics(genericsMap);
    }
}
