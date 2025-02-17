/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2021 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.eolang;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dataized object.
 *
 * @since 0.1
 */
public final class Dataized {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Dataized.class.getName());

    /**
     * The object to datarize.
     */
    private final Phi phi;

    /**
     * Ctor.
     * @param src The object
     */
    public Dataized(final Phi src) {
        this.phi = src;
    }

    /**
     * Take the object, no matter the type.
     * @return The data
     */
    public Object take() {
        Phi src = this.phi;
        try {
            if (!(src instanceof Data)) {
                src = src.attr("Δ").get();
            }
        } catch (final Attr.IllegalAttrException ex) {
            throw new Attr.IllegalAttrException(
                String.format("Attribute failure at:%n%s", this.phi),
                ex
            );
        }
        if (!(src instanceof Data)) {
            throw new Attr.IllegalAttrException(
                String.format(
                    "The attribute Δ has %s instead of %s at:%n%s",
                    src.getClass().getCanonicalName(),
                    Data.class.getCanonicalName(),
                    this.phi
                )
            );
        }
        final Object data = Data.class.cast(src).take();
        Dataized.LOGGER.log(
            Level.FINE,
            String.format(
                "\uD835\uDD3B(%s) ➜ %s",
                this.phi.φTerm(),
                new Data.Value<>(data).φTerm()
            )
        );
        return data;
    }

    /**
     * Take the data with a type.
     * @param type The type
     * @param <T> The type
     * @return The data
     */
    public <T> T take(final Class<T> type) {
        return type.cast(this.take());
    }
}
