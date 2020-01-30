/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
//package org.openhab.binding.insteonplm;
//
//import java.util.HashMap;
//import java.util.Map.Entry;
//
//import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
//import org.openhab.core.binding.BindingConfig;
//
///**
// * Holds binding configuration
// *
// * @author Bernd Pfrommer
// * @author Daniel Pfrommer
// * @since 1.5.0
// */
//public class InsteonPLMBindingConfig implements BindingConfig {
//    /**
//     * Constructor
//     * 
//     * @param adr is the Insteon address (format xx.xx.xx) as a string
//     * @param params arguments given in the binding file, as key-value pairs
//     */
//    public InsteonPLMBindingConfig(String name, InsteonAddress adr, String feature, String productKey,
//            HashMap<String, String> params) {
//        this.itemName = name;
//        this.address = adr;
//        this.feature = feature;
//        this.productKey = productKey;
//        this.params = params;
//    }
//
//    private final String itemName;
//    private final InsteonAddress address;
//    private final String feature;
//    private final String productKey;
//    private final HashMap<String, String> params;
//
//    /**
//     * Returns insteon address of device bound to item
//     * 
//     * @return address of device
//     */
//    public InsteonAddress getAddress() {
//        return address;
//    }
//
//    /**
//     * Returns the feature of the device bound to item
//     * 
//     * @return feature of device
//     */
//    public String getFeature() {
//        return feature;
//    }
//
//    /**
//     * Returns the name of the item to which this configuration belongs
//     * 
//     * @return name of item
//     */
//    public String getItemName() {
//        return itemName;
//    }
//
//    /**
//     * Returns the product key of the device. The product key
//     * must be unique for each device type, and is mandatory
//     * to configure the device properly.
//     * 
//     * @return product key
//     */
//    public String getProductKey() {
//        return productKey;
//    }
//
//    /**
//     * Returns the arguments entered in the binding string.
//     * 
//     * @return a map of arguments
//     */
//    public HashMap<String, String> getParameters() {
//        return params;
//    }
//
//    /**
//     * Returns a parameter that starts with key=
//     * 
//     * @param key
//     * @return parameter value or null if not found
//     */
//    public String getParameter(String key) {
//        return (params == null ? null : params.get(key));
//    }
//
//    @Override
//    public String toString() {
//        String s = "addr=" + ((address != null) ? address.toString() : "null_address");
//        s += "|prodKey:" + String.format("%9s", ((productKey != null) ? productKey : "null_pkey"));
//        s += "|feature:" + ((feature != null) ? feature : "null_feature");
//        if (params == null) {
//            s += "|null_params";
//        } else {
//            String sepChar = "|params:";
//            for (Entry<String, String> h : params.entrySet()) {
//                s += sepChar + h.getKey() + "=" + h.getValue();
//                sepChar = ",";
//            }
//        }
//        return (s);
//    }
//}
