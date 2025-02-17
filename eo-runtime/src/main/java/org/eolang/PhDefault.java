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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A simple object.
 *
 * @since 0.1
 */
public abstract class PhDefault implements Phi, Cloneable {

    /**
     * Named attr format.
     */
    private static final String FORMAT = "%s#%s";

    /**
     * Line bread and tab.
     */
    private static final String REPLACEMENT = "\n  ";

    /**
     * Attribute name matcher.
     */
    private static final Pattern SORTABLE = Pattern.compile("^[a-z].*$");

    /**
     * Attributes.
     */
    private Map<String, Attr> attrs;

    /**
     * Order of their names.
     */
    private final List<String> order;

    /**
     * Ctor.
     */
    public PhDefault() {
        this(Phi.Φ);
    }

    /**
     * Ctor.
     *
     * @param sigma Sigma
     */
    public PhDefault(final Phi sigma) {
        this.attrs = new HashMap<>(0);
        this.order = new ArrayList<>(0);
        this.add("ρ", new AtSimple(sigma));
        this.add("σ", new AtSimple(sigma));
    }

    @Override
    public String φTerm() {
        final Collection<String> list = new ArrayList<>(this.attrs.size());
        for (final Map.Entry<String, Attr> ent : this.attrs.entrySet()) {
            if (!PhDefault.SORTABLE.matcher(ent.getKey()).matches()) {
                continue;
            }
            final String attr = String.format(
                "%s↦%s",
                ent.getKey(),
                ent.getValue().φTerm()
            );
            if (attr.endsWith("λ")) {
                continue;
            }
            list.add(attr);
        }
        String txt = this.getClass().getSimpleName();
        final XmirObject xmir = this.getClass().getAnnotation(XmirObject.class);
        if (xmir != null) {
            txt = xmir.oname();
        }
        if (!list.isEmpty()) {
            txt = String.format(
                "%s⟦%s⟧", txt,
                String.join(", ", list)
            );
        }
        return txt;
    }

    @Override
    public String toString() {
        final Collection<String> list = new ArrayList<>(this.attrs.size());
        list.add(
            String.format(
                "_order=%s",
                this.order
            )
        );
        for (final Map.Entry<String, Attr> ent : this.attrs.entrySet()) {
            final int idx = this.order.indexOf(ent.getKey());
            list.add(
                String.format(
                    "%s%s=%s",
                    ent.getKey(),
                    idx >= 0 ? String.format("(%d)", idx) : "",
                    ent.getValue().toString().replace("\n", PhDefault.REPLACEMENT)
                )
            );
        }
        return String.format(
            "%s#%d:{\n  %s\n}",
            this.getClass().getCanonicalName(),
            this.hashCode(),
            String.join(PhDefault.REPLACEMENT, list)
        );
    }

    @Override
    public final Phi copy(final Phi rho) {
        try {
            final PhDefault copy = PhDefault.class.cast(this.clone());
            final Map<String, Attr> map = new HashMap<>(this.attrs.size());
            for (final Map.Entry<String, Attr> ent : this.attrs.entrySet()) {
                if ("ρ".equals(ent.getKey())) {
                    continue;
                }
                map.put(ent.getKey(), ent.getValue().copy(copy));
            }
            map.put("ρ", new AtSimple(rho));
            copy.attrs = map;
            return copy;
        } catch (final CloneNotSupportedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public final Attr attr(final int pos) {
        if (this.order.isEmpty()) {
            throw new Attr.IllegalAttrException(
                String.format(
                    "There are no attributes here, can't get the %d-th one",
                    pos
                )
            );
        }
        final int idx;
        if (pos >= this.order.size()) {
            idx = this.order.size() - 1;
        } else {
            idx = pos;
        }
        return this.attr(this.order.get(idx));
    }

    @Override
    public final Attr attr(final String name) {
        Attr attr = this.attrs.get(name);
        if (attr == null) {
            final Attr phi = this.attrs.get("φ");
            if (phi == null) {
                attr = new AtAbsent(
                    name,
                    String.format(
                        " among other %d attrs (%s)",
                        this.attrs.size(),
                        String.join(", ", this.attrs.keySet())
                    )
                );
            } else {
                attr = new AtDecorated(phi, name, this);
            }
        }
        return new AtNamed(
            String.format(
                PhDefault.FORMAT,
                this.getClass().getCanonicalName(),
                name
            ),
            this,
            attr
        );
    }

    /**
     * Add new attribute.
     *
     * This method can only be called from child classes, in their
     * constructors, when the declare their attributes. This is why it's
     * protected. Not the brightest design, I admit.
     *
     * @param name The name
     * @param attr The attr
     */
    protected final void add(final String name, final Attr attr) {
        if (PhDefault.SORTABLE.matcher(name).matches()) {
            this.order.add(name);
        }
        this.attrs.put(
            name,
            new AtNamed(
                String.format(
                    PhDefault.FORMAT,
                    this.getClass().getCanonicalName(),
                    name
                ),
                this,
                attr
            )
        );
    }

}
