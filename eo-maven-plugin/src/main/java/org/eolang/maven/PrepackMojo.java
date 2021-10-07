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
package org.eolang.maven;

import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.cactoos.list.Mapped;
import org.cactoos.set.SetOf;

/**
 * Goes through all .class files and deletes those that
 * were created from autogenerated sources; also it deletes
 * binary files, which were previously copied by "resolve" mojo.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@Mojo(
    name = "prepack",
    defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
    threadSafe = true
)
@SuppressWarnings("PMD.ImmutableField")
public final class PrepackMojo extends SafeMojo {

    /**
     * Directory with Java classes.
     * @checkstyle MemberNameCheck (7 lines)
     */
    @Parameter(
        required = true,
        defaultValue = "${project.build.directory}/classes"
    )
    private File classesDir;

    /**
     * Directory with generated sources.
     * @checkstyle MemberNameCheck (7 lines)
     */
    @Parameter(
        required = true,
        defaultValue = "${project.build.directory}/generated-sources"
    )
    private File generatedDir;

    /**
     * The path to a text file where paths of all added
     * .class (and maybe others) files are placed.
     * @checkstyle MemberNameCheck (7 lines)
     * @since 0.11.0
     */
    @Parameter(
        required = true,
        defaultValue = "${project.build.directory}/eo-resolved.csv"
    )
    private File resolvedList;

    /**
     * List of inclusion GLOB filters for finding .class files.
     */
    @Parameter
    private Set<String> includes = new SetOf<>("**/*.class");

    @Override
    public void exec() throws IOException {
        new Walk(this.classesDir.toPath()).stream()
            .filter(
                file -> this.includes.stream().anyMatch(
                    glob -> PrepackMojo.matcher(glob).matches(file)
                )
            )
            .forEach(this::delete);
        this.clean();
    }

    /**
     * Clean resolved files.
     * @throws IOException If fails
     */
    private void clean() throws IOException {
        if (this.resolvedList.exists()) {
            final Collection<Path> files = new Mapped<>(
                t -> Paths.get(t),
                Files.readAllLines(
                    this.resolvedList.toPath(), StandardCharsets.UTF_8
                )
            );
            for (final Path file : files) {
                Files.delete(file);
            }
            Logger.info(
                this, "All %d binari(es) deleted, which were found in %s",
                files.size(), this.resolvedList
            );
        } else {
            Logger.info(this, "The list of resolved binaries is absent: %s", this.resolvedList);
        }
    }

    /**
     * Create glob matcher from text.
     * @param text The pattern
     * @return Matcher
     */
    private static PathMatcher matcher(final String text) {
        return FileSystems.getDefault()
            .getPathMatcher(String.format("glob:%s", text));
    }

    /**
     * Delete .class file if .java file is present.
     * @param file EO file
     */
    private void delete(final Path file) {
        final String name = file.toString().substring(
            this.classesDir.toString().length() + 1
        );
        final Path java = this.generatedDir.toPath().resolve(
            name.replaceAll("\\.class$", ".java")
        );
        if (Files.exists(java)) {
            try {
                Files.delete(file);
            } catch (final IOException ex) {
                throw new IllegalStateException(ex);
            }
            Logger.info(this, "%s deleted since %s is present", file, java);
        }
    }

}