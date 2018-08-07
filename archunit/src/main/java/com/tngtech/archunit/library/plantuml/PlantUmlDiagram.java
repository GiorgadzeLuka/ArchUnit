/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.plantuml;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import static com.google.common.base.Preconditions.checkNotNull;

class PlantUmlDiagram {
    private final PlantUmlComponents plantUmlComponents;

    private PlantUmlDiagram(PlantUmlDiagram.Builder builder) {
        validateStereotypes(builder.plantUmlComponents);
        this.plantUmlComponents = checkNotNull(builder.plantUmlComponents);
    }

    Set<PlantUmlComponent> getAllComponents() {
        return ImmutableSet.copyOf(plantUmlComponents.getAllComponents());
    }

    Set<PlantUmlComponent> getComponentsWithAlias() {
        return ImmutableSet.copyOf(plantUmlComponents.getComponentsWithAlias());
    }

    private void validateStereotypes(PlantUmlComponents plantUmlComponents) {
        Set<Stereotype> visited = new HashSet<>();
        for (PlantUmlComponent component : plantUmlComponents.getAllComponents()) {
            for (Stereotype stereotype : component.getStereotypes()) {
                if (visited.contains(stereotype)) {
                    throw new IllegalDiagramException(String.format("Stereotype '%s' should be unique", stereotype.getValue()));
                }
                visited.add(stereotype);
            }
        }
    }

    static class Builder {
        private final PlantUmlComponents plantUmlComponents;
        private Multimap<ComponentIdentifier, ParsedDependency> originToParsedDependency;

        Builder(PlantUmlComponents plantUmlComponents) {
            this.plantUmlComponents = plantUmlComponents;
        }

        Builder withDependencies(List<ParsedDependency> dependencies) {
            originToParsedDependency = FluentIterable.from(dependencies).index(ParsedDependency.GET_ORIGIN);
            return this;
        }

        PlantUmlDiagram build() {
            for (PlantUmlComponent component : plantUmlComponents.getAllComponents()) {
                finish(component);
            }
            return new PlantUmlDiagram(this);
        }

        private void finish(PlantUmlComponent component) {
            ImmutableList.Builder<PlantUmlComponentDependency> dependencies = ImmutableList.builder();
            for (ParsedDependency dependencyOriginatingFromComponent : originToParsedDependency.get(component.getIdentifier())) {
                PlantUmlComponent target = plantUmlComponents.findComponentWith(dependencyOriginatingFromComponent.getTarget());
                dependencies.add(new PlantUmlComponentDependency(component, target));
            }
            component.finish(dependencies.build());
        }
    }
}