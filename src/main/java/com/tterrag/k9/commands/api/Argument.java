package com.tterrag.k9.commands.api;

import com.tterrag.k9.util.Patterns;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public interface Argument<T> {

    String name();
    
    String description();
    
    T parse(String input);
    
    default Pattern pattern() {
        return Patterns.MATCH_ALL;
    }
    
    boolean required(Collection<Flag> flags);

    @Nullable
    List<T> allowedValues();
}
