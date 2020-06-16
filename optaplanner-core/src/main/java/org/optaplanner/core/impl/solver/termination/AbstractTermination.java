/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

package org.optaplanner.core.impl.solver.termination;

import org.optaplanner.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import org.optaplanner.core.impl.solver.scope.SolverScope;
import org.optaplanner.core.impl.solver.thread.ChildThreadType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract superclass for {@link Termination}.
 */
public abstract class AbstractTermination extends PhaseLifecycleListenerAdapter implements Termination {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    // ************************************************************************
    // Other methods
    // ************************************************************************

    @Override
    public Termination createChildThreadTermination(SolverScope solverScope, ChildThreadType childThreadType) {
        throw new UnsupportedOperationException("This terminationClass (" + getClass()
                + ") does not yet support being used in child threads of type (" + childThreadType + ").");
    }

}
