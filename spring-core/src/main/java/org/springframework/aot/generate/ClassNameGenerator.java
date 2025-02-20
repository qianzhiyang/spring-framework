/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aot.generate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.javapoet.ClassName;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Generate unique class names based on an optional target {@link Class} and
 * a feature name. This class is stateful so the same instance should be used
 * for all name generation. Most commonly the class name generator is obtained
 * via a {@link GenerationContext}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class ClassNameGenerator {

	private static final String SEPARATOR = "__";

	private static final String AOT_PACKAGE = "__.";

	private static final String AOT_FEATURE = "Aot";

	private final Map<String, AtomicInteger> sequenceGenerator = new ConcurrentHashMap<>();


	/**
	 * Generate a unique {@link ClassName} based on the specified {@code target}
	 * class and {@code featureName}. If a {@code target} is specified, the
	 * generated class name is a suffixed version of it.
	 * <p>For instance, a {@code com.example.Demo} target with an
	 * {@code Initializer} feature name leads to a
	 * {@code com.example.Demo__Initializer} generated class name. If such a
	 * feature was already requested for this target, a counter is used to
	 * ensure uniqueness.
	 * <p>If there is no target, the {@code featureName} is used to generate the
	 * class name in the {@value #AOT_PACKAGE} package.
	 * @param target the class the newly generated class relates to, or
	 * {@code null} if there is not target
	 * @param featureName the name of the feature that the generated class
	 * supports
	 * @return a unique generated class name
	 */
	public ClassName generateClassName(@Nullable Class<?> target, String featureName) {
		Assert.hasLength(featureName, "'featureName' must not be empty");
		featureName = clean(featureName);
		if (target != null) {
			return generateSequencedClassName(target.getName().replace("$", "_")
					+ SEPARATOR + StringUtils.capitalize(featureName));
		}
		return generateSequencedClassName(AOT_PACKAGE + featureName);
	}

	private String clean(String name) {
		StringBuilder clean = new StringBuilder();
		boolean lastNotLetter = true;
		for (char ch : name.toCharArray()) {
			if (!Character.isLetter(ch)) {
				lastNotLetter = true;
				continue;
			}
			clean.append(lastNotLetter ? Character.toUpperCase(ch) : ch);
			lastNotLetter = false;
		}
		return (!clean.isEmpty()) ? clean.toString() : AOT_FEATURE;
	}

	private ClassName generateSequencedClassName(String name) {
		name = addSequence(name);
		return ClassName.get(ClassUtils.getPackageName(name),
				ClassUtils.getShortName(name));
	}

	private String addSequence(String name) {
		int sequence = this.sequenceGenerator
				.computeIfAbsent(name, key -> new AtomicInteger()).getAndIncrement();
		return (sequence > 0) ? name + sequence : name;
	}

}
