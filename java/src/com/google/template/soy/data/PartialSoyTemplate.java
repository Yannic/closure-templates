/*
 * Copyright 2023 Google Inc.
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

package com.google.template.soy.data;


/**
 * An invocation of a Soy template, encapsulating both the template name and some the data
 * parameters passed to the template. Note that this is allowed to have some of its parameters not
 * fully added, but it cannot be used for rendering.
 */
public abstract class PartialSoyTemplate extends TemplateInterface {}
