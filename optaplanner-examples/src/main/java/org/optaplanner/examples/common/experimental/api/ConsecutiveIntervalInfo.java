/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.common.experimental.api;

public interface ConsecutiveIntervalInfo<IntervalType_, PointType_ extends Comparable<PointType_>, DifferenceType_ extends Comparable<DifferenceType_>> {

    /**
     * @return never null, an iterable that iterates through the interval clusters
     *         contained in the collection in ascending order
     */
    Iterable<IntervalCluster<IntervalType_, PointType_, DifferenceType_>> getIntervalClusters();

    /**
     * @return never null, an iterable that iterates through the breaks contained in
     *         the collection in ascending order
     */
    Iterable<IntervalBreak<IntervalType_, PointType_, DifferenceType_>> getBreaks();
}
