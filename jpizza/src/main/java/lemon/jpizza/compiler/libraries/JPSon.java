package lemon.jpizza.compiler.libraries;

import com.fasterxml.jackson.core.type.TypeReference;
import lemon.jpizza.Pair;
import lemon.jpizza.Token;
import lemon.jpizza.TokenType;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;
import lemon.jpizza.errors.Error;
import lemon.jpizza.generators.Lexer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

public class JPSon extends JPExtension {
    static final List<TokenType> Valid = Arrays.asList(
            TokenType.String,
            TokenType.Float,
            TokenType.Int,
            TokenType.LeftBracket,
            TokenType.RightBracket,
            TokenType.Comma,
            TokenType.Colon,
            TokenType.LeftBrace,
            TokenType.RightBrace,
            TokenType.EndOfFile,
            TokenType.Boolean
    );

    @Override
    public String name() { return "json"; }

    public JPSon(VM vm) {
        super(vm);
    }

    private String dump(Value value) {
        if (value.isString) {
            return "\"" + value.asString() + "\"";
        }
        else if (value.isNumber) {
            return value.asNumber().toString();
        }
        else if (value.isBool) {
            return value.asBool() ? "true" : "false";
        }
        else if (value.isList) {
            return dumpList(value.asList());
        }
        else if (value.isMap) {
            return dumpMap(value.asMap());
        }
        else {
            return "";
        }
    }

    private String dumpList(List<Value> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < list.size(); i++) {
            Value v = list.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append(dump(v));
        }
        sb.append("]");
        return sb.toString();
    }

    private String dumpMap(Map<Value, Value> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<Value, Value> entry : map.entrySet()) {
            Value k = entry.getKey();
            Value v = entry.getValue();
            sb.append(dump(k));
            sb.append(":");
            sb.append(dump(v));
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public void setup() {
        func("loads", args -> {
            String value = args[0].toString();
            if ((!value.startsWith("{") || !value.endsWith("}")) &&
                    (!value.startsWith("[") || !value.endsWith("]"))) {
                return Err("Malformed JSON", "JSON must be enclosed in brackets");
            }
            Pair<List<Token>, Error> tokens = new Lexer("json-loads", value).make_tokens();
            if (tokens.b != null) {
                return Err("Malformed JSON", tokens.b.details);
            }

            for (Token t : tokens.a) {
                if (!Valid.contains(t.type)) {
                    return Err("Malformed JSON", "Invalid token type: " + t.type);
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            try {
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
                Map<String, Object> map = mapper.readValue(value, typeRef);
                Map<Value, Value> values = new HashMap<>();
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    values.put(new Value(entry.getKey()), Value.fromObject(entry.getValue()));
                }
                return Ok(values);
            } catch (IOException e) {
                return Err("Malformed JSON", e.getMessage());
            }
        }, Collections.singletonList("String"));
        func("dumps", args -> Ok(dump(args[0])), 1);
    }

}
