/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.gui.components.formatters;

import com.haulmont.cuba.core.global.MetadataTools;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionFormatter implements Function<Collection, String> {

    protected MetadataTools metadataTools;

    public CollectionFormatter(MetadataTools metadataTools) {
        this.metadataTools = metadataTools;
    }

    @Override
    public String apply(Collection value) {
        if (value == null) {
            return StringUtils.EMPTY;
        }

        //noinspection unchecked
        return ((Collection<Object>) value).stream()
                .map(metadataTools::format)
                .collect(Collectors.joining(", "));
    }
}