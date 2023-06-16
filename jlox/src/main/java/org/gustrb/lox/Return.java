package org.gustrb.lox;

public class Return extends RuntimeException {
    final Object value;

    public Return(final Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
