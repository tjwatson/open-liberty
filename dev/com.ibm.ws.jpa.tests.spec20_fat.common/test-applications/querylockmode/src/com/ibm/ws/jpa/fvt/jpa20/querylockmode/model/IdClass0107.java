/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.model;

import java.io.Serializable;

/**
 * <p>Id class of the Common Datamodel (which uses all the possible JPA 2.0 Annotations as described in the
 * <a href="http://www.j2ee.me/javaee/6/docs/api/javax/persistence/package-summary.html">javax.persistence documentation</a>)
 *
 *
 * <p><b>Notes:</b>
 * <ol>
 * <li>Per the JSR-317 spec (page 28), the primary key class:
 * <ul>
 * <li>Must be serializable
 * <li>Must define equals and hashCode methods
 * </ul>
 * </ol>
 */
public class IdClass0107 implements Serializable {

    private Double entity0107_id1;

    private Double entity0107_id2;

    public IdClass0107() {}

    public IdClass0107(Double id1,
                       Double id2) {
        this.entity0107_id1 = id1;
        this.entity0107_id2 = id2;
    }

    @Override
    public String toString() {
        return (" IdClass0107: " +
                " entity0107_id1: " + getEntity0107_id1() +
                " entity0107_id2: " + getEntity0107_id2());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0107))
            return false;
        if (o == this)
            return true;
        IdClass0107 idClass = (IdClass0107) o;
        return (idClass.entity0107_id1 == entity0107_id1 &&
                idClass.entity0107_id2 == entity0107_id2);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0107_id1.hashCode();
        result = result + entity0107_id2.hashCode();
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public Double getEntity0107_id1() {
        return entity0107_id1;
    }

    public Double getEntity0107_id2() {
        return entity0107_id2;
    }
}
