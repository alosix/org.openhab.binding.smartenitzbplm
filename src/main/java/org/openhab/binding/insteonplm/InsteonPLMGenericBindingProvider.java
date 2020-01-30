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
//import java.util.Map;
//
//import org.openhab.binding.smartenitzbplm.internal.device.InsteonAddress;
//import org.openhab.core.items.Item;
//import org.openhab.model.item.binding.AbstractGenericBindingProvider;
//import org.openhab.model.item.binding.BindingConfigParseException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
///**
// * This class is responsible for parsing the binding configuration.
// *
// * @author Bernd Pfrommer
// * @since 1.5.0
// */
//public class InsteonPLMGenericBindingProvider extends AbstractGenericBindingProvider
//        implements InsteonPLMBindingProvider {
//
//    private static final Logger logger = LoggerFactory.getLogger(InsteonPLMGenericBindingProvider.class);
//    private final Map<String, Item> items = new HashMap<String, Item>();
//
//    /**
//     * Inherited from AbstractGenericBindingProvider.
//     * {@inheritDoc}
//     */
//    @Override
//    public String getBindingType() {
//        return "insteonplm";
//    }
//
//    /**
//     * Inherited from AbstractGenericBindingProvider.
//     * {@inheritDoc}
//     */
//    @Override
//    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
//        String[] parts = parseConfigString(bindingConfig);
//        if (parts.length != 3) {
//            throw new BindingConfigParseException("item config must have addr:prodKey#feature format");
//        }
//    }
//
//    /**
//     * Inherited from AbstractGenericBindingProvider.
//     * Processes InsteonPLM binding configuration string.
//     * {@inheritDoc}
//     */
//    @Override
//    public void processBindingConfiguration(String context, Item item, String bindingConfig)
//            throws BindingConfigParseException {
//        super.processBindingConfiguration(context, item, bindingConfig);
//        String[] parts = parseConfigString(bindingConfig);
//        if (parts.length != 3) {
//            throw new BindingConfigParseException("item config must have addr:prodKey#feature format");
//        }
//        InsteonAddress addr = new InsteonAddress(parts[0]);
//        String[] params = parts[2].split(",");
//        String feature = params[0];
//        HashMap<String, String> args = new HashMap<String, String>();
//        for (int i = 1; i < params.length; i++) {
//            String[] kv = params[i].split("=");
//            if (kv.length == 2) {
//                args.put(kv[0], kv[1]);
//            } else {
//                logger.error("parameter {} does not have format a=b", params[i]);
//            }
//        }
//        InsteonPLMBindingConfig config = new InsteonPLMBindingConfig(item.getName(), addr, feature, parts[1], args);
//        addBindingConfig(item, config);
//
//        logger.trace("processing item \"{}\" read from .items file with cfg string {}", item.getName(), bindingConfig);
//        items.put(item.getName(), item);
//    }
//
//    /**
//     * Inherited from InsteonPLMBindingProvider.
//     * {@inheritDoc}
//     */
//    @Override
//    public InsteonPLMBindingConfig getInsteonPLMBindingConfig(String itemName) {
//        return (InsteonPLMBindingConfig) this.bindingConfigs.get(itemName);
//    }
//
//    /**
//     * Parses binding configuration string. The config string has the format:
//     *
//     * xx.xxx.xxx:productKey#feature,param1=yyy,param2=zzz
//     *
//     * @param bindingConfig string with binding parameters
//     * @return String array with split arguments: [address,prodKey,features+params]
//     * @throws BindingConfigParseException if parameters are invalid
//     */
//    private String[] parseConfigString(String bindingConfig) throws BindingConfigParseException {
//        // the config string has the format
//        //
//        // xx.xx.xx:productKey#feature
//        //
//        String shouldBe = "should be address:prodKey#feature, e.g. 28.c3.91:F00.00.01#switch,param=xxx";
//        String[] segments = bindingConfig.split("#");
//        if (segments.length != 2) {
//            throw new BindingConfigParseException("invalid item format: " + bindingConfig + ", " + shouldBe);
//        }
//        String[] dev = segments[0].split(":");
//
//        if (dev.length != 2) {
//            throw new BindingConfigParseException("missing colon in item format: " + bindingConfig + ", " + shouldBe);
//        }
//        String addr = dev[0];
//        String[] retval = { addr, dev[1], segments[1] };
//        if (!InsteonAddress.s_isValid(addr)) {
//            String errstr = "invalid insteon or X10 device address: " + addr
//                    + " in items file. Must have format AB.CD.EF or (for X10): H.U";
//            logger.error(errstr);
//            throw new BindingConfigParseException(errstr);
//        }
//        return retval;
//    }
//}
