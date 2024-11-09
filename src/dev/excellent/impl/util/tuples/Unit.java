package dev.excellent.impl.util.tuples;


import dev.excellent.impl.util.tuples.immutable.ImmutableUnit;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Unit<A> implements Serializable {

    public static <A> Unit<A> of(A a) {
        return ImmutableUnit.of(a);
    }

    public abstract A get();

    public abstract <R> R apply(Function<? super A, ? extends R> func);

    public abstract void use(Consumer<? super A> func);

    @Override
    public int hashCode() {
        return Objects.hash(get());
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that instanceof final Unit<?> other) {
            return Objects.equals(get(), other.get());
        }
        return false;
    }

}
