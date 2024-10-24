/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package jaxrs21.fat.provider;

public class TestEntity { 
    
    private String data1;
    private Integer data2;
    private Boolean data3;

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public Integer getData2() {
        return data2;
    }

    public void setData2(Integer data2) {
        this.data2 = data2;
    }

    public Boolean getData3() {
        return data3;
    }

    public void setData3(Boolean data3) {
        this.data3 = data3;
    }

    public String toString() {
        return "TestEntity data1: " + data1 + " data2: " + data2 + " data3: " + data3;
    }
}
